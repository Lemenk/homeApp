import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppIcon from '@/components/AppIcon.vue'

describe('AppIcon 账户图标组件', () => {
  it('品牌账户（wechat）渲染内联 SVG 品牌 logo', () => {
    const w = mount(AppIcon, { props: { icon: 'wechat' } })
    expect(w.find('svg').exists()).toBe(true)
    expect(w.find('svg path').attributes('fill')).toBe('#07C160')
    expect(w.find('svg path').attributes('d')).toMatch(/^M/)
  })

  it('支付宝渲染品牌 SVG 且颜色正确', () => {
    const w = mount(AppIcon, { props: { icon: 'alipay' } })
    expect(w.find('svg').exists()).toBe(true)
    expect(w.find('svg path').attributes('fill')).toBe('#1677FF')
  })

  it('无 SVG 账户（cash）渲染 emoji 兜底', () => {
    const w = mount(AppIcon, { props: { icon: 'cash' } })
    expect(w.find('svg').exists()).toBe(false)
    expect(w.find('span').text()).toBe('¥')
  })

  it('未知 key 回退 other 图标', () => {
    const w = mount(AppIcon, { props: { icon: 'not-exist' } })
    expect(w.find('svg').exists()).toBe(false)
    expect(w.find('span').text()).toBe('⭐')
  })

  it('空值安全回退（不抛错）', () => {
    const w = mount(AppIcon, { props: { icon: undefined } })
    expect(w.find('span').exists()).toBe(true)
  })

  it('size prop 控制 SVG 尺寸', () => {
    const w = mount(AppIcon, { props: { icon: 'alipay', size: 28 } })
    expect(w.find('svg').attributes('width')).toBe('28')
    expect(w.find('svg').attributes('height')).toBe('28')
  })
})
