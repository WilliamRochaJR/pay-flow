import { useMemo } from 'react'
import type { Account } from '../../../accounts/list-accounts/listAccounts.model'
import { formatCurrency } from '../../../../shared/formatters/currency'
import { formatDateTime } from '../../../../shared/formatters/dateTime'
import type { Transfer } from '../listTransfers.model'

type TransferHistoryProps = {
  accounts: Account[]
  transfers: Transfer[]
}

export function TransferHistory({ accounts, transfers }: TransferHistoryProps) {
  const accountNames = useMemo(
    () => new Map(accounts.map((account) => [account.id, account.holderName])),
    [accounts],
  )

  return (
    <section className="history-card" aria-labelledby="history-heading">
      <div className="section-heading">
        <div>
          <span className="eyebrow">ATIVIDADE</span>
          <h2 id="history-heading">Histórico recente</h2>
        </div>
      </div>
      {transfers.length === 0 ? (
        <div className="empty">
          <span>↗</span>
          <strong>Nenhuma transferência ainda</strong>
          <p>As operações concluídas aparecerão aqui.</p>
        </div>
      ) : (
        <div className="transfer-list">
          {transfers.map((transfer) => (
            <article key={transfer.id}>
              <span className="transfer-icon">↗</span>
              <div>
                <strong>
                  {accountNames.get(transfer.sourceAccountId)} →{' '}
                  {accountNames.get(transfer.destinationAccountId)}
                </strong>
                <span>{formatDateTime(new Date(transfer.createdAt))}</span>
              </div>
              <div className="transfer-amount">
                <strong>{formatCurrency(transfer.amount)}</strong>
                <span>Concluída</span>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
