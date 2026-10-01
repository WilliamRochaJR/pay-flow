import { useState } from 'react'
import { formatCurrency } from '../../../../shared/formatters/currency'
import type { Transfer } from '../../list-transfers/listTransfers.model'

type ReverseTransferActionProps = {
  transfer: Transfer
  submitting: boolean
  onConfirm: (transferId: string) => Promise<boolean>
}

export function ReverseTransferAction({
  transfer,
  submitting,
  onConfirm,
}: ReverseTransferActionProps) {
  const [confirming, setConfirming] = useState(false)

  async function confirm() {
    await onConfirm(transfer.id)
    setConfirming(false)
  }

  return (
    <>
      <button className="reverse-trigger" type="button" onClick={() => setConfirming(true)}>
        Estornar
      </button>
      {confirming && (
        <div className="reversal-backdrop">
          <section
            className="reversal-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby={`reversal-title-${transfer.id}`}
          >
            <span className="eyebrow">CONFIRMAÇÃO</span>
            <h3 id={`reversal-title-${transfer.id}`}>Estornar transferência?</h3>
            <p>
              Uma nova operação de <strong>{formatCurrency(transfer.amount)}</strong> será criada no
              sentido contrário. A transferência original continuará no histórico.
            </p>
            <div className="reversal-dialog-actions">
              <button type="button" disabled={submitting} onClick={() => setConfirming(false)}>
                Cancelar
              </button>
              <button type="button" disabled={submitting} onClick={confirm}>
                {submitting ? 'Estornando…' : 'Confirmar estorno'}
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  )
}
