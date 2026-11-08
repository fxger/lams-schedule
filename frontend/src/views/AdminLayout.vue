<template>
  <div class="layout">
    <header class="header">
      <div class="brand">实验室排课系统</div>
      <el-menu mode="horizontal" :default-active="activeMenu" router class="menu" :ellipsis="false">
        <el-menu-item index="/admin/schedule">课表与排课</el-menu-item>
        <el-menu-item index="/admin/courses">课程管理</el-menu-item>
        <el-menu-item index="/admin/labs">实验室管理</el-menu-item>
      </el-menu>
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
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const realName = computed(() => localStorage.getItem('realName') || '管理员')
const activeMenu = computed(() => route.path)

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
  gap: 24px;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.brand {
  font-size: 18px;
  font-weight: 600;
  color: #1f3a5f;
  white-space: nowrap;
}

.menu {
  flex: 1;
  border-bottom: none;
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
