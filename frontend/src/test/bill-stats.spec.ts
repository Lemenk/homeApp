import { describe, it, expect } from 'vitest'
import type { BillVO } from '@/api/bill'
import { parseBillDateTime, groupBillsByDay, monthStats } from '@/utils/billStats'

function bill(partial: Partial<BillVO> & { id: number; billDate: string }): BillVO {
  const base: Partial<BillVO> = {
    ledgerId: 1,
    type: 'expense',
    categoryName: '未分类',
    memberId: 1,
    amount: 0,
    tags: [],
    accounts: [],
    createdAt: '',
    updatedAt: '',
  }
  return { ...base, ...partial } as BillVO
}

describe('parseBillDateTime', () => {
  it('解析空格分隔的账单时间', () => {
    expect(parseBillDateTime('2026-09-01 15:26')).toEqual({ date: '2026-09-01', time: '15:26' })
  })

  it('解析 T 分隔的 ISO 时间', () => {
    expect(parseBillDateTime('2026-09-01T08:05:00')).toEqual({ date: '2026-09-01', time: '08:05' })
  })

  it('仅有日期时时间为空', () => {
    expect(parseBillDateTime('2026-09-01')).toEqual({ date: '2026-09-01', time: '' })
  })

  it('空值安全返回', () => {
    expect(parseBillDateTime(undefined)).toEqual({ date: '', time: '' })
  })
})

describe('groupBillsByDay', () => {
  it('按日期倒序分组，组内按时间倒序，并计算每日收入/支出/结余', () => {
    const bills = [
      bill({ id: 1, billDate: '2026-09-01 15:26', type: 'expense', amount: 88.5 }),
      bill({ id: 2, billDate: '2026-09-01 09:00', type: 'income', amount: 100 }),
      bill({ id: 3, billDate: '2026-08-31 20:38', type: 'expense', amount: 13 }),
    ]
    const groups = groupBillsByDay(bills)
    // 日期倒序：09-01 在前，08-31 在后
    expect(groups.map((g) => g.date)).toEqual(['2026-09-01', '2026-08-31'])
    const g1 = groups[0]
    // 组内时间倒序：15:26 在 09:00 前
    expect(g1.list.map((b) => b.id)).toEqual([1, 2])
    expect(g1.income).toBe(100)
    expect(g1.expense).toBe(88.5)
    expect(g1.balance).toBeCloseTo(11.5)
    const g2 = groups[1]
    expect(g2.income).toBe(0)
    expect(g2.expense).toBe(13)
    expect(g2.balance).toBe(-13)
  })

  it('空数组返回空', () => {
    expect(groupBillsByDay([])).toEqual([])
  })

  it('无日期账单被跳过', () => {
    const groups = groupBillsByDay([bill({ id: 1, billDate: '' })])
    expect(groups).toEqual([])
  })
})

describe('monthStats', () => {
  it('统计收入、支出、结余', () => {
    const bills = [
      bill({ id: 1, billDate: '2026-09-01 10:00', type: 'income', amount: 200 }),
      bill({ id: 2, billDate: '2026-09-02 10:00', type: 'expense', amount: 50 }),
      bill({ id: 3, billDate: '2026-09-03 10:00', type: 'expense', amount: 30.5 }),
    ]
    expect(monthStats(bills)).toEqual({ income: 200, expense: 80.5, balance: 119.5 })
  })

  it('空数组结余为 0', () => {
    expect(monthStats([])).toEqual({ income: 0, expense: 0, balance: 0 })
  })
})
