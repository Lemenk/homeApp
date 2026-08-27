import { http } from './http'

export interface AccountVO {
  id: number
  ledgerId: number
  type: 'common' | 'liability' | 'stored_value' | 'investment'
  name: string
  icon?: string
  initialBalance: number
  balance: number
}

export function listAccounts(ledgerId: number): Promise<AccountVO[]> {
  return http.get(`/ledgers/${ledgerId}/accounts`)
}

export function createAccount(
  ledgerId: number,
  data: { name: string; type: string; initialBalance?: number }
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
