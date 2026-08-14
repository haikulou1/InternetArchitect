<template>
  <div class="page-container" v-loading="loading">
    <el-page-header @back="$router.back()" title="返回" />
    <el-card style="margin-top: 16px;" v-if="employee">
      <template #header>
        <div class="card-header">
          <span>{{ employee.name }} - {{ employee.employeeNo }}</span>
          <el-tag :type="employee.isWhitelist ? 'warning' : 'info'">
            {{ employee.isWhitelist ? '白名单: ' + (employee.whitelistType || '') : '非白名单' }}
          </el-tag>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="姓名">{{ employee.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ employee.gender === 1 ? '男' : '女' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ employee.department }}</el-descriptions-item>
        <el-descriptions-item label="职位">{{ employee.position }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ employee.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ employee.email }}</el-descriptions-item>
        <el-descriptions-item label="入职日期">{{ employee.hireDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ employee.status }}</el-descriptions-item>
        <el-descriptions-item label="学历">{{ employee.education }}</el-descriptions-item>
        <el-descriptions-item label="基本工资">¥{{ employee.baseSalary }}</el-descriptions-item>
        <el-descriptions-item label="社保">¥{{ employee.socialSecurity }}</el-descriptions-item>
        <el-descriptions-item label="公积金">¥{{ employee.housingFund }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ employee.remarks || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getEmployee } from '@/api/employee'

const route = useRoute()
const employee = ref(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    employee.value = await getEmployee(route.params.id)
  } finally {
    loading.value = false
  }
})
</script>
