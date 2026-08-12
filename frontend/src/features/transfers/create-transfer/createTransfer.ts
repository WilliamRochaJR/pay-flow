import type { Transfer } from '../list-transfers/listTransfers.model'
import type { CreateTransferInput } from './createTransfer.model'
import { sendTransfer } from './createTransfer.service'

type TransferSender = (input: CreateTransferInput) => Promise<Transfer>

export function createTransfer(input: CreateTransferInput, sender: TransferSender = sendTransfer) {
  if (input.sourceAccountId === input.destinationAccountId) {
    throw new Error('As contas de origem e destino devem ser diferentes.')
  }

  if (input.amount <= 0) {
    throw new Error('O valor da transferência deve ser maior que zero.')
  }

  return sender(input)
}
