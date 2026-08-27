import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const { ledgers, created } = vi.hoisted(() => {
  return {
    ledgers: [
      { id: 1, name: '家庭账本', type: 'public', ownerId: 1, familyId: 1, role: 'creator', memberCount: 2 },
      { id: 2, name: '我的私账', type: 'personal', ownerId: 1, role: 'creator', memberCount: 1 },
    ],
    created: { id: 3, name: '新账本', type: 'personal', ownerId: 1, role: 'creator', memberCount: 1 },
  }
})

vi.mock('@/api/ledger', () => ({
  listLedgers: vi.fn().mockResolvedValue(ledgers),
  createLedger: vi.fn().mockResolvedValue(created),
  deleteLedger: vi.fn().mockResolvedValue(undefined),
  getLedger: vi.fn(),
  listCategories: vi.fn().mockResolvedValue([]),
  createCategory: vi.fn(),
  toggleCategory: vi.fn(),
  deleteCategory: vi.fn(),
  listTags: vi.fn().mockResolvedValue([]),
  createTag: vi.fn(),
  deleteTag: vi.fn(),
}))

import { useLedgerStore } from '@/stores/ledger'

describe('Phase 3 账本 store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('获取账本列表并自动选中第一个', async () => {
    const store = useLedgerStore()
    await store.fetch()
    expect(store.ledgers.length).toBe(2)
    expect(store.currentLedger?.name).toBe('家庭账本')
  })

  it('新建账本后自动切换为当前账本', async () => {
    const store = useLedgerStore()
    await store.fetch()
    const ledger = await store.create({ name: '新账本', type: 'personal' })
    expect(ledger.id).toBe(3)
    expect(store.currentLedgerId).toBe(3)
    expect(localStorage.getItem('currentLedgerId')).toBe('3')
  })

  it('切换账本并持久化', async () => {
    const store = useLedgerStore()
    await store.fetch()
    store.switchTo(2)
    expect(store.currentLedger?.name).toBe('我的私账')
    expect(localStorage.getItem('currentLedgerId')).toBe('2')
  })
})
