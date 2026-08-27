import { http } from './http'

export interface TrendPoint {
  period: string
  expense: number
  income: number
}

export interface CategoryStat {
  categoryId: number
  categoryName: string
  categoryIcon?: string
  amount: number
  percent: number
}

export function trend(
  ledgerId: number,
  params: { startDate?: string; endDate?: string; groupBy?: 'day' | 'month' }
): Promise<TrendPoint[]> {
  return http.get(`/ledgers/${ledgerId}/statistics/trend`, { params })
}

export function categoryStat(
  ledgerId: number,
  params: { type: 'expense' | 'income'; startDate?: string; endDate?: string }
): Promise<CategoryStat[]> {
  return http.get(`/ledgers/${ledgerId}/statistics/category`, { params })
}
