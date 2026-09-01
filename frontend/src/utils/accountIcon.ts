/**
 * 账户图标映射工具
 * 账户的 icon 字段存储为 key（如 cash/wechat/bankcard），通过本表映射为 emoji / 名称 / 背景色
 * 有品牌 SVG 的账户（微信/支付宝/QQ）优先渲染真实 logo，其余用 emoji 兜底
 */
import { siWechat, siAlipay, siQq } from 'simple-icons'

export interface BrandSvg {
  path: string
  color: string
}

export interface IconOption {
  key: string
  label: string
  emoji: string
  bg: string
  svg?: BrandSvg
}

function brand(icon: { path: string; hex: string }): BrandSvg {
  return { path: icon.path, color: `#${icon.hex}` }
}

export const ICON_OPTIONS: IconOption[] = [
  { key: 'cash', label: '现金', emoji: '¥', bg: '#FFF3E0' },
  { key: 'wechat', label: '微信', emoji: '💬', bg: '#E8F5E9', svg: brand(siWechat) },
  { key: 'alipay', label: '支付宝', emoji: '支', bg: '#E3F2FD', svg: brand(siAlipay) },
  { key: 'qq', label: 'QQ钱包', emoji: '🐧', bg: '#FFEBEE', svg: brand(siQq) },
  { key: 'bankcard', label: '银行卡', emoji: '💳', bg: '#FFF8E1' },
  { key: 'creditcard', label: '信用卡', emoji: '💳', bg: '#ECEFF1' },
  { key: 'member', label: '会员卡', emoji: '🎫', bg: '#FFF3E0' },
  { key: 'meal', label: '饭卡', emoji: '🍱', bg: '#FFF3E0' },
  { key: 'bus', label: '公交卡', emoji: '🚌', bg: '#E8F5E9' },
  { key: 'huabei', label: '花呗', emoji: '🌸', bg: '#E3F2FD' },
  { key: 'baitiao', label: '白条', emoji: '📋', bg: '#FFEBEE' },
  { key: 'jd', label: '京东金融', emoji: '🐕', bg: '#FFEBEE' },
  { key: 'stock', label: '股票', emoji: '📈', bg: '#FFEBEE' },
  { key: 'fund', label: '基金', emoji: '📊', bg: '#E8F5E9' },
  { key: 'finance', label: '理财', emoji: '💰', bg: '#FFF8E1' },
  { key: 'deposit', label: '存款', emoji: '🏦', bg: '#E3F2FD' },
  { key: 'fixed', label: '固定资产', emoji: '🔒', bg: '#ECEFF1' },
  { key: 'house', label: '房子', emoji: '🏠', bg: '#FFEBEE' },
  { key: 'car', label: '车', emoji: '🚗', bg: '#E3F2FD' },
  { key: 'insurance', label: '保险', emoji: '🛡️', bg: '#E3F2FD' },
  { key: 'reimburse', label: '报销', emoji: '🧾', bg: '#E3F2FD' },
  { key: 'deposit_fee', label: '押金', emoji: '🔑', bg: '#E3F2FD' },
  { key: 'crypto', label: '虚拟货币', emoji: '🪙', bg: '#F3E5F5' },
  { key: 'ledger', label: '账本', emoji: '📒', bg: '#ECEFF1' },
  { key: 'other', label: '其他', emoji: '⭐', bg: '#E8F5E9' },
  { key: 'lend', label: '借出', emoji: '💸', bg: '#FFF8E1' },
  { key: 'borrow', label: '借入', emoji: '💵', bg: '#FFF8E1' },
]

const FALLBACK: IconOption = { key: 'other', label: '其他', emoji: '⭐', bg: '#E8F5E9' }

export function iconOption(key?: string): IconOption {
  return ICON_OPTIONS.find((i) => i.key === key) || FALLBACK
}
export function iconEmoji(key?: string): string {
  return iconOption(key).emoji
}
export function iconLabel(key?: string): string {
  return iconOption(key).label
}
export function iconBg(key?: string): string {
  return iconOption(key).bg
}
export function iconSvg(key?: string): BrandSvg | undefined {
  return iconOption(key).svg
}
