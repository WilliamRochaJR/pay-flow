import { afterEach, describe, expect, it, vi } from 'vitest'
import { listTransfers } from './listTransfers.service'

describe('listTransfers', () => {
  afterEach(() => vi.restoreAllMocks())

  it('sends pagination and inclusive date filters', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      Response.json({
        content: [],
        page: 1,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        first: false,
        last: true,
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await listTransfers('token', 1, 10, { from: '2026-09-01', to: '2026-09-30' })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/transfers?page=1&size=10&from=2026-09-01T00%3A00%3A00Z&to=2026-09-30T23%3A59%3A59.999Z',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer token' }),
      }),
    )
  })
})
