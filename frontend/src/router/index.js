import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginPage.vue')
  },
  {
    path: '/admin',
    component: () => import('../views/AdminLayout.vue'),
    meta: { role: 'admin' },
    children: [
      { path: '', redirect: '/admin/schedule' },
      {
        path: 'schedule',
        name: 'AdminSchedule',
        component: () => import('../views/ScheduleManager.vue')
      },
      {
        path: 'courses',
        name: 'AdminCourses',
        component: () => import('../views/CourseManager.vue')
      },
      {
        path: 'labs',
        name: 'AdminLabs',
        component: () => import('../views/LabsManagement.vue')
      }
    ]
  },
  {
    path: '/student',
    component: () => import('../views/StudentLayout.vue'),
    meta: { role: 'student' },
    children: [
      { path: '', redirect: '/student/schedule' },
      {
        path: 'schedule',
        name: 'StudentSchedule',
        component: () => import('../views/StudentScheduleView.vue')
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const role = localStorage.getItem('userRole')
  const token = localStorage.getItem('token')
  if (to.path === '/login') {
    return true
  }
  if (!token || !role) {
    return { path: '/login' }
  }
  if (to.meta.role && to.meta.role !== role) {
    return { path: role === 'admin' ? '/admin/schedule' : '/student/schedule' }
  }
  return true
})

export default router
