import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/auth', () => ({
  loginByPhone: vi.fn().mockResolvedValue({
    token: 'mock-token',
    user: { id: 1, phone: '13800000001', nickname: '用户0001' },
  }),
  fetchMe: vi.fn().mockResolvedValue({ id: 1, phone: '13800000001', nickname: '用户0001' }),
}))

import { useUserStore } from '@/stores/user'

describe('Phase 1 用户登录 store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('登录成功后写入 token 并持久化', async () => {
    const store = useUserStore()
    expect(store.isLoggedIn).toBe(false)
    await store.login('13800000001', '123456')
    expect(store.token).toBe('mock-token')
    expect(store.user?.phone).toBe('13800000001')
    expect(localStorage.getItem('token')).toBe('mock-token')
    expect(store.isLoggedIn).toBe(true)
  })

  it('退出登录清空 token', async () => {
    const store = useUserStore()
    await store.login('13800000001', '123456')
    store.logout()
    expect(store.token).toBe('')
    expect(store.user).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })
})
