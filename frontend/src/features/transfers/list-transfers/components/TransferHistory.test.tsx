import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { TransferHistory } from './TransferHistory'

const accounts = [
  { id: 'account-1', holderName: 'Ana Lima', balance: 100, currency: 'BRL' },
  { id: 'account-2', holderName: 'Bruno Costa', balance: 200, currency: 'BRL' },
]

describe('TransferHistory', () => {
  afterEach(cleanup)

  it('applies date filters and changes pages', async () => {
    const user = userEvent.setup()
    const onFilter = vi.fn()
    const onPageChange = vi.fn()
    render(
      <TransferHistory
        accounts={accounts}
        transfers={{
          content: [
            {
              id: 'transfer-1',
              type: 'INTERNAL_TRANSFER',
              originalTransferId: null,
              sourceAccountId: 'account-1',
              destinationAccountId: 'account-2',
              amount: 50,
              currency: 'BRL',
              status: 'COMPLETED',
              createdAt: '2026-09-15T12:00:00Z',
            },
          ],
          page: 0,
          size: 1,
          totalElements: 2,
          totalPages: 2,
          first: true,
          last: false,
        }}
        filters={{}}
        onFilter={onFilter}
        onPageChange={onPageChange}
        reversingTransferId=""
        onReverse={async () => true}
      />,
    )

    await user.type(screen.getByLabelText('De'), '2026-09-01')
    await user.type(screen.getByLabelText('Até'), '2026-09-30')
    await user.click(screen.getByRole('button', { name: 'Filtrar' }))
    await user.click(screen.getByRole('button', { name: 'Próxima' }))

    expect(onFilter).toHaveBeenCalledWith({ from: '2026-09-01', to: '2026-09-30' })
    expect(onPageChange).toHaveBeenCalledWith(1)
    expect(screen.getByText('Ana Lima → Bruno Costa')).toBeInTheDocument()
  })
})
