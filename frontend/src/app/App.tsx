import { useCallback, useEffect, useState } from 'react'
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
import './App.css'
import '../features/transfers/transfers.css'

export default function App() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [transfers, setTransfers] = useState<Transfer[]>([])
  const [sourceId, setSourceId] = useState('')
  const [destinationId, setDestinationId] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const loadDashboard = useCallback(async () => {
    const [accountData, transferData] = await Promise.all([listAccounts(), listTransfers()])
    setAccounts(accountData)
    setTransfers(transferData)
    setSourceId((current) => current || accountData[0]?.id || '')
    setDestinationId((current) => current || accountData[1]?.id || '')
  }, [])

  useEffect(() => {
    loadDashboard()
      .catch((cause: Error) => setError(cause.message))
      .finally(() => setLoading(false))
  }, [loadDashboard])

  function changeSource(nextSourceId: string) {
    setSourceId(nextSourceId)
    if (nextSourceId === destinationId) {
      const nextDestination = accounts.find((account) => account.id !== nextSourceId)
      setDestinationId(nextDestination?.id ?? '')
    }
  }

  async function submitTransfer(input: CreateTransferInput): Promise<boolean> {
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      await createTransfer(input)
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

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#top" aria-label="PayFlow, início">
          <span className="brand-mark">P</span>
          <span>PayFlow</span>
        </a>
        <span className="environment">
          <i /> Ambiente local
        </span>
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
}
