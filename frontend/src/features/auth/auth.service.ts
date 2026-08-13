import { request } from '../../shared/api/httpClient'
import type { AuthInput, TokenResponse } from './auth.model'

export async function authenticate(input: AuthInput, register: boolean) {
  if (register) {
    await request('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(input),
    })
    return null
  }
  return request<TokenResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email: input.email, password: input.password }),
  })
}
