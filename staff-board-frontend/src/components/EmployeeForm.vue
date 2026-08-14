<template>
  <el-dialog :title="isEdit ? '编辑员工' : '新增员工'" v-model="visible" width="700px"
    :close-on-click-modal="false" @close="handleClose">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="工号" prop="employeeNo">
            <el-input v-model="form.employeeNo" :disabled="isEdit" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="部门">
            <el-input v-model="form.department" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="职位">
            <el-input v-model="form.position" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号">
            <el-input v-model="form.phone" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邮箱">
            <el-input v-model="form.email" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="入职日期">
            <el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD"
              style="width: 100%;" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%;">
              <el-option label="正式" value="REGULAR" />
              <el-option label="试用期" value="PROBATION" />
              <el-option label="离职" value="RESIGNED" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="基本工资">
            <el-input-number v-model="form.baseSalary" :min="0" :precision="2"
              style="width: 100%;" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="白名单">
            <el-switch v-model="form.isWhitelist" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-col>
        <el-col :span="12" v-if="form.isWhitelist === 1">
          <el-form-item label="白名单类型">
            <el-select v-model="form.whitelistType" style="width: 100%;">
              <el-option label="核心人才" value="CORE_TALENT" />
              <el-option label="高管" value="EXECUTIVE" />
              <el-option label="特殊岗位" value="SPECIAL" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="备注">
        <el-input v-model="form.remarks" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createEmployee, updateEmployee } from '@/api/employee'

const props = defineProps({
  modelValue: Boolean,
  employee: Object
})
const emit = defineEmits(['update:modelValue', 'saved'])

const visible = ref(false)
const formRef = ref(null)
const saving = ref(false)
const isEdit = ref(false)

const defaultForm = {
  employeeNo: '', name: '', gender: 1, department: '', position: '',
  phone: '', email: '', hireDate: '', status: 'PROBATION',
  baseSalary: 0, isWhitelist: 0, whitelistType: '', remarks: ''
}
const form = reactive({ ...defaultForm })

const rules = {
  employeeNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

watch(() => props.modelValue, val => {
  visible.value = val
  if (val && props.employee) {
    isEdit.value = true
    Object.assign(form, props.employee)
  } else {
    isEdit.value = false
    Object.assign(form, defaultForm)
  }
})

watch(visible, val => emit('update:modelValue', val))

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await updateEmployee(props.employee.id, form)
      ElMessage.success('修改成功')
    } else {
      await createEmployee(form)
      ElMessage.success('新增成功')
    }
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

function handleClose() {
  formRef.value?.resetFields()
}
</script>
