import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { Transfer } from '../features/transfers/list-transfers/listTransfers.model'
import App from './App'

const accounts = [
  { id: 'account-1', holderName: 'Ana Lima', balance: 2500, currency: 'BRL' },
  { id: 'account-2', holderName: 'Bruno Costa', balance: 1800, currency: 'BRL' },
]

function transferPage(content: object[] = []) {
  return {
    content,
    page: 0,
    size: 5,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    first: true,
    last: true,
  }
}

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
      if (url.includes('/transfers?')) return Response.json(transferPage())
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
              originalTransferId: null,
              createdAt: '2026-08-10T22:00:00Z',
            },
          ]
          return Response.json(transfers[0], { status: 201 })
        }
        if (url.includes('/transfers?')) return Response.json(transferPage(transfers))
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

  it('confirms a reversal and refreshes the history with the compensating operation', async () => {
    const original: Transfer = {
      id: 'transfer-1',
      type: 'INTERNAL_TRANSFER',
      originalTransferId: null,
      sourceAccountId: 'account-1',
      destinationAccountId: 'account-2',
      amount: 50,
      currency: 'BRL',
      status: 'COMPLETED',
      createdAt: '2026-09-15T12:00:00Z',
    }
    let transfers: Transfer[] = [original]
    const fetchMock = vi.fn(async (input: RequestInfo | URL, options?: RequestInit) => {
      const url = String(input)
      if (url.endsWith('/auth/login')) {
        return Response.json({ accessToken: 'test-token', tokenType: 'Bearer', expiresIn: 900 })
      }
      if (url.endsWith('/accounts')) return Response.json(accounts)
      if (url.endsWith('/transfers/transfer-1/reversals') && options?.method === 'POST') {
        const reversal: Transfer = {
          ...original,
          id: 'reversal-1',
          type: 'REVERSAL',
          originalTransferId: original.id,
          sourceAccountId: original.destinationAccountId,
          destinationAccountId: original.sourceAccountId,
          createdAt: '2026-09-15T12:01:00Z',
        }
        transfers = [reversal, original]
        return Response.json(reversal, { status: 201 })
      }
      if (url.includes('/transfers?')) return Response.json(transferPage(transfers))
      return Response.json({}, { status: 404 })
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)
    await login()
    await screen.findByText('Ana Lima → Bruno Costa')

    await userEvent.click(screen.getByRole('button', { name: 'Estornar' }))
    expect(screen.getByRole('dialog')).toHaveTextContent(
      'A transferência original continuará no histórico.',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar estorno' }))

    expect(await screen.findByText('Transferência estornada com sucesso.')).toBeInTheDocument()
    expect(await screen.findByText('Estorno concluído')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Estornar' })).not.toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/transfers/transfer-1/reversals',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: 'Bearer test-token',
          'Idempotency-Key': expect.any(String),
        }),
      }),
    )
  })

  it('shows a reversal error and reuses the idempotency key on retry', async () => {
    const original: Transfer = {
      id: 'transfer-1',
      type: 'INTERNAL_TRANSFER',
      originalTransferId: null,
      sourceAccountId: 'account-1',
      destinationAccountId: 'account-2',
      amount: 50,
      currency: 'BRL',
      status: 'COMPLETED',
      createdAt: '2026-09-15T12:00:00Z',
    }
    const reversalKeys: string[] = []
    let reversalAttempts = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, options?: RequestInit) => {
        const url = String(input)
        if (url.endsWith('/auth/login')) {
          return Response.json({ accessToken: 'test-token', tokenType: 'Bearer', expiresIn: 900 })
        }
        if (url.endsWith('/accounts')) return Response.json(accounts)
        if (url.endsWith('/transfers/transfer-1/reversals') && options?.method === 'POST') {
          reversalAttempts += 1
          reversalKeys.push(String((options.headers as Record<string, string>)['Idempotency-Key']))
          if (reversalAttempts === 1) {
            return Response.json(
              { detail: 'Serviço temporariamente indisponível.' },
              { status: 503 },
            )
          }
          return Response.json(
            { ...original, id: 'reversal-1', type: 'REVERSAL', originalTransferId: original.id },
            { status: 201 },
          )
        }
        if (url.includes('/transfers?')) return Response.json(transferPage([original]))
        return Response.json({}, { status: 404 })
      }),
    )

    render(<App />)
    await login()
    await screen.findByText('Ana Lima → Bruno Costa')

    await userEvent.click(screen.getByRole('button', { name: 'Estornar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar estorno' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Serviço temporariamente indisponível.',
    )
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Estornar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar estorno' }))

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Transferência estornada com sucesso.',
    )
    expect(reversalKeys).toHaveLength(2)
    expect(reversalKeys[1]).toBe(reversalKeys[0])
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
        if (url.includes('/transfers?')) return Response.json(transferPage())
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
