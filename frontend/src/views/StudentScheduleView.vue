<template>
  <div class="page">
    <div class="toolbar">
      <h3>我的课表</h3>
      <div class="filters">
        <span class="label">周次</span>
        <el-select v-model="week" style="width: 110px" @change="load">
          <el-option v-for="w in 16" :key="w" :label="`第 ${w} 周`" :value="w" />
        </el-select>
      </div>
    </div>

    <el-empty v-if="!loading && schedules.length === 0" description="本周暂无课程安排" />

    <el-table v-else :data="tableRows" border v-loading="loading">
      <el-table-column prop="sectionLabel" label="节次" width="90" fixed />
      <el-table-column v-for="d in DAYS" :key="d.value" :label="d.label" min-width="130">
        <template #default="{ row }">
          <div class="cell">
            <div v-for="item in cellItems(row.section, d.value)" :key="item.id" class="chip">
              <span class="chip-course">{{ item.courseName }}</span>
              <span class="chip-lab">{{ item.labName }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMySchedule } from '../api'

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
const schedules = ref([])
const loading = ref(false)

const tableRows = computed(() =>
  Array.from({ length: 11 }, (_, index) => ({
    section: index + 1,
    sectionLabel: `第 ${index + 1} 节`
  }))
)

const cellItems = (section, day) =>
  schedules.value.filter((item) => item.section === section && item.day === day)

const load = async () => {
  loading.value = true
  try {
    const res = await getMySchedule({ week: week.value })
    schedules.value = res.data || []
  } finally {
    loading.value = false
  }
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

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  color: #606266;
  font-size: 14px;
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
  background: #f0f9eb;
  border-left: 3px solid #67c23a;
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
