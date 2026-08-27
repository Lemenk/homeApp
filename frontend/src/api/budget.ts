import { http } from './http'

export interface BudgetVO {
  id: number
  ledgerId: number
  categoryId: number
  categoryName: string
  categoryIcon?: string
  periodType: 'monthly' | 'custom'
  startDate?: string
  endDate?: string
  amount: number
  remark?: string
  usage: number
  percent: number
  overBudget: boolean
}

export function listBudgets(ledgerId: number): Promise<BudgetVO[]> {
  return http.get(`/ledgers/${ledgerId}/budgets`)
}

export function createBudget(
  ledgerId: number,
  data: {
    categoryId: number
    periodType: 'monthly' | 'custom'
    startDate?: string
    endDate?: string
    amount: number
    remark?: string
  }
): Promise<BudgetVO> {
  return http.post(`/ledgers/${ledgerId}/budgets`, data)
}

export function updateBudget(id: number, data: any): Promise<BudgetVO> {
  return http.put(`/budgets/${id}`, data)
}

export function deleteBudget(id: number): Promise<void> {
  return http.delete(`/budgets/${id}`)
}
