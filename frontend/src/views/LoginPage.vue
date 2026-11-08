<template>
  <div class="login-page">
    <div class="login-card">
      <h2>实验室排课系统</h2>
      <p class="subtitle">只保留课程与排课管理的最小版本</p>
      <el-form :model="form" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="身份">
          <el-radio-group v-model="form.role">
            <el-radio-button label="admin">管理员</el-radio-button>
            <el-radio-button label="student">学生</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" :placeholder="form.role === 'admin' ? 'admin' : 'student1'" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="123456" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="handleLogin">登录</el-button>
      </el-form>
      <div class="tips">
        演示账号：管理员 admin / 学生 student1、student2，密码均为 123456
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456', role: 'admin' })

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login({ ...form })
    const data = res.data || {}
    localStorage.setItem('token', data.token)
    localStorage.setItem('userRole', data.role)
    localStorage.setItem('username', data.username)
    localStorage.setItem('realName', data.realName || data.username)
    localStorage.setItem('userId', data.id)
    ElMessage.success('登录成功')
    router.push(data.role === 'admin' ? '/admin/schedule' : '/student/schedule')
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3a5f 0%, #2f6f8f 100%);
}

.login-card {
  width: 380px;
  padding: 32px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
}

h2 {
  margin: 0 0 4px;
  text-align: center;
  color: #1f3a5f;
}

.subtitle {
  margin: 0 0 24px;
  text-align: center;
  color: #909399;
  font-size: 12px;
}

.submit {
  width: 100%;
}

.tips {
  margin-top: 16px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
