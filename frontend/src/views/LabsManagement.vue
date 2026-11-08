<template>
  <div class="page">
    <div class="toolbar">
      <h3>实验室管理</h3>
      <el-button type="primary" @click="openDialog()">新增实验室</el-button>
    </div>

    <el-table :data="labs" v-loading="loading" border stripe>
      <el-table-column prop="name" label="实验室名称" min-width="140" />
      <el-table-column prop="location" label="楼栋" width="100" />
      <el-table-column prop="roomNumber" label="房间号" width="100" />
      <el-table-column prop="capacity" label="容量" width="90" />
      <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑实验室' : '新增实验室'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="楼栋">
          <el-input v-model="form.location" placeholder="如 A" />
        </el-form-item>
        <el-form-item label="房间号" prop="roomNumber">
          <el-input-number v-model="form.roomNumber" :min="100" :max="9999" />
        </el-form-item>
        <el-form-item label="容量" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="500" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLabs, addLab, updateLab, deleteLab } from '../api'

const labs = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)

const emptyForm = () => ({
  id: null,
  name: '',
  location: 'A',
  roomNumber: 101,
  capacity: 30,
  description: ''
})

const form = reactive(emptyForm())

const rules = {
  name: [{ required: true, message: '请输入实验室名称', trigger: 'blur' }],
  roomNumber: [{ required: true, message: '请输入房间号', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const res = await getLabs()
    labs.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  Object.assign(form, emptyForm())
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      location: row.location,
      roomNumber: row.roomNumber,
      capacity: row.capacity,
      description: row.description
    })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateLab(form.id, { ...form })
    } else {
      await addLab({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除实验室《${row.name}》？`, '提示', { type: 'warning' })
  await deleteLab(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.page {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.toolbar h3 {
  margin: 0;
  color: #1f3a5f;
}
</style>
