import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

const accounts = [
  { id: 'account-1', holderName: 'Ana Lima', balance: 2500, currency: 'BRL' },
  { id: 'account-2', holderName: 'Bruno Costa', balance: 1800, currency: 'BRL' },
]

describe('PayFlow dashboard', () => {
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('creates a transfer and refreshes the history', async () => {
    let transfers: object[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, options?: RequestInit) => {
        const url = String(input)
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
        if (url.endsWith('/accounts')) return Response.json(accounts)
        if (url.endsWith('/transfers')) return Response.json([])
        return Response.json({}, { status: 404 })
      }),
    )

    render(<App />)
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
