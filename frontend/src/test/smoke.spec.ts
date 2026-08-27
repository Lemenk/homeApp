import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/bill', () => ({
  listBills: vi.fn().mockResolvedValue({ list: [], total: 0, page: 1, size: 20 }),
}))
vi.mock('@/api/ledger', () => ({ listLedgers: vi.fn().mockResolvedValue([]) }))
vi.mock('@/api/auth', () => ({
  fetchMe: vi.fn().mockResolvedValue({ id: 1, phone: '13800000000', nickname: '测试用户' }),
}))

import HomeView from '@/views/HomeView.vue'

describe('Phase 0 前端冒烟测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('首页概览组件可正常渲染并含记账入口', async () => {
    const wrapper = mount(HomeView)
    await new Promise((r) => setTimeout(r, 50))
    expect(wrapper.text()).toContain('记一笔')
  })
})
