import { formatCurrency } from '../../../../shared/formatters/currency'
import type { Account } from '../listAccounts.model'

type AccountCardProps = {
  account: Account
  index: number
}

export function AccountCard({ account, index }: AccountCardProps) {
  return (
    <article className="account-card">
      <div className={`avatar avatar-${index + 1}`}>{account.holderName.charAt(0)}</div>
      <div className="account-person">
        <strong>{account.holderName}</strong>
        <span>Conta • {account.id.slice(0, 6)}</span>
      </div>
      <div className="account-balance">
        <span>Saldo disponível</span>
        <strong>{formatCurrency(account.balance)}</strong>
      </div>
    </article>
  )
}
