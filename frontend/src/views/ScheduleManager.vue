<template>
  <div class="page">
    <div class="toolbar">
      <div class="filters">
        <span class="label">周次</span>
        <el-select v-model="week" style="width: 110px" @change="load">
          <el-option v-for="w in 16" :key="w" :label="`第 ${w} 周`" :value="w" />
        </el-select>
        <span class="label">实验室</span>
        <el-select v-model="activeLab" clearable placeholder="全部实验室" style="width: 180px" @change="load">
          <el-option v-for="lab in labs" :key="lab.id" :label="lab.name" :value="lab.id" />
        </el-select>
      </div>
      <div class="actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button @click="handleClearAll">清空课表</el-button>
        <el-button type="primary" :loading="autoRunning" @click="handleAutoSchedule">自动排课</el-button>
      </div>
    </div>

    <el-alert v-if="task.visible" :type="task.type" :closable="false" class="task-bar">
      <div class="task-content">
        <span>{{ task.message }}</span>
        <el-progress :percentage="task.progress" :status="task.status" style="flex: 1; margin: 0 16px" />
      </div>
    </el-alert>

    <el-table :data="tableRows" border class="grid" :cell-style="cellStyle">
      <el-table-column prop="sectionLabel" label="节次" width="90" fixed />
      <el-table-column v-for="d in DAYS" :key="d.value" :label="d.label" min-width="130">
        <template #default="{ row }">
          <div class="cell" @click="openDialog(row.section, d.value)">
            <div v-for="item in cellItems(row.section, d.value)" :key="item.id" class="chip">
              <span class="chip-course">{{ item.courseName }}</span>
              <span class="chip-lab">{{ item.labName }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑排课' : '新增排课'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="form.courseId" filterable placeholder="选择课程" style="width: 100%" @change="onCourseChange">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="实验室" prop="labId">
          <el-select v-model="form.labId" placeholder="选择实验室" style="width: 100%" @change="onLabChange">
            <el-option v-for="lab in labs" :key="lab.id" :label="`${lab.name}（${lab.capacity} 座）`" :value="lab.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="周次" prop="week">
          <el-input-number v-model="form.week" :min="1" :max="16" />
        </el-form-item>
        <el-form-item label="星期" prop="day">
          <el-select v-model="form.day" style="width: 100%">
            <el-option v-for="d in DAYS" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="节次" prop="section">
          <el-input-number v-model="form.section" :min="1" :max="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="form.id" type="danger" plain @click="handleDelete">删除</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSchedule,
  getLabs,
  getCourses,
  addSchedule,
  updateSchedule,
  deleteSchedule,
  deleteAllSchedules,
  autoSchedule,
  getScheduleTaskStatus
} from '../api'

const DAYS = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' }
]

const week = ref(1)
const activeLab = ref(null)
const schedules = ref([])
const labs = ref([])
const courses = ref([])
const loading = ref(false)
const saving = ref(false)
const autoRunning = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
let pollTimer = null

const task = reactive({
  visible: false,
  type: 'info',
  status: '',
  progress: 0,
  message: ''
})

const emptyForm = () => ({
  id: null,
  courseId: null,
  courseName: '',
  labId: null,
  labName: '',
  week: week.value,
  day: 1,
  section: 1
})

const form = reactive(emptyForm())

const rules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  labId: [{ required: true, message: '请选择实验室', trigger: 'change' }]
}

const tableRows = computed(() =>
  Array.from({ length: 11 }, (_, index) => ({
    section: index + 1,
    sectionLabel: `第 ${index + 1} 节`
  }))
)

const visibleSchedules = computed(() =>
  activeLab.value ? schedules.value.filter((item) => item.labId === activeLab.value) : schedules.value
)

const cellItems = (section, day) =>
  visibleSchedules.value.filter((item) => item.section === section && item.day === day)

const cellStyle = () => ({ cursor: 'pointer', verticalAlign: 'top' })

