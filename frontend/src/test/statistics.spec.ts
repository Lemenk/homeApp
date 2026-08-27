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

import { trend, categoryStat } from '@/api/statistics'

describe('Phase 7 统计 API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('trend 调用正确端点并透传分组', async () => {
    httpMock.get.mockResolvedValue([])
    await trend(1, { groupBy: 'day' })
    expect(httpMock.get).toHaveBeenCalledWith('/ledgers/1/statistics/trend', { params: { groupBy: 'day' } })
  })

  it('categoryStat 按类型统计', async () => {
    httpMock.get.mockResolvedValue([])
    await categoryStat(1, { type: 'expense' })
    expect(httpMock.get).toHaveBeenCalledWith('/ledgers/1/statistics/category', { params: { type: 'expense' } })
  })
})
