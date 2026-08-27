import { defineStore } from 'pinia'
import { createFamily, joinFamily, myFamily, refreshInvite } from '@/api/family'
import type { FamilyVO } from '@/types/family'

export const useFamilyStore = defineStore('family', {
  state: () => ({
    family: null as FamilyVO | null,
  }),
  getters: {
    hasFamily: (s) => !!s.family,
  },
  actions: {
    async fetch() {
      this.family = await myFamily()
    },
    async create(name: string) {
      this.family = await createFamily(name)
    },
    async join(code: string) {
      this.family = await joinFamily(code)
    },
    async refresh() {
      if (this.family) {
        this.family.inviteCode = await refreshInvite(this.family.id)
      }
    },
    async fetchMembers() {
      if (!this.family) {
        await this.fetch()
      }
      return this.family?.members || []
    },
  },
})
