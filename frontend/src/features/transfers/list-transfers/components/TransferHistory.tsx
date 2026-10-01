import { useEffect, useMemo, useState } from 'react'
import type { Account } from '../../../accounts/list-accounts/listAccounts.model'
import { formatCurrency } from '../../../../shared/formatters/currency'
import { formatDateTime } from '../../../../shared/formatters/dateTime'
import type { TransferFilters, TransferPage } from '../listTransfers.model'
import { ReverseTransferAction } from '../../reverse-transfer/components/ReverseTransferAction'

type TransferHistoryProps = {
  accounts: Account[]
  transfers: TransferPage
  filters: TransferFilters
  onFilter: (filters: TransferFilters) => void
  onPageChange: (page: number) => void
  reversingTransferId: string
  onReverse: (transferId: string) => Promise<boolean>
}

export function TransferHistory({
  accounts,
  transfers,
  filters,
  onFilter,
  onPageChange,
  reversingTransferId,
  onReverse,
}: TransferHistoryProps) {
  const [from, setFrom] = useState(filters.from ?? '')
  const [to, setTo] = useState(filters.to ?? '')
  const accountNames = useMemo(
    () => new Map(accounts.map((account) => [account.id, account.holderName])),
    [accounts],
  )
  const reversedTransferIds = useMemo(
    () => new Set(transfers.content.flatMap((transfer) => transfer.originalTransferId ?? [])),
    [transfers.content],
  )

  useEffect(() => {
    setFrom(filters.from ?? '')
    setTo(filters.to ?? '')
  }, [filters])

  function submitFilters(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onFilter({ from: from || undefined, to: to || undefined })
  }

  return (
    <section className="history-card" aria-labelledby="history-heading">
      <div className="section-heading">
        <div>
          <span className="eyebrow">ATIVIDADE</span>
          <h2 id="history-heading">Histórico recente</h2>
        </div>
      </div>
      <form className="history-filters" onSubmit={submitFilters}>
        <label>
          De
          <input
            type="date"
            value={from}
            max={to || undefined}
            onChange={(event) => setFrom(event.target.value)}
          />
        </label>
        <label>
          Até
          <input
            type="date"
            value={to}
            min={from || undefined}
            onChange={(event) => setTo(event.target.value)}
          />
        </label>
        <button type="submit">Filtrar</button>
        <button
          type="button"
          onClick={() => {
            setFrom('')
            setTo('')
            onFilter({})
          }}
        >
          Limpar
        </button>
      </form>
      {transfers.content.length === 0 ? (
        <div className="empty">
          <span>↗</span>
          <strong>Nenhuma transferência ainda</strong>
          <p>As operações concluídas aparecerão aqui.</p>
        </div>
      ) : (
        <div className="transfer-list">
          {transfers.content.map((transfer) => (
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
                <span>{transfer.type === 'REVERSAL' ? 'Estorno concluído' : 'Concluída'}</span>
                {transfer.type === 'INTERNAL_TRANSFER' && !reversedTransferIds.has(transfer.id) && (
                  <ReverseTransferAction
                    transfer={transfer}
                    submitting={reversingTransferId === transfer.id}
                    onConfirm={onReverse}
                  />
                )}
              </div>
            </article>
          ))}
        </div>
      )}
      {transfers.totalPages > 1 && (
        <nav className="history-pagination" aria-label="Paginação do histórico">
          <button
            type="button"
            disabled={transfers.first}
            onClick={() => onPageChange(transfers.page - 1)}
          >
            Anterior
          </button>
          <span>
            Página {transfers.page + 1} de {transfers.totalPages}
          </span>
          <button
            type="button"
            disabled={transfers.last}
            onClick={() => onPageChange(transfers.page + 1)}
          >
            Próxima
          </button>
        </nav>
      )}
    </section>
  )
}
