import { defineStore } from 'pinia'
import { login as loginApi, getUserInfo, logout as logoutApi } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    username: '',
    role: ''
  }),

  getters: {
    isLoggedIn: state => !!state.token,
    isAdmin: state => state.role === 'ADMIN'
  },

  actions: {
    async login(username, password) {
      const res = await loginApi({ username, password })
      this.token = res.token
      this.username = res.username
      this.role = res.role
      setToken(res.token)
    },

    async fetchUserInfo() {
      const res = await getUserInfo()
      this.username = res.username
      this.role = res.role
    },

    async logout() {
      try {
        await logoutApi()
      } finally {
        this.token = ''
        this.username = ''
        this.role = ''
        removeToken()
      }
    }
  }
})
