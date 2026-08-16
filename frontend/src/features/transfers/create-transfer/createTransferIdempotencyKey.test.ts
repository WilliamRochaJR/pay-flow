import { describe, expect, it, vi } from 'vitest'
import { createTransferIdempotencyKey } from './createTransferIdempotencyKey'

describe('createTransferIdempotencyKey', () => {
  it('uses the native UUID implementation when it is available', () => {
    const randomUUID = vi.fn(
      () =>
        '123e4567-e89b-42d3-a456-426614174000' as `${string}-${string}-${string}-${string}-${string}`,
    )
    const provider = { randomUUID, getRandomValues: vi.fn() } as unknown as Crypto

    expect(createTransferIdempotencyKey(provider)).toBe('123e4567-e89b-42d3-a456-426614174000')
    expect(randomUUID).toHaveBeenCalledOnce()
    expect(provider.getRandomValues).not.toHaveBeenCalled()
  })

  it('creates a UUID v4 with random bytes when randomUUID is unavailable', () => {
    const provider = {
      getRandomValues: (bytes: Uint8Array) => {
        bytes.set(Array.from({ length: 16 }, (_, index) => index))
        return bytes
      },
    } as Crypto

    expect(createTransferIdempotencyKey(provider)).toBe('00010203-0405-4607-8809-0a0b0c0d0e0f')
  })
})
