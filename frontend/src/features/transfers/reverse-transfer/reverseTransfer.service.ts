import { request } from '../../../shared/api/httpClient'
import type { Transfer } from '../list-transfers/listTransfers.model'

export function reverseTransfer(transferId: string, idempotencyKey: string, accessToken: string) {
  return request<Transfer>(
    `/api/v1/transfers/${transferId}/reversals`,
    {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
    },
    accessToken,
  )
}
