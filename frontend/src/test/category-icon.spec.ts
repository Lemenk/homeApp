import { describe, it, expect } from 'vitest'
import { CATEGORY_ICONS, categoryEmoji, isKnownCategoryIcon } from '@/utils/categoryIcon'

describe('categoryIcon 账单分类图标映射', () => {
  it('覆盖支出与收入分类 key', () => {
    expect(CATEGORY_ICONS.food).toBeDefined()
    expect(CATEGORY_ICONS.traffic).toBeDefined()
    expect(CATEGORY_ICONS.shopping).toBeDefined()
    expect(CATEGORY_ICONS.home).toBeDefined()
    expect(CATEGORY_ICONS.medical).toBeDefined()
    expect(CATEGORY_ICONS.salary).toBeDefined()
    expect(CATEGORY_ICONS.bonus).toBeDefined()
  })

  it('已知 key 返回对应 emoji', () => {
    expect(categoryEmoji('food')).toBe('🍚')
    expect(categoryEmoji('traffic')).toBe('🚌')
    expect(categoryEmoji('salary')).toBe('💰')
    expect(isKnownCategoryIcon('food')).toBe(true)
  })

  it('未知/空 key 回退默认，且不被识别', () => {
    expect(categoryEmoji('not-exist')).toBe('📄')
    expect(categoryEmoji(undefined)).toBe('📄')
    expect(categoryEmoji('')).toBe('📄')
    expect(isKnownCategoryIcon('not-exist')).toBe(false)
    expect(isKnownCategoryIcon(undefined)).toBe(false)
  })
})
