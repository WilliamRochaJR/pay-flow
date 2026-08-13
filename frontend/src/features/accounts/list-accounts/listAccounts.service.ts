import { request } from '../../../shared/api/httpClient'
import type { Account } from './listAccounts.model'

export function listAccounts(accessToken: string) {
  return request<Account[]>('/api/v1/accounts', undefined, accessToken)
}
