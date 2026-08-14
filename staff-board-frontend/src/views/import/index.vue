<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>批量导入</span>
          <el-button type="primary" text @click="handleDownloadTemplate">
            <el-icon><Download /></el-icon> 下载导入模板
          </el-button>
        </div>
      </template>

      <el-upload ref="uploadRef" drag :auto-upload="false" :limit="1"
        accept=".xlsx,.xls,.csv" :on-change="handleFileChange" :on-exceed="handleExceed">
        <el-icon style="font-size: 48px; color: #909399;"><Upload /></el-icon>
        <div>将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div style="color: #909399; font-size: 12px;">
            支持 .xlsx / .xls / .csv 格式，单文件不超过 10MB
          </div>
        </template>
      </el-upload>

      <el-button type="primary" style="margin-top: 16px;" :loading="importing"
        :disabled="!selectedFile" @click="handleImport">
        开始导入
      </el-button>

      <ImportProgress :result="importResult" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { importEmployees, downloadTemplate } from '@/api/employee'
import ImportProgress from '@/components/ImportProgress.vue'

const uploadRef = ref(null)
const selectedFile = ref(null)
const importing = ref(false)
const importResult = ref(null)

function handleFileChange(file) {
  selectedFile.value = file.raw
}

function handleExceed() {
  ElMessage.warning('只能上传一个文件，请先移除已有文件')
}

async function handleImport() {
  if (!selectedFile.value) return
  importing.value = true
  importResult.value = null
  try {
    importResult.value = await importEmployees(selectedFile.value)
    if (importResult.value.failCount === 0) {
      ElMessage.success('导入完成')
    } else {
      ElMessage.warning(`导入完成，${importResult.value.failCount} 条失败`)
    }
  } finally {
    importing.value = false
  }
}

async function handleDownloadTemplate() {
  try {
    const blob = await downloadTemplate()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'employee_template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载模板失败')
  }
}
</script>
