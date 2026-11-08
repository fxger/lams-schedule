<template>
  <div class="page">
    <div class="toolbar">
      <h3>课程管理</h3>
      <el-button type="primary" @click="openDialog()">新增课程</el-button>
    </div>

    <el-table :data="courses" v-loading="loading" border stripe>
      <el-table-column prop="name" label="课程名称" min-width="140" />
      <el-table-column prop="code" label="课程编号" width="120" />
      <el-table-column prop="semester" label="学期" width="130" />
      <el-table-column prop="credits" label="学分" width="80" />
      <el-table-column prop="weeks" label="周数" width="80" />
      <el-table-column prop="periods" label="每周节数" width="100" />
      <el-table-column prop="studentCount" label="选课人数" width="100" />
      <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑课程' : '新增课程'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="课程编号">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="学期">
          <el-input v-model="form.semester" placeholder="如 2026-2027-1" />
        </el-form-item>
        <el-form-item label="学分">
          <el-input-number v-model="form.credits" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="周数">
          <el-input-number v-model="form.weeks" :min="1" :max="16" />
        </el-form-item>
        <el-form-item label="每周节数">
          <el-input-number v-model="form.periods" :min="1" :max="8" />
        </el-form-item>
        <el-form-item label="上课学生">
          <el-select v-model="form.studentIds" multiple filterable placeholder="选择学生" style="width: 100%">
            <el-option v-for="s in students" :key="s.id" :label="`${s.realName}（${s.username}）`" :value="s.id" />
          </el-select>
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
import { getCourses, addCourse, updateCourse, deleteCourse, getStudents } from '../api'

const courses = ref([])
const students = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)

const emptyForm = () => ({
  id: null,
  name: '',
  code: '',
  semester: '2026-2027-1',
  credits: 3,
  weeks: 8,
  periods: 2,
  description: '',
  studentIds: []
})

const form = reactive(emptyForm())

const rules = {
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const res = await getCourses()
    courses.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadStudents = async () => {
  const res = await getStudents()
  students.value = res.data || []
}

const openDialog = (row) => {
  Object.assign(form, emptyForm())
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      code: row.code,
      semester: row.semester,
      credits: row.credits,
      weeks: row.weeks,
      periods: row.periods,
      description: row.description,
      studentIds: row.studentIds || []
    })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      await updateCourse(form.id, payload)
    } else {
      await addCourse(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`删除课程《${row.name}》会同时删除它的排课与选课记录，确定继续？`, '提示', {
    type: 'warning'
  })
  await deleteCourse(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(async () => {
  await Promise.all([load(), loadStudents()])
})
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
