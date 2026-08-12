import { request } from '../../../shared/api/httpClient'
import type { Account } from './listAccounts.model'

export function listAccounts() {
  return request<Account[]>('/api/v1/accounts')
}
