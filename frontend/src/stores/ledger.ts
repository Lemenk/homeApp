import { defineStore } from 'pinia'
import { createLedger, deleteLedger, listLedgers } from '@/api/ledger'
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
  },
  actions: {
    async fetch() {
      this.ledgers = await listLedgers()
      if (!this.ledgers.find((l) => l.id === this.currentLedgerId)) {
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
        this.currentLedgerId = this.ledgers[0]?.id || 0
        this.persist()
      }
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
