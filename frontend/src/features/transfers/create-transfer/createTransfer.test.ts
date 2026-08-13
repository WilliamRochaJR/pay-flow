import { describe, expect, it, vi } from 'vitest'
import type { Transfer } from '../list-transfers/listTransfers.model'
import { createTransfer } from './createTransfer'
import type { CreateTransferInput } from './createTransfer.model'

const validInput: CreateTransferInput = {
  sourceAccountId: 'account-1',
  destinationAccountId: 'account-2',
  amount: 50,
  currency: 'BRL',
}

describe('createTransfer', () => {
  it('rejects equal source and destination accounts', () => {
    expect(() => createTransfer({ ...validInput, destinationAccountId: 'account-1' })).toThrow(
      'As contas de origem e destino devem ser diferentes.',
    )
  })

  it('rejects an amount that is not positive', () => {
    expect(() => createTransfer({ ...validInput, amount: 0 })).toThrow(
      'O valor da transferência deve ser maior que zero.',
    )
  })

  it('sends a valid transfer to the service', async () => {
    const transfer = { id: 'transfer-1' } as Transfer
    const sender = vi.fn().mockResolvedValue(transfer)

    await expect(createTransfer(validInput, undefined, sender)).resolves.toBe(transfer)
    expect(sender).toHaveBeenCalledWith(validInput)
  })
})
