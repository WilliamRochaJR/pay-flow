import { request } from '../../../shared/api/httpClient'
import type { Transfer } from '../list-transfers/listTransfers.model'
import type { CreateTransferInput } from './createTransfer.model'

export function sendTransfer(input: CreateTransferInput, accessToken?: string) {
  return request<Transfer>(
    '/api/v1/transfers',
    {
      method: 'POST',
      body: JSON.stringify(input),
    },
    accessToken,
  )
}
