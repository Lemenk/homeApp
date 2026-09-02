import { http } from './http'
import type { CategoryVO, TagVO } from './ledger'

export interface BillAccountItem {
  accountId: number
  direction: 'out' | 'in'
  amount: number
}

export interface BillVO {
  id: number
  ledgerId: number
  type: 'expense' | 'income' | 'transfer'
  categoryId?: number
  categoryName?: string
  categoryIcon?: string
  memberId: number
  memberName?: string
  amount: number
  billDate: string
  remark?: string
  tags: TagVO[]
  accounts: { accountId: number; accountName: string; direction: string; amount: number; pairId?: number }[]
  createdAt: string
  updatedAt: string
}

/** 账单操作留痕 */
export interface BillLogVO {
  id: number
  action: 'create' | 'update' | 'delete'
  operatorName: string
  summary: string
  accountIcons?: string[]
  createdAt: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export function listBills(
  ledgerId: number,
  params: Record<string, string | number | undefined>
): Promise<PageResult<BillVO>> {
  return http.get(`/ledgers/${ledgerId}/bills`, { params })
}

export function createBill(
  ledgerId: number,
  data: {
    type: 'expense' | 'income' | 'transfer'
    categoryId?: number
    memberId?: number
    amount: number
    billDate?: string
    remark?: string
    tagIds?: number[]
    items: BillAccountItem[]
  }
): Promise<BillVO> {
  return http.post(`/ledgers/${ledgerId}/bills`, data)
}

export function getBill(id: number): Promise<BillVO> {
  return http.get(`/bills/${id}`)
}

export function updateBill(id: number, data: any): Promise<BillVO> {
  return http.put(`/bills/${id}`, data)
}

export function deleteBill(id: number): Promise<void> {
  return http.delete(`/bills/${id}`)
}

export function billLogs(id: number): Promise<BillLogVO[]> {
  return http.get(`/bills/${id}/logs`)
}

export type { CategoryVO }
