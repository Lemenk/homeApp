import { http } from './http'
import type { LedgerVO } from '@/types'

export interface CategoryVO {
  id: number
  ledgerId: number
  type: 'expense' | 'income'
  name: string
  icon?: string
  sort: number
  enabled: number
}

export interface TagVO {
  id: number
  ledgerId: number
  name: string
  color?: string
}

export function listLedgers(): Promise<LedgerVO[]> {
  return http.get('/ledgers')
}

export function createLedger(data: {
  name: string
  type: 'public' | 'personal'
  icon?: string
}): Promise<LedgerVO> {
  return http.post('/ledgers', data)
}

export function getLedger(id: number): Promise<LedgerVO> {
  return http.get(`/ledgers/${id}`)
}

export function deleteLedger(id: number): Promise<void> {
  return http.delete(`/ledgers/${id}`)
}

export function listCategories(ledgerId: number): Promise<CategoryVO[]> {
  return http.get(`/ledgers/${ledgerId}/categories`)
}

export function createCategory(
  ledgerId: number,
  data: { name: string; type: 'expense' | 'income'; icon?: string }
): Promise<CategoryVO> {
  return http.post(`/ledgers/${ledgerId}/categories`, data)
}

export function toggleCategory(id: number, enabled: boolean): Promise<CategoryVO> {
  return http.put(`/categories/${id}/toggle?enabled=${enabled}`)
}

export function deleteCategory(id: number): Promise<void> {
  return http.delete(`/categories/${id}`)
}

export function listTags(ledgerId: number): Promise<TagVO[]> {
  return http.get(`/ledgers/${ledgerId}/tags`)
}

export function createTag(ledgerId: number, data: { name: string; color?: string }): Promise<TagVO> {
  return http.post(`/ledgers/${ledgerId}/tags`, data)
}

export function deleteTag(id: number): Promise<void> {
  return http.delete(`/tags/${id}`)
}
