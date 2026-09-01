import { describe, it, expect } from 'vitest'
import { ICON_OPTIONS, iconOption, iconEmoji, iconLabel, iconBg, iconSvg } from '@/utils/accountIcon'

describe('accountIcon 账户图标映射', () => {
  it('包含常见的账户图标选项', () => {
    const keys = ICON_OPTIONS.map((i) => i.key)
    expect(keys).toContain('cash')
    expect(keys).toContain('wechat')
    expect(keys).toContain('alipay')
    expect(keys).toContain('bankcard')
    expect(keys).toContain('creditcard')
    expect(keys).toContain('huabei')
  })

  it('已知 key 映射出对应 emoji / 名称 / 背景色', () => {
    expect(iconEmoji('wechat')).toBe('💬')
    expect(iconLabel('alipay')).toBe('支付宝')
    expect(iconBg('bankcard')).toBe('#FFF8E1')
    expect(iconEmoji('cash')).toBe('¥')
  })

  it('未知 key 回退到默认图标（不抛错）', () => {
    expect(iconEmoji('not-exist')).toBeDefined()
    expect(iconLabel('not-exist')).toBe('其他')
    expect(iconBg('not-exist')).toBeDefined()
  })

  it('空值/undefined 安全回退', () => {
    expect(iconEmoji(undefined)).toBeDefined()
    expect(iconLabel('')).toBeDefined()
    expect(iconOption(undefined)).toEqual(iconOption('other'))
  })

  it('每个选项的 key 唯一', () => {
    const keys = ICON_OPTIONS.map((i) => i.key)
    expect(new Set(keys).size).toBe(keys.length)
  })

  it('品牌账户返回真实 SVG 图标，普通账户返回 undefined', () => {
    expect(iconSvg('wechat')?.color).toBe('#07C160')
    expect(iconSvg('alipay')?.color).toBe('#1677FF')
    expect(iconSvg('wechat')?.path).toMatch(/^M/)
    expect(iconSvg('cash')).toBeUndefined()
    expect(iconSvg('not-exist')).toBeUndefined()
  })
})
