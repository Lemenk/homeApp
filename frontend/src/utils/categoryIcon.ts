/**
 * 账单分类图标映射工具
 * 分类的 icon 字段存储为 key（如 food/traffic/salary），通过本表映射为展示用 emoji
 */
export const CATEGORY_ICONS: Record<string, string> = {
  // 支出
  food: '🍚',
  traffic: '🚌',
  shopping: '🛍️',
  home: '🏠',
  fun: '🎮',
  medical: '🏥',
  edu: '📚',
  gift: '🎁',
  other: '📦',
  // 收入
  salary: '💰',
  bonus: '🧧',
  invest: '📈',
  parttime: '💼',
  redpacket: '🧧',
}

const FALLBACK = '📄'

/** 分类 key → emoji；未知 key 回退默认 */
export function categoryEmoji(key?: string): string {
  if (key && CATEGORY_ICONS[key]) return CATEGORY_ICONS[key]
  return FALLBACK
}

/** 判断 key 是否是已知分类图标 */
export function isKnownCategoryIcon(key?: string): boolean {
  return !!key && !!CATEGORY_ICONS[key]
}
