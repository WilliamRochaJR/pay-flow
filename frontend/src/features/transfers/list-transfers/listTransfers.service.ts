import { request } from '../../../shared/api/httpClient'
import type { TransferFilters, TransferPage } from './listTransfers.model'

export function listTransfers(
  accessToken: string,
  page = 0,
  size = 5,
  filters: TransferFilters = {},
) {
  const search = new URLSearchParams({ page: String(page), size: String(size) })
  if (filters.from) search.set('from', `${filters.from}T00:00:00Z`)
  if (filters.to) search.set('to', `${filters.to}T23:59:59.999Z`)
  return request<TransferPage>(`/api/v1/transfers?${search}`, undefined, accessToken)
}
