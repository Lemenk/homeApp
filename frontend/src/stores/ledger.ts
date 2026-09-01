import { defineStore } from 'pinia'
import { createLedger, deleteLedger, listLedgers, setDefaultLedger } from '@/api/ledger'
import type { LedgerVO } from '@/types'

export const useLedgerStore = defineStore('ledger', {
  state: () => ({
    ledgers: [] as LedgerVO[],
    currentLedgerId: Number(localStorage.getItem('currentLedgerId')) || 0,
  }),
  getters: {
    currentLedger(state): LedgerVO | null {
      return state.ledgers.find((l) => l.id === state.currentLedgerId) || state.ledgers[0] || null
    },
    defaultLedger(state): LedgerVO | null {
      return state.ledgers.find((l) => l.isDefault === 1) || null
    },
  },
  actions: {
    async fetch() {
      this.ledgers = await listLedgers()
      // 优先选中默认账本；没有默认账本或当前账本已失效时回退到第一个
      const preferred = this.defaultLedger
      if (this.ledgers.find((l) => l.id === this.currentLedgerId)) {
        // 当前账本仍存在，保持选中
      } else if (preferred) {
        this.currentLedgerId = preferred.id
        this.persist()
      } else {
        this.currentLedgerId = this.ledgers[0]?.id || 0
        this.persist()
      }
    },
    async create(data: { name: string; type: 'public' | 'personal'; icon?: string }) {
      const ledger = await createLedger(data)
      this.ledgers.push(ledger)
      this.currentLedgerId = ledger.id
      this.persist()
      return ledger
    },
    async remove(id: number) {
      await deleteLedger(id)
      this.ledgers = this.ledgers.filter((l) => l.id !== id)
      if (this.currentLedgerId === id) {
        const preferred = this.defaultLedger
        this.currentLedgerId = preferred?.id || this.ledgers[0]?.id || 0
        this.persist()
      }
    },
    /** 切换默认账本 */
    async setDefault(id: number) {
      const updated = await setDefaultLedger(id)
      this.ledgers = this.ledgers.map((l) =>
        l.id === id ? updated : { ...l, isDefault: 0 }
      )
      this.currentLedgerId = id
      this.persist()
    },
    switchTo(id: number) {
      this.currentLedgerId = id
      this.persist()
    },
    persist() {
      localStorage.setItem('currentLedgerId', String(this.currentLedgerId))
    },
  },
})
