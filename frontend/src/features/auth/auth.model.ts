export type AuthMode = 'login' | 'register'

export type AuthResult = 'authenticated' | 'registered' | 'failed'

export type AuthInput = {
  name?: string
  email: string
  password: string
}

export type TokenResponse = {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
}
