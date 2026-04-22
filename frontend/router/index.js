import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'

const getDefaultHomePath = (roleId) => {
  const map = {
    '1': '/home/system/user',
    '2': '/home/student/info',
    '3': '/home/student/info',
    '4': '/home/grade/query',
    '5': '/home/selection'
  }
  return map[roleId] || '/'
}

const routes = [
  {
    path: '/',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'system',
        name: 'System',
        component: () => import('../views/system/Index.vue'),
        meta: { requiresAuth: true, roles: ['1'] },
        children: [
          { path: 'user', name: 'User', component: () => import('../views/system/User.vue'), meta: { requiresAuth: true, roles: ['1'] } },
          { path: 'role', name: 'Role', component: () => import('../views/system/Role.vue'), meta: { requiresAuth: true, roles: ['1'] } },
          { path: 'permission', name: 'Permission', component: () => import('../views/system/Permission.vue'), meta: { requiresAuth: true, roles: ['1'] } }
        ]
      },
      {
        path: 'student',
        name: 'Student',
        component: () => import('../views/student/Index.vue'),
        meta: { requiresAuth: true, roles: ['1', '2', '3'] },
        children: [
          { path: 'info', name: 'StudentInfo', component: () => import('../views/student/Info.vue'), meta: { requiresAuth: true, roles: ['1', '2', '3'] } },
          { path: 'register', name: 'StudentRegister', component: () => import('../views/student/Register.vue'), meta: { requiresAuth: true, roles: ['1', '2', '3'] } }
        ]
      },
      {
        path: 'teacher',
        name: 'Teacher',
        component: () => import('../views/teacher/Index.vue'),
        meta: { requiresAuth: true, roles: ['1', '2', '3'] },
        children: [
          { path: 'info', name: 'TeacherInfo', component: () => import('../views/teacher/Info.vue'), meta: { requiresAuth: true, roles: ['1', '2', '3'] } }
        ]
      },
      {
        path: 'course',
        name: 'Course',
        component: () => import('../views/course/Index.vue'),
        meta: { requiresAuth: true, roles: ['1', '2', '3'] },
        children: [
          { path: 'info', name: 'CourseInfo', component: () => import('../views/course/Info.vue'), meta: { requiresAuth: true, roles: ['1', '2', '3'] } },
          { path: 'plan', name: 'CoursePlan', component: () => import('../views/course/Plan.vue'), meta: { requiresAuth: true, roles: ['1', '2', '3'] } }
        ]
      },
      {
        path: 'schedule',
        name: 'Schedule',
        component: () => import('../views/schedule/Index.vue'),
        meta: { requiresAuth: true, roles: ['1', '2'] },
        children: [
          { path: 'arrange', name: 'ScheduleArrange', component: () => import('../views/schedule/Arrange.vue'), meta: { requiresAuth: true, roles: ['1', '2'] } },
          { path: 'query', name: 'ScheduleQuery', component: () => import('../views/schedule/Query.vue'), meta: { requiresAuth: true, roles: ['1', '2', '4', '5'] } }
        ]
      },
      {
        path: 'selection',
        name: 'Selection',
        component: () => import('../views/selection/Index.vue'),
        meta: { requiresAuth: true, roles: ['5'] }
      },
      {
        path: 'grade',
        name: 'Grade',
        component: () => import('../views/grade/Index.vue'),
        meta: { requiresAuth: true, roles: ['1', '2', '3', '4'] },
        children: [
          { path: 'input', name: 'GradeInput', component: () => import('../views/grade/Input.vue'), meta: { requiresAuth: true, roles: ['1', '2', '3', '4'] } },
          { path: 'query', name: 'GradeQuery', component: () => import('../views/grade/Query.vue'), meta: { requiresAuth: true, roles: ['1', '2', '3', '4', '5'] } },
          { path: 'statistics', name: 'GradeStatistics', component: () => import('../views/grade/Statistics.vue'), meta: { requiresAuth: true, roles: ['1', '2', '3'] } }
        ]
      },
      {
        path: 'attendance',
        name: 'Attendance',
        component: () => import('../views/attendance/Index.vue'),
        meta: { requiresAuth: true, roles: ['1', '2', '3', '4'] },
        children: [
          { path: 'record', name: 'AttendanceRecord', component: () => import('../views/attendance/Record.vue'), meta: { requiresAuth: true, roles: ['1', '2', '3', '4'] } },
          { path: 'statistics', name: 'AttendanceStatistics', component: () => import('../views/attendance/Statistics.vue'), meta: { requiresAuth: true, roles: ['1', '2', '3'] } }
        ]
      },
      {
        path: 'graduation',
        name: 'Graduation',
        component: () => import('../views/graduation/Index.vue'),
        meta: { requiresAuth: true, roles: ['1', '2', '3'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const isLoggedIn = !!store.state.token
  const userRole = store.state.user?.roleId || ''

  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isLoggedIn) {
      next({ path: '/' })
    } else {
      if (to.path === '/home') {
        next(getDefaultHomePath(userRole))
        return
      }
      const requiredRoles = to.meta.roles
      if (requiredRoles && requiredRoles.length > 0 && !requiredRoles.includes(userRole)) {
        next(getDefaultHomePath(userRole))
      } else {
        next()
      }
    }
  } else {
    if (to.path === '/' && isLoggedIn) {
      next('/home')
    } else {
      next()
    }
  }
})

export default router
