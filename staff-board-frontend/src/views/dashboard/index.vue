<template>
  <div class="page-container" v-loading="loading">
    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="总员工数" :value="summary.totalEmployees">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="白名单人数" :value="summary.whitelistCount">
            <template #prefix><el-icon><Star /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="总人力成本" :value="summary.totalCost" prefix="¥" :precision="2" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="部门数量" :value="deptCount" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>部门成本概览</template>
          <BudgetChart type="bar" :data="barData" :height="300" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>人员分布</template>
          <BudgetChart type="pie" :data="pieData" :height="300" />
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
const deptCount = computed(() => summary.value.departmentCosts?.length || 0)

const barData = computed(() => ({
  title: '',
  categories: summary.value.departmentCosts?.map(d => d.department) || [],
  actual: summary.value.departmentCosts?.map(d => d.actualCost) || [],
  budget: summary.value.departmentCosts?.map(d => d.budgetAmount) || []
}))

const pieData = computed(() => ({
  title: '',
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
