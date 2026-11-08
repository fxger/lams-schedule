<template>
  <div class="layout">
    <header class="header">
      <div class="brand">实验室排课系统 · 学生端</div>
      <div class="user">
        <span>{{ realName }}</span>
        <el-button link type="primary" @click="logout">退出</el-button>
      </div>
    </header>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const realName = computed(() => localStorage.getItem('realName') || '学生')

const logout = () => {
  localStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.brand {
  font-size: 18px;
  font-weight: 600;
  color: #1f3a5f;
}

.user {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #606266;
}

.content {
  padding: 20px 24px 32px;
}
</style>
