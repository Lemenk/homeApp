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

import { listAccounts, createAccount, adjustBalance, updateAccount, summary } from '@/api/account'

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

  it('createAccount 透传类型与余额', async () => {
    httpMock.post.mockResolvedValue({ id: 1 })
    await createAccount(1, { name: '信用卡', type: 'credit', balance: 0 })
    expect(httpMock.post).toHaveBeenCalledWith('/ledgers/1/accounts', {
      name: '信用卡',
      type: 'credit',
      balance: 0,
    })
  })

  it('adjustBalance 携带新余额与原因', async () => {
    httpMock.post.mockResolvedValue({ id: 1, balance: 950 })
    await adjustBalance(7, { newBalance: 950, reason: '现金清点' })
    expect(httpMock.post).toHaveBeenCalledWith('/accounts/7/balance', { newBalance: 950, reason: '现金清点' })
  })

  it('updateAccount 调用 PUT 并透传名称/类型/余额/备注/计入总资产', async () => {
    httpMock.put.mockResolvedValue({ id: 1, name: '工资卡', type: 'credit', balance: 1200 })
    await updateAccount(3, {
      name: '工资卡',
      type: 'credit',
      remark: '每月工资',
      includeInTotal: 0,
      balance: 1200,
    })
    expect(httpMock.put).toHaveBeenCalledWith('/accounts/3', {
      name: '工资卡',
      type: 'credit',
      remark: '每月工资',
      includeInTotal: 0,
      balance: 1200,
    })
  })
})
