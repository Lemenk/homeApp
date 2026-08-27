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

import { listBills, createBill, updateBill, deleteBill, billLogs } from '@/api/bill'

describe('Phase 4 账单 API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('listBills 调用正确端点并透传筛选参数', async () => {
    httpMock.get.mockResolvedValue({ list: [], total: 0, page: 1, size: 20 })
    await listBills(1, { type: 'expense', keyword: '早餐' })
    expect(httpMock.get).toHaveBeenCalledWith('/ledgers/1/bills', { params: { type: 'expense', keyword: '早餐' } })
  })

  it('createBill 透传多账户组合付款明细', async () => {
    httpMock.post.mockResolvedValue({ id: 1 })
    const data = {
      type: 'expense' as const,
      categoryId: 5,
      amount: 300,
      items: [
        { accountId: 10, direction: 'out' as const, amount: 200 },
        { accountId: 11, direction: 'out' as const, amount: 100 },
      ],
    }
    await createBill(1, data)
    expect(httpMock.post).toHaveBeenCalledWith('/ledgers/1/bills', data)
  })

  it('updateBill / deleteBill / billLogs 调用正确端点', async () => {
    httpMock.put.mockResolvedValue({ id: 9 })
    httpMock.delete.mockResolvedValue(undefined)
    httpMock.get.mockResolvedValue([])

    await updateBill(9, { amount: 50 })
    expect(httpMock.put).toHaveBeenCalledWith('/bills/9', { amount: 50 })

    await deleteBill(9)
    expect(httpMock.delete).toHaveBeenCalledWith('/bills/9')

    await billLogs(9)
    expect(httpMock.get).toHaveBeenCalledWith('/bills/9/logs')
  })
})
