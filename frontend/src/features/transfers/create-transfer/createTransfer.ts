import type { Transfer } from '../list-transfers/listTransfers.model'
import type { CreateTransferInput } from './createTransfer.model'
import { sendTransfer } from './createTransfer.service'

type TransferSender = (input: CreateTransferInput, idempotencyKey: string) => Promise<Transfer>

export function createTransfer(
  input: CreateTransferInput,
  idempotencyKey: string,
  accessToken?: string,
  sender: TransferSender = (transfer, key) => sendTransfer(transfer, key, accessToken),
) {
  if (input.sourceAccountId === input.destinationAccountId) {
    throw new Error('As contas de origem e destino devem ser diferentes.')
  }

  if (input.amount <= 0) {
    throw new Error('O valor da transferência deve ser maior que zero.')
  }

  return sender(input, idempotencyKey)
}
