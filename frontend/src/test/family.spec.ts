import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/family', () => ({
  createFamily: vi.fn().mockResolvedValue({
    id: 1,
    name: '我们一家',
    creatorId: 1,
    inviteCode: 'ABCD1234',
    role: 'creator',
    members: [],
  }),
  joinFamily: vi.fn().mockResolvedValue({
    id: 1,
    name: '我们一家',
    creatorId: 2,
    inviteCode: 'ABCD1234',
    role: 'member',
    members: [],
  }),
  myFamily: vi.fn().mockResolvedValue(null),
  refreshInvite: vi.fn().mockResolvedValue('NEWCODE1'),
}))

import { useFamilyStore } from '@/stores/family'

describe('Phase 2 家庭 store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('创建家庭后写入状态', async () => {
    const store = useFamilyStore()
    await store.create('我们一家')
    expect(store.hasFamily).toBe(true)
    expect(store.family?.name).toBe('我们一家')
    expect(store.family?.role).toBe('creator')
  })

  it('加入家庭后角色为 member', async () => {
    const store = useFamilyStore()
    await store.join('ABCD1234')
    expect(store.family?.role).toBe('member')
  })
})
