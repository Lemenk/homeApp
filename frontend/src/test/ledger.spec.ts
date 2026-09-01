import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const { ledgers, created } = vi.hoisted(() => {
  return {
    ledgers: [
      { id: 1, name: '家庭账本', type: 'public', ownerId: 1, familyId: 1, role: 'creator', memberCount: 2, isDefault: 0 },
      { id: 2, name: '个人账本', type: 'personal', ownerId: 1, role: 'creator', memberCount: 1, isDefault: 1 },
    ],
    created: { id: 3, name: '新账本', type: 'personal', ownerId: 1, role: 'creator', memberCount: 1, isDefault: 0 },
  }
})

vi.mock('@/api/ledger', () => ({
  listLedgers: vi.fn().mockResolvedValue(ledgers),
  createLedger: vi.fn().mockResolvedValue(created),
  deleteLedger: vi.fn().mockResolvedValue(undefined),
  setDefaultLedger: vi.fn().mockResolvedValue({ id: 1, name: '家庭账本', type: 'public', ownerId: 1, familyId: 1, role: 'creator', memberCount: 2, isDefault: 1 }),
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

  it('获取账本列表，已选账本仍存在时保持选中', async () => {
    const store = useLedgerStore()
    store.switchTo(1)
    await store.fetch()
    expect(store.ledgers.length).toBe(2)
    expect(store.currentLedger?.name).toBe('家庭账本')
  })

  it('获取账本列表时默认选中默认账本', async () => {
    const store = useLedgerStore()
    // 清空当前账本，模拟首次进入
    localStorage.clear()
    store.currentLedgerId = 0
    await store.fetch()
    expect(store.defaultLedger?.id).toBe(2)
    // 默认展示默认账本的数据
    expect(store.currentLedgerId).toBe(2)
    expect(store.currentLedger?.name).toBe('个人账本')
    expect(store.currentLedger?.isDefault).toBe(1)
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
    expect(store.currentLedger?.name).toBe('个人账本')
    expect(localStorage.getItem('currentLedgerId')).toBe('2')
  })

  it('设置默认账本后仅目标账本为默认', async () => {
    const store = useLedgerStore()
    await store.fetch()
    await store.setDefault(1)
    expect(store.ledgers.find((l) => l.id === 1)?.isDefault).toBe(1)
    expect(store.ledgers.find((l) => l.id === 2)?.isDefault).toBe(0)
    expect(store.currentLedgerId).toBe(1)
    expect(localStorage.getItem('currentLedgerId')).toBe('1')
  })
})
