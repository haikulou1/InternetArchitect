<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>卡片视图</span>
          <el-button @click="$router.push('/employees')">切换列表视图</el-button>
        </div>
      </template>
      <el-row :gutter="16" v-loading="loading">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="emp in employees" :key="emp.id"
          style="margin-bottom: 16px;">
          <EmployeeCard :employee="emp" />
        </el-col>
      </el-row>
      <el-pagination style="margin-top: 16px; justify-content: center;"
        v-model:current-page="currentPage" :page-size="pageSize" :total="total"
        layout="total, prev, pager, next" @current-change="fetchData" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getEmployees } from '@/api/employee'
import EmployeeCard from '@/components/EmployeeCard.vue'

const employees = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)

async function fetchData() {
  loading.value = true
  try {
    const res = await getEmployees({ page: currentPage.value - 1, size: pageSize })
    employees.value = res.content
    total.value = res.totalElements
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
