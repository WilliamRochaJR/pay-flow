import { afterEach, describe, expect, it, vi } from 'vitest'
import { reverseTransfer } from './reverseTransfer.service'

describe('reverseTransfer', () => {
  afterEach(() => vi.restoreAllMocks())

  it('sends the transfer identifier, authentication and idempotency key', async () => {
    const fetchMock = vi.fn(async () => Response.json({ id: 'reversal-1' }, { status: 201 }))
    vi.stubGlobal('fetch', fetchMock)

    await reverseTransfer('transfer-1', 'key-1', 'token-1')

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/transfers/transfer-1/reversals', {
      method: 'POST',
      headers: {
        Authorization: 'Bearer token-1',
        'Content-Type': 'application/json',
        'Idempotency-Key': 'key-1',
      },
    })
  })
})
