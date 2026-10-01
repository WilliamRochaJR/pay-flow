import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ReverseTransferAction } from './ReverseTransferAction'

const transfer = {
  id: 'transfer-1',
  type: 'INTERNAL_TRANSFER' as const,
  originalTransferId: null,
  sourceAccountId: 'account-1',
  destinationAccountId: 'account-2',
  amount: 50,
  currency: 'BRL',
  status: 'COMPLETED' as const,
  createdAt: '2026-09-15T12:00:00Z',
}

describe('ReverseTransferAction', () => {
  afterEach(cleanup)

  it('requires confirmation before requesting a reversal', async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn(async () => true)
    render(<ReverseTransferAction transfer={transfer} submitting={false} onConfirm={onConfirm} />)

    await user.click(screen.getByRole('button', { name: 'Estornar' }))

    expect(screen.getByRole('dialog')).toHaveTextContent(
      'A transferência original continuará no histórico.',
    )
    expect(onConfirm).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Confirmar estorno' }))

    expect(onConfirm).toHaveBeenCalledWith('transfer-1')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('closes the confirmation so the dashboard can show an operation error', async () => {
    const user = userEvent.setup()
    render(
      <ReverseTransferAction
        transfer={transfer}
        submitting={false}
        onConfirm={async () => false}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Estornar' }))
    await user.click(screen.getByRole('button', { name: 'Confirmar estorno' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
