import { defineStore } from 'pinia'
import { fetchMe, loginByPhone } from '@/api/auth'
import type { UserVO } from '@/types'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null as UserVO | null,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
  },
  actions: {
    async login(phone: string, code: string) {
      const res = await loginByPhone(phone, code)
      this.token = res.token
      this.user = res.user
      localStorage.setItem('token', res.token)
    },
    async fetchMe() {
      if (!this.token) return
      this.user = await fetchMe()
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
    },
  },
})
