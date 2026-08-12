import { useState, type FormEvent } from 'react'
import type { Account } from '../../../accounts/list-accounts/listAccounts.model'
import { formatCurrency } from '../../../../shared/formatters/currency'
import type { CreateTransferInput } from '../createTransfer.model'

type TransferFormProps = {
  accounts: Account[]
  sourceId: string
  destinationId: string
  submitting: boolean
  onSourceChange: (accountId: string) => void
  onDestinationChange: (accountId: string) => void
  onSubmit: (input: CreateTransferInput) => Promise<boolean>
}

export function TransferForm({
  accounts,
  sourceId,
  destinationId,
  submitting,
  onSourceChange,
  onDestinationChange,
  onSubmit,
}: TransferFormProps) {
  const [amount, setAmount] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    const created = await onSubmit({
      sourceAccountId: sourceId,
      destinationAccountId: destinationId,
      amount: Number(amount),
      currency: 'BRL',
    })
    if (created) setAmount('')
  }

  return (
    <form className="transfer-card" onSubmit={submit}>
      <span className="eyebrow">NOVA OPERAÇÃO</span>
      <h2>Fazer transferência</h2>
      <label>
        Conta de origem
        <select value={sourceId} onChange={(event) => onSourceChange(event.target.value)}>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.holderName} — {formatCurrency(account.balance)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Conta de destino
        <select value={destinationId} onChange={(event) => onDestinationChange(event.target.value)}>
          {accounts.map((account) => (
            <option key={account.id} value={account.id} disabled={account.id === sourceId}>
              {account.holderName}
            </option>
          ))}
        </select>
      </label>
      <label>
        Valor
        <div className="amount-input">
          <span>R$</span>
          <input
            aria-label="Valor"
            type="number"
            min="0.01"
            step="0.01"
            placeholder="0,00"
            required
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
          />
        </div>
      </label>
      <button
        disabled={submitting || sourceId === destinationId}
        aria-busy={submitting}
        data-submitting={submitting}
        type="submit"
      >
        {submitting ? 'Transferindo…' : 'Transferir agora'} <span>→</span>
      </button>
      <small>Transferências são processadas de forma atômica e não podem ser editadas.</small>
    </form>
  )
}
