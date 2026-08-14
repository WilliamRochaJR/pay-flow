import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

const accounts = [
  { id: 'account-1', holderName: 'Ana Lima', balance: 2500, currency: 'BRL' },
  { id: 'account-2', holderName: 'Bruno Costa', balance: 1800, currency: 'BRL' },
]

async function login() {
  await userEvent.type(screen.getByLabelText('E-mail'), 'user@example.com')
  await userEvent.type(screen.getByLabelText('Senha'), 'safe-password')
  await userEvent.click(screen.getByRole('button', { name: /^entrar$/i }))
}

describe('PayFlow dashboard', () => {
  beforeEach(() => window.history.replaceState({}, '', '/'))

  afterEach(() => {
    cleanup()
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  it('restores the authenticated session after a page reload', async () => {
    sessionStorage.setItem('payflow.access-token', 'persisted-token')
    const fetchMock = vi.fn(async (input: RequestInfo | URL, options?: RequestInit) => {
      const url = String(input)
      expect(options?.headers).toMatchObject({ Authorization: 'Bearer persisted-token' })
      if (url.endsWith('/accounts')) return Response.json(accounts)
      if (url.endsWith('/transfers')) return Response.json([])
      return Response.json({}, { status: 404 })
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)

    expect(
      await screen.findByRole('heading', { name: 'Seu dinheiro, em movimento.' }),
    ).toBeVisible()
    expect(window.location.pathname).toBe('/dashboard')
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('clears an expired session when the API returns unauthorized', async () => {
    sessionStorage.setItem('payflow.access-token', 'expired-token')
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => Response.json({ detail: 'Token expirado.' }, { status: 401 })),
    )

    render(<App />)

    expect(await screen.findByText('Sua sessão expirou. Entre novamente.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Acesse sua conta' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/login')
    expect(sessionStorage.getItem('payflow.access-token')).toBeNull()
  })

  it('returns to login without authenticating after registration', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      if (url.endsWith('/auth/register')) {
        return Response.json(
          { id: 'user-1', name: 'New User', email: 'new@example.com' },
          { status: 201 },
        )
      }
      return Response.json({}, { status: 404 })
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)
    await user.click(screen.getByRole('link', { name: /criar conta/i }))
    await user.type(screen.getByLabelText('Nome'), 'New User')
    await user.type(screen.getByLabelText('E-mail'), 'new@example.com')
    await user.type(screen.getByLabelText('Senha'), 'safe-password')
    await user.click(screen.getByRole('button', { name: /^criar conta$/i }))

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    expect(await screen.findByRole('heading', { name: 'Acesse sua conta' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(
      'Conta criada com sucesso. Entre com seu e-mail e senha.',
    )
    expect(window.location.pathname).toBe('/login')
    expect(screen.getByLabelText('E-mail')).toHaveValue('new@example.com')
    expect(screen.getByLabelText('Senha')).toHaveValue('')
  })

  it('creates a transfer and refreshes the history', async () => {
    let transfers: object[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, options?: RequestInit) => {
        const url = String(input)
        if (url.endsWith('/auth/login')) {
          return Response.json({ accessToken: 'test-token', tokenType: 'Bearer', expiresIn: 900 })
        }
        if (url.endsWith('/accounts')) return Response.json(accounts)
        if (url.endsWith('/transfers') && options?.method === 'POST') {
          transfers = [
            {
              id: 'transfer-1',
              sourceAccountId: 'account-1',
              destinationAccountId: 'account-2',
              amount: 50,
              currency: 'BRL',
              status: 'COMPLETED',
              type: 'INTERNAL_TRANSFER',
              createdAt: '2026-08-10T22:00:00Z',
            },
          ]
          return Response.json(transfers[0], { status: 201 })
        }
        if (url.endsWith('/transfers')) return Response.json(transfers)
        return Response.json({}, { status: 404 })
      }),
    )

    render(<App />)
    await login()
    expect((await screen.findAllByText('Ana Lima')).length).toBeGreaterThan(0)
    await userEvent.type(screen.getByLabelText('Valor'), '50')
    await userEvent.click(screen.getByRole('button', { name: /transferir agora/i }))

    expect(await screen.findByText('Transferência concluída com sucesso.')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('Ana Lima → Bruno Costa')).toBeInTheDocument())
  })

  it('selects another destination when the source changes to the current destination', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input)
        if (url.endsWith('/auth/login')) {
          return Response.json({ accessToken: 'test-token', tokenType: 'Bearer', expiresIn: 900 })
        }
        if (url.endsWith('/accounts')) return Response.json(accounts)
        if (url.endsWith('/transfers')) return Response.json([])
        return Response.json({}, { status: 404 })
      }),
    )

    render(<App />)
    await login()
    const source = await screen.findByLabelText('Conta de origem')
    const destination = screen.getByLabelText('Conta de destino')

    expect(source).toHaveValue('account-1')
    expect(destination).toHaveValue('account-2')

    await userEvent.selectOptions(source, 'account-2')

    expect(source).toHaveValue('account-2')
    expect(destination).toHaveValue('account-1')
    expect(screen.getByRole('button', { name: /transferir agora/i })).toBeEnabled()
  })
})
