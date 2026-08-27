import { describe, it, expect, vi, beforeEach } from 'vitest'

const { httpMock } = vi.hoisted(() => ({
  httpMock: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/api/http', () => ({ http: httpMock }))

import { listAccounts, createAccount, adjustBalance, summary } from '@/api/account'

describe('Phase 5 账户 API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('listAccounts / summary 调用正确端点', async () => {
    httpMock.get.mockResolvedValue([])
    await listAccounts(1)
    expect(httpMock.get).toHaveBeenCalledWith('/ledgers/1/accounts')

    await summary(1)
    expect(httpMock.get).toHaveBeenCalledWith('/ledgers/1/accounts/summary')
  })

  it('createAccount 透传类型与期初余额', async () => {
    httpMock.post.mockResolvedValue({ id: 1 })
    await createAccount(1, { name: '信用卡', type: 'liability', initialBalance: 0 })
    expect(httpMock.post).toHaveBeenCalledWith('/ledgers/1/accounts', {
      name: '信用卡',
      type: 'liability',
      initialBalance: 0,
    })
  })

  it('adjustBalance 携带新余额与原因', async () => {
    httpMock.post.mockResolvedValue({ id: 1, balance: 950 })
    await adjustBalance(7, { newBalance: 950, reason: '现金清点' })
    expect(httpMock.post).toHaveBeenCalledWith('/accounts/7/balance', { newBalance: 950, reason: '现金清点' })
  })
})
