import { request } from '../../../shared/api/httpClient'
import type { Transfer } from './listTransfers.model'

export function listTransfers() {
  return request<Transfer[]>('/api/v1/transfers')
}
