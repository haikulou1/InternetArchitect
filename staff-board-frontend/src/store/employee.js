import { defineStore } from 'pinia'
import { getEmployees } from '@/api/employee'

export const useEmployeeStore = defineStore('employee', {
  state: () => ({
    list: [],
    total: 0,
    page: 0,
    size: 10,
    filters: { department: '', status: '', whitelist: null },
    loading: false
  }),

  actions: {
    async fetchList() {
      this.loading = true
      try {
        const res = await getEmployees({
          ...this.filters,
          page: this.page,
          size: this.size
        })
        this.list = res.content
        this.total = res.totalElements
      } finally {
        this.loading = false
      }
    },

    setPage(page) {
      this.page = page
      this.fetchList()
    },

    setFilters(filters) {
      this.filters = { ...this.filters, ...filters }
      this.page = 0
      this.fetchList()
    }
  }
})
