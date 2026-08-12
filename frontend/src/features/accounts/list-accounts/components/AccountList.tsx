import { AccountCard } from './AccountCard'
import type { Account } from '../listAccounts.model'
import '../listAccounts.css'

type AccountListProps = {
  accounts: Account[]
}

export function AccountList({ accounts }: AccountListProps) {
  return (
    <section aria-labelledby="accounts-heading">
      <div className="section-heading">
        <div>
          <span className="eyebrow">CARTEIRAS</span>
          <h2 id="accounts-heading">Contas disponíveis</h2>
        </div>
        <span>{accounts.length} contas</span>
      </div>
      <div className="account-grid">
        {accounts.map((account, index) => (
          <AccountCard account={account} index={index} key={account.id} />
        ))}
      </div>
    </section>
  )
}
