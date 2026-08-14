<template>
  <el-card shadow="hover" class="employee-card" @click="$router.push(`/employees/${employee.id}`)">
    <div class="card-body">
      <el-avatar :size="48" :src="employee.avatar">
        {{ employee.name?.charAt(0) }}
      </el-avatar>
      <div class="card-info">
        <div class="card-name">{{ employee.name }}</div>
        <div class="card-dept">{{ employee.department }} · {{ employee.position }}</div>
      </div>
      <el-tag v-if="employee.isWhitelist" type="warning" size="small" class="card-badge">
        白名单
      </el-tag>
    </div>
    <div class="card-footer">
      <el-tag :type="statusType" size="small">{{ statusLabel }}</el-tag>
      <span class="card-no">{{ employee.employeeNo }}</span>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ employee: Object })

const statusLabel = computed(() =>
  ({ REGULAR: '正式', PROBATION: '试用期', RESIGNED: '离职' }[props.employee.status] || props.employee.status))

const statusType = computed(() =>
  ({ REGULAR: 'success', PROBATION: 'warning', RESIGNED: 'info' }[props.employee.status] || ''))
</script>

<style scoped lang="scss">
.employee-card {
  cursor: pointer;
  transition: transform 0.2s;
  &:hover { transform: translateY(-2px); }
}
.card-body {
  display: flex;
  align-items: center;
  gap: 12px;
}
.card-info { flex: 1; }
.card-name { font-weight: bold; font-size: 16px; }
.card-dept { color: #909399; font-size: 13px; margin-top: 4px; }
.card-badge { position: absolute; top: 12px; right: 12px; }
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.card-no { color: #909399; font-size: 12px; }
</style>
