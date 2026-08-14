<template>
  <el-card v-if="result" style="margin-top: 16px;">
    <template #header>导入结果</template>
    <el-result :icon="result.failCount === 0 ? 'success' : 'warning'"
      :title="`共 ${result.totalCount} 条`"
      :sub-title="`成功 ${result.successCount} 条，失败 ${result.failCount} 条`">
    </el-result>
    <div v-if="result.errors?.length" style="margin-top: 12px;">
      <h4>错误明细：</h4>
      <el-alert v-for="(err, i) in result.errors" :key="i" :title="err" type="error"
        :closable="false" style="margin-bottom: 8px;" />
    </div>
  </el-card>
</template>

<script setup>
defineProps({ result: Object })
</script>