const load = async () => {
  loading.value = true
  try {
    const res = await getSchedule({ week: week.value })
    schedules.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadBase = async () => {
  const [labRes, courseRes] = await Promise.all([getLabs(), getCourses()])
  labs.value = labRes.data || []
  courses.value = courseRes.data || []
}

const onCourseChange = (id) => {
  const course = courses.value.find((item) => item.id === id)
  form.courseName = course ? course.name : ''
}

const onLabChange = (id) => {
  const lab = labs.value.find((item) => item.id === id)
  form.labName = lab ? lab.name : ''
}

const openDialog = (section, day, row) => {
  Object.assign(form, emptyForm())
  form.week = week.value
  form.section = section
  form.day = day
  if (row) {
    Object.assign(form, {
      id: row.id,
      courseId: row.courseId,
      courseName: row.courseName,
      labId: row.labId,
      labName: row.labName,
      week: row.week,
      day: row.day,
      section: row.section
    })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      week: form.week,
      day: form.day,
      section: form.section,
      courseId: form.courseId,
      courseName: form.courseName,
      labId: form.labId,
      labName: form.labName
    }
    if (form.id) {
      await updateSchedule(form.id, payload)
    } else {
      await addSchedule(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const handleDelete = async () => {
  await ElMessageBox.confirm('确定删除这条排课记录？', '提示', { type: 'warning' })
  await deleteSchedule(form.id)
  ElMessage.success('已删除')
  dialogVisible.value = false
  await load()
}

const handleClearAll = async () => {
  await ElMessageBox.confirm('清空所有排课记录？该操作不可恢复。', '提示', { type: 'warning' })
  await deleteAllSchedules()
  ElMessage.success('已清空')
  await load()
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const pollStatus = (taskId) => {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const res = await getScheduleTaskStatus(taskId)
      const data = res.data || {}
      task.visible = true
      task.progress = data.progress || 0
      task.message = data.message || '排课进行中...'
      if (data.status === 'COMPLETED') {
        task.type = 'success'
        task.status = 'success'
        task.progress = 100
        stopPolling()
        autoRunning.value = false
        ElMessage.success(data.message || '排课完成')
        await load()
      } else if (data.status === 'FAILED' || data.status === 'NOT_FOUND') {
        task.type = 'error'
        task.status = 'exception'
        stopPolling()
        autoRunning.value = false
        ElMessage.error(data.message || '排课失败')
      }
    } catch (e) {
      stopPolling()
      autoRunning.value = false
    }
  }, 3000)
}

const handleAutoSchedule = async () => {
  if (!courses.value.length) {
    ElMessage.warning('请先维护课程数据')
    return
  }
  if (!labs.value.length) {
    ElMessage.warning('请先维护实验室数据')
    return
  }
  autoRunning.value = true
  task.visible = true
  task.type = 'info'
  task.status = ''
  task.progress = 0
  task.message = '正在提交排课任务...'
  try {
    const res = await autoSchedule()
    const taskId = res.data
    task.message = '任务已提交，正在求解...'
    pollStatus(taskId)
  } catch (e) {
    autoRunning.value = false
    task.type = 'error'
    task.status = 'exception'
    task.message = '提交排课任务失败'
  }
}

onMounted(async () => {
  await loadBase()
  await load()
})

onBeforeUnmount(stopPolling)
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
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  color: #606266;
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 8px;
}

.task-bar {
  margin-bottom: 16px;
}

.task-content {
  display: flex;
  align-items: center;
}

.cell {
  min-height: 36px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chip {
  display: flex;
  flex-direction: column;
  padding: 4px 6px;
  border-radius: 4px;
  background: #ecf5ff;
  border-left: 3px solid #409eff;
  font-size: 12px;
  line-height: 1.4;
}

.chip-course {
  color: #1f3a5f;
  font-weight: 600;
}

.chip-lab {
  color: #909399;
}
</style>
