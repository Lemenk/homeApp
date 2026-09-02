import { http } from './http'

export type AccountType = 'asset' | 'credit' | 'stored_value'

export interface AccountVO {
  id: number
  ledgerId: number
  type: AccountType
  name: string
  icon?: string
  balance: number
  groupName?: string
  remark?: string
  includeInTotal?: number
}

/** 账户类型 → 展示名 */
export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  asset: '资金账户',
  credit: '信贷账户',
  stored_value: '储值账户',
}

export const ACCOUNT_TYPE_ORDER: AccountType[] = ['asset', 'credit', 'stored_value']

/** 各类型推荐账户名称（可快捷选择，也可自定义） */
export const ACCOUNT_SUGGESTIONS: Record<AccountType, string[]> = {
  asset: ['现金', '微信', '支付宝', '银行卡', '京东钱包', '抖音钱包', '零钱通', '余额宝'],
  credit: ['信用卡', '花呗', '京东白条', '美团月付', '抖音月付', '房贷', '车贷', '装修贷'],
  stored_value: ['会员卡', '公交卡', '加油卡', '购物卡', '就餐卡', '话费卡'],
}

export function listAccounts(ledgerId: number): Promise<AccountVO[]> {
  return http.get(`/ledgers/${ledgerId}/accounts`)
}

export function createAccount(
  ledgerId: number,
  data: {
    name: string
    type: AccountType
    balance?: number
    icon?: string
    groupName?: string
    remark?: string
    includeInTotal?: number
  }
): Promise<AccountVO> {
  return http.post(`/ledgers/${ledgerId}/accounts`, data)
}

export function summary(ledgerId: number): Promise<{
  totalAssets: number
  totalLiability: number
  netAssets: number
  accounts: AccountVO[]
}> {
  return http.get(`/ledgers/${ledgerId}/accounts/summary`)
}

export function adjustBalance(
  accountId: number,
  data: { newBalance: number; reason?: string }
): Promise<AccountVO> {
  return http.post(`/accounts/${accountId}/balance`, data)
}

export function updateAccount(
  accountId: number,
  data: {
    name: string
    type?: AccountType
    groupName?: string
    remark?: string
    includeInTotal?: number
    balance?: number
  }
): Promise<AccountVO> {
  return http.put(`/accounts/${accountId}`, data)
}
