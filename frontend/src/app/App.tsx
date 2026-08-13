import { useCallback, useEffect, useRef, useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { AuthForm } from '../features/auth/components/AuthForm'
import { authenticate } from '../features/auth/auth.service'
import type { AuthInput, AuthMode, AuthResult } from '../features/auth/auth.model'
import {
  clearAccessToken,
  readAccessToken,
  saveAccessToken,
} from '../features/auth/authSession.service'
import { AccountList } from '../features/accounts/list-accounts/components/AccountList'
import type { Account } from '../features/accounts/list-accounts/listAccounts.model'
import { listAccounts } from '../features/accounts/list-accounts/listAccounts.service'
import { TransferForm } from '../features/transfers/create-transfer/components/TransferForm'
import { createTransfer } from '../features/transfers/create-transfer/createTransfer'
import type { CreateTransferInput } from '../features/transfers/create-transfer/createTransfer.model'
import { TransferHistory } from '../features/transfers/list-transfers/components/TransferHistory'
import type { Transfer } from '../features/transfers/list-transfers/listTransfers.model'
import { listTransfers } from '../features/transfers/list-transfers/listTransfers.service'
import { formatCurrency } from '../shared/formatters/currency'
import { ApiError } from '../shared/api/httpClient'
import './App.css'
import '../features/transfers/transfers.css'

function AppRoutes() {
  const navigate = useNavigate()
  const [accessToken, setAccessToken] = useState(readAccessToken)
  const [authEmail, setAuthEmail] = useState('')
  const [accounts, setAccounts] = useState<Account[]>([])
  const [transfers, setTransfers] = useState<Transfer[]>([])
  const [sourceId, setSourceId] = useState('')
  const [destinationId, setDestinationId] = useState('')
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const pendingTransfer = useRef<{ fingerprint: string; key: string } | null>(null)

  const loadDashboard = useCallback(async () => {
    if (!accessToken) return
    const [accountData, transferData] = await Promise.all([
      listAccounts(accessToken),
      listTransfers(accessToken),
    ])
    setAccounts(accountData)
    setTransfers(transferData)
    setSourceId((current) => current || accountData[0]?.id || '')
    setDestinationId((current) => current || accountData[1]?.id || '')
  }, [accessToken])

  useEffect(() => {
    if (!accessToken) return
    setLoading(true)
    loadDashboard()
      .catch((cause: Error) => {
        if (cause instanceof ApiError && cause.status === 401) {
          clearAccessToken()
          setAccessToken('')
          setError('Sua sessão expirou. Entre novamente.')
          return
        }
        setError(cause.message)
      })
      .finally(() => setLoading(false))
  }, [accessToken, loadDashboard])

  async function submitAuth(input: AuthInput, mode: AuthMode): Promise<AuthResult> {
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const response = await authenticate(input, mode === 'register')
      if (!response) {
        setAuthEmail(input.email)
        setSuccess('Conta criada com sucesso. Entre com seu e-mail e senha.')
        navigate('/login', { replace: true })
        return 'registered'
      }
      saveAccessToken(response.accessToken)
      setAccessToken(response.accessToken)
      navigate('/dashboard', { replace: true })
      return 'authenticated'
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível autenticar.')
      return 'failed'
    } finally {
      setSubmitting(false)
    }
  }

  function changeSource(nextSourceId: string) {
    setSourceId(nextSourceId)
    if (nextSourceId === destinationId) {
      const nextDestination = accounts.find((account) => account.id !== nextSourceId)
      setDestinationId(nextDestination?.id ?? '')
    }
  }

  function logout() {
    clearAccessToken()
    setAccessToken('')
    setAccounts([])
    setTransfers([])
    setSuccess('')
    setError('')
    navigate('/login', { replace: true })
  }

  async function submitTransfer(input: CreateTransferInput): Promise<boolean> {
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      const fingerprint = JSON.stringify(input)
      if (pendingTransfer.current?.fingerprint !== fingerprint) {
        pendingTransfer.current = { fingerprint, key: crypto.randomUUID() }
      }
      await createTransfer(input, pendingTransfer.current.key, accessToken)
      pendingTransfer.current = null
      setSuccess('Transferência concluída com sucesso.')
      await loadDashboard()
      return true
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : 'Não foi possível concluir a transferência.',
      )
      return false
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <main className="center-state">Carregando PayFlow…</main>
  }

  const totalBalance = accounts.reduce((total, account) => total + Number(account.balance), 0)

  const dashboard = (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#top" aria-label="PayFlow, início">
          <span className="brand-mark">P</span>
          <span>PayFlow</span>
        </a>
        <div className="session-actions">
          <span className="environment">
            <i /> Ambiente local
          </span>
          <button type="button" onClick={logout}>
            Sair
          </button>
        </div>
      </header>

      <main id="top">
        <section className="hero">
          <div>
            <span className="eyebrow">VISÃO GERAL</span>
            <h1>Seu dinheiro, em movimento.</h1>
            <p>Uma demonstração simples de transferências internas em tempo real.</p>
          </div>
          <div className="total-card">
            <span>Saldo total das contas</span>
            <strong>{formatCurrency(totalBalance)}</strong>
            <small>Valores fictícios para demonstração</small>
          </div>
        </section>

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

        <AccountList accounts={accounts} />

        <section className="workspace">
          <TransferForm
            accounts={accounts}
            sourceId={sourceId}
            destinationId={destinationId}
            submitting={submitting}
            onSourceChange={changeSource}
            onDestinationChange={setDestinationId}
            onSubmit={submitTransfer}
          />
          <TransferHistory accounts={accounts} transfers={transfers} />
        </section>
      </main>

      <footer>PayFlow M0 · Ambiente demonstrativo · Nenhum valor real é movimentado</footer>
    </div>
  )

  const authForm = (mode: AuthMode) => (
    <AuthForm
      key={mode}
      mode={mode}
      initialEmail={authEmail}
      submitting={submitting}
      error={error}
      success={success}
      onSubmit={submitAuth}
    />
  )

  return (
    <Routes>
      <Route path="/" element={<Navigate replace to={accessToken ? '/dashboard' : '/login'} />} />
      <Route
        path="/login"
        element={accessToken ? <Navigate replace to="/dashboard" /> : authForm('login')}
      />
      <Route
        path="/register"
        element={accessToken ? <Navigate replace to="/dashboard" /> : authForm('register')}
      />
      <Route
        path="/dashboard"
        element={accessToken ? dashboard : <Navigate replace to="/login" />}
      />
      <Route path="*" element={<Navigate replace to="/" />} />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}
