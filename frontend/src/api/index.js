import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '',
  timeout: 120000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code === 0) {
      return res
    }
    const msg = (res && res.msg) || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      localStorage.clear()
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    const body = error.response && error.response.data
    const msg = (body && (body.msg || body.detail)) || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

/* ============ 登录 ============ */
export const login = (data) => request.post('/auth/login', data)

/* ============ 学生 ============ */
export const getStudents = () => request.get('/users/students')

/* ============ 课程 ============ */
export const getCourses = () => request.get('/courses')
export const addCourse = (data) => request.post('/courses', data)
export const updateCourse = (id, data) => request.put(`/courses/${id}`, data)
export const deleteCourse = (id) => request.delete(`/courses/${id}`)

/* ============ 实验室 ============ */
export const getLabs = () => request.get('/labs')
export const addLab = (data) => request.post('/labs', data)
export const updateLab = (id, data) => request.put(`/labs/${id}`, data)
export const deleteLab = (id) => request.delete(`/labs/${id}`)

/* ============ 排课 ============ */
export const getSchedule = (params) => request.get('/schedule', { params })
export const getMySchedule = (params) => request.get('/schedule/mine', { params })
export const addSchedule = (data) => request.post('/schedule', data)
export const updateSchedule = (id, data) => request.put(`/schedule/${id}`, data)
export const deleteSchedule = (id) => request.delete(`/schedule/${id}`)
export const deleteAllSchedules = () => request.delete('/schedule/all')
export const autoSchedule = () => request.post('/schedule/auto')
export const getScheduleTaskStatus = (taskId) => request.get(`/schedule/task-status/${taskId}`)
