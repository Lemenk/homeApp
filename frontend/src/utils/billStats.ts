/**
 * 账单列表分组与统计纯函数
 * billDate 支持 "YYYY-MM-DD HH:mm" / "YYYY-MM-DDTHH:mm" / "YYYY-MM-DD" 等格式
 */
import type { BillVO } from '@/api/bill'

export interface DayGroup {
  /** 日期 YYYY-MM-DD */
  date: string
  /** 当日账单，按时间倒序（新在前） */
  list: BillVO[]
  income: number
  expense: number
  balance: number
}

export interface MonthStats {
  income: number
  expense: number
  balance: number
}

/** 解析账单时间，返回 { date: YYYY-MM-DD, time: HH:mm } */
export function parseBillDateTime(billDate?: string): { date: string; time: string } {
  if (!billDate) return { date: '', time: '' }
  const s = billDate.trim()
  // 兼容 T 或空格分隔
  const [d, t] = s.split(/[T ]/)
  const date = d || ''
  // 取 HH:mm
  const time = t ? t.slice(0, 5) : ''
  return { date, time }
}

/** 按日期倒序分组，每日收入/支出/结余，组内按时间倒序 */
export function groupBillsByDay(bills: BillVO[]): DayGroup[] {
  const map = new Map<string, BillVO[]>()
  for (const b of bills) {
    const { date } = parseBillDateTime(b.billDate)
    if (!date) continue
    if (!map.has(date)) map.set(date, [])
    map.get(date)!.push(b)
  }
  const groups: DayGroup[] = []
  for (const [date, list] of map.entries()) {
    const sorted = [...list].sort((a, b) => (b.billDate || '').localeCompare(a.billDate || ''))
    const income = sorted
      .filter((b) => b.type === 'income')
      .reduce((s, b) => s + (b.amount || 0), 0)
    const expense = sorted
      .filter((b) => b.type === 'expense')
      .reduce((s, b) => s + (b.amount || 0), 0)
    groups.push({ date, list: sorted, income, expense, balance: income - expense })
  }
  // 日期倒序（新在前）
  groups.sort((a, b) => b.date.localeCompare(a.date))
  return groups
}

/** 指定月份内的收入/支出/结余统计 */
export function monthStats(bills: BillVO[]): MonthStats {
  const income = bills
    .filter((b) => b.type === 'income')
    .reduce((s, b) => s + (b.amount || 0), 0)
  const expense = bills
    .filter((b) => b.type === 'expense')
    .reduce((s, b) => s + (b.amount || 0), 0)
  return { income, expense, balance: income - expense }
}
