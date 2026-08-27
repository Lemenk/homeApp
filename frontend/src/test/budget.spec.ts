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

import { listBudgets, createBudget, updateBudget, deleteBudget } from '@/api/budget'

describe('Phase 6 预算 API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('listBudgets 调用正确端点', async () => {
    httpMock.get.mockResolvedValue([])
    await listBudgets(1)
    expect(httpMock.get).toHaveBeenCalledWith('/ledgers/1/budgets')
  })

  it('createBudget 透传自定义周期', async () => {
    httpMock.post.mockResolvedValue({ id: 1 })
    await createBudget(1, {
      categoryId: 5,
      periodType: 'custom',
      startDate: '2026-08-01',
      endDate: '2026-08-31',
      amount: 500,
    })
    expect(httpMock.post).toHaveBeenCalledWith('/ledgers/1/budgets', {
      categoryId: 5,
      periodType: 'custom',
      startDate: '2026-08-01',
      endDate: '2026-08-31',
      amount: 500,
    })
  })

  it('updateBudget / deleteBudget 调用正确端点', async () => {
    httpMock.put.mockResolvedValue({ id: 3 })
    httpMock.delete.mockResolvedValue(undefined)
    await updateBudget(3, { categoryId: 5, periodType: 'monthly', amount: 300 })
    expect(httpMock.put).toHaveBeenCalledWith('/budgets/3', {
      categoryId: 5,
      periodType: 'monthly',
      amount: 300,
    })
    await deleteBudget(3)
    expect(httpMock.delete).toHaveBeenCalledWith('/budgets/3')
  })
})
