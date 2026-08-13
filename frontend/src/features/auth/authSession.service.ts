const ACCESS_TOKEN_KEY = 'payflow.access-token'

export function readAccessToken(): string {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY) ?? ''
}

export function saveAccessToken(accessToken: string): void {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
}

export function clearAccessToken(): void {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
}
