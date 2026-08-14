<template>
  <div class="page-container">
    <!-- 筛选栏 -->
    <el-card style="margin-bottom: 16px;">
      <el-form :inline="true" :model="filters">
        <el-form-item label="部门">
          <el-input v-model="filters.department" placeholder="部门" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部">
            <el-option label="正式" value="REGULAR" />
            <el-option label="试用期" value="PROBATION" />
            <el-option label="离职" value="RESIGNED" />
          </el-select>
        </el-form-item>
        <el-form-item label="白名单">
          <el-select v-model="filters.whitelist" clearable placeholder="全部">
            <el-option label="是" :value="1" />
            <el-option label="否" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 表格 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>员工列表</span>
          <div>
            <el-button type="primary" @click="showForm()">新增员工</el-button>
            <el-button @click="$router.push('/employees/card')">切换卡片视图</el-button>
          </div>
        </div>
      </template>

      <el-table :data="employeeStore.list" v-loading="employeeStore.loading" stripe>
        <el-table-column prop="employeeNo" label="工号" width="100" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="position" label="职位" width="140" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isWhitelist" label="白名单" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isWhitelist" type="warning">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="$router.push(`/employees/${row.id}`)">详情</el-button>
            <el-button type="warning" text size="small" @click="showForm(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" text size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top: 16px; justify-content: flex-end;"
        v-model:current-page="currentPage"
        :page-size="employeeStore.size"
        :total="employeeStore.total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange" />
    </el-card>

    <EmployeeForm v-model="formVisible" :employee="editingEmployee" @saved="refresh" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useEmployeeStore } from '@/store/employee'
import { deleteEmployee } from '@/api/employee'
import EmployeeForm from '@/components/EmployeeForm.vue'

const employeeStore = useEmployeeStore()
const formVisible = ref(false)
const editingEmployee = ref(null)

const filters = reactive({ department: '', status: '', whitelist: null })
const currentPage = computed({
  get: () => employeeStore.page + 1,
  set: val => employeeStore.setPage(val - 1)
})

onMounted(() => employeeStore.fetchList())

function handleSearch() {
  employeeStore.setFilters({ ...filters })
}

function handleReset() {
  Object.assign(filters, { department: '', status: '', whitelist: null })
  employeeStore.setFilters({ department: '', status: '', whitelist: null })
}

function handlePageChange(page) {
  employeeStore.setPage(page - 1)
}

function showForm(employee = null) {
  editingEmployee.value = employee
  formVisible.value = true
}

async function handleDelete(id) {
  await deleteEmployee(id)
  ElMessage.success('删除成功')
  refresh()
}

function refresh() {
  employeeStore.fetchList()
}

function statusLabel(status) {
  return { REGULAR: '正式', PROBATION: '试用期', RESIGNED: '离职' }[status] || status
}

function statusTagType(status) {
  return { REGULAR: 'success', PROBATION: 'warning', RESIGNED: 'info' }[status] || ''
}
</script>
