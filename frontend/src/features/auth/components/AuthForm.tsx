import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import type { AuthInput, AuthMode, AuthResult } from '../auth.model'
import '../auth.css'

type AuthFormProps = {
  submitting: boolean
  error: string
  success: string
  mode: AuthMode
  initialEmail?: string
  onSubmit: (input: AuthInput, mode: AuthMode) => Promise<AuthResult>
}

export function AuthForm({
  submitting,
  error,
  success,
  mode,
  initialEmail = '',
  onSubmit,
}: AuthFormProps) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState(initialEmail)
  const [password, setPassword] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    const result = await onSubmit(
      { name: mode === 'register' ? name : undefined, email, password },
      mode,
    )
    if (result === 'registered') {
      setName('')
      setPassword('')
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-intro">
        <span className="brand auth-brand">
          <span className="brand-mark">P</span>PayFlow
        </span>
        <span className="eyebrow">AMBIENTE DEMONSTRATIVO</span>
        <h1>Transferências simples, acesso protegido.</h1>
        <p>
          Crie seu usuário e receba duas carteiras com saldo fictício para explorar o fluxo
          completo.
        </p>
      </section>
      <form className="auth-card" onSubmit={submit}>
        <div className="auth-tabs" aria-label="Escolha a forma de acesso">
          <Link aria-current={mode === 'login' ? 'page' : undefined} to="/login">
            Entrar
          </Link>
          <Link aria-current={mode === 'register' ? 'page' : undefined} to="/register">
            Criar conta
          </Link>
        </div>
        <h2>{mode === 'login' ? 'Acesse sua conta' : 'Comece sua demonstração'}</h2>
        {mode === 'register' && (
          <label>
            Nome
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              required
              maxLength={100}
            />
          </label>
        )}
        <label>
          E-mail
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </label>
        <label>
          Senha
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            minLength={8}
            maxLength={72}
          />
        </label>
        {error && (
          <div className="message error" role="alert">
            {error}
          </div>
        )}
        {success && (
          <div className="message success" role="status">
            {success}
          </div>
        )}
        <button className="auth-submit" disabled={submitting} type="submit">
          {submitting ? 'Aguarde…' : mode === 'login' ? 'Entrar' : 'Criar conta'}
        </button>
        <small>Os valores e as operações são exclusivamente fictícios.</small>
      </form>
    </main>
  )
}
