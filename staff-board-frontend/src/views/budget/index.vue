<template>
  <div class="page-container" v-loading="loading">
    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="总员工数" :value="summary.totalEmployees" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="白名单人数" :value="summary.whitelistCount" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="总人力成本" :value="summary.totalCost" prefix="¥" :precision="2" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card>
          <template #header>部门成本对比</template>
          <BudgetChart type="bar" :data="barData" :height="350" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>部门成本分布</template>
          <BudgetChart type="pie" :data="pieData" :height="350" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getBudgetSummary } from '@/api/budget'
import BudgetChart from '@/components/BudgetChart.vue'

const loading = ref(false)
const summary = ref({ totalEmployees: 0, whitelistCount: 0, totalCost: 0, departmentCosts: [] })

const barData = computed(() => ({
  title: '部门成本对比',
  categories: summary.value.departmentCosts?.map(d => d.department) || [],
  actual: summary.value.departmentCosts?.map(d => d.actualCost) || [],
  budget: summary.value.departmentCosts?.map(d => d.budgetAmount) || []
}))

const pieData = computed(() => ({
  title: '部门成本分布',
  items: summary.value.departmentCosts?.map(d => ({
    name: d.department, value: d.actualCost
  })) || []
}))

onMounted(async () => {
  loading.value = true
  try {
    summary.value = await getBudgetSummary()
  } finally {
    loading.value = false
  }
})
</script>
