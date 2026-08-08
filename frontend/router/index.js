/**
 * 路由配置模块，负责注册页面路由与权限守卫。
 */
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import store from '../store'

const validRoleIds = new Set(['1', '2', '3', '4', '5'])

// 获取默认首页路径
export const getDefaultHomePath = (roleId) => {
  const map = {
    '1': '/home/dashboard',
    '2': '/home/student/info',
    '3': '/home/student/info',
    '4': '/home/grade/query',
    '5': '/home/selection'
  }
  return map[roleId] || null
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
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/dashboard/Index.vue'),
        meta: { requiresAuth: true, roles: ['1'], title: '数据看板' }
      },
      {
        path: 'profile',
        name: 'ProfileCenter',
        component: () => import('../views/profile/Index.vue'),
        meta: { requiresAuth: true, roles: ['1', '2', '3', '4', '5'], title: '个人中心' }
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('../views/system/Index.vue'),
        meta: { requiresAuth: true, roles: ['1'] },
        children: [
          { path: 'user', name: 'User', component: () => import('../views/system/User.vue'), meta: { requiresAuth: true, roles: ['1'] } },
          { path: 'role', name: 'Role', component: () => import('../views/system/Role.vue'), meta: { requiresAuth: true, roles: ['1'] } },
          { path: 'permission', name: 'Permission', component: () => import('../views/system/Permission.vue'), meta: { requiresAuth: true, roles: ['1'] } },
          { path: 'model', name: 'Model', component: () => import('../views/system/Model.vue'), meta: { requiresAuth: true, roles: ['1'] } }
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
        path: 'leave/my',
        name: 'MyLeaveRequests',
        component: () => import('../views/leave/My.vue'),
        meta: { requiresAuth: true, roles: ['5'], title: '我的请假' }
      },
      {
        path: 'leave/approval',
        name: 'LeaveApproval',
        component: () => import('../views/leave/Approval.vue'),
        meta: { requiresAuth: true, roles: ['1', '2', '3'], title: '请假审批' }
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
      },
      {
        path: 'analysis',
        name: 'AnalysisAgent',
        redirect: '/home/agent/analysis',
        meta: { requiresAuth: true, roles: ['1', '2', '3'] }
      },
      {
        path: 'agent/analysis',
        name: 'AgentAnalysis',
        component: () => import('../views/agent/Analysis.vue'),
        meta: { requiresAuth: true, roles: ['1', '2', '3'] }
      },
      {
        path: 'agent/warning',
        name: 'AgentWarning',
        component: () => import('../views/agent/Warning.vue'),
        meta: { requiresAuth: true, roles: ['1'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/** 纯权限决策函数供路由守卫和单元测试共用。 */
export const resolveRouteAccess = ({ path, requiresAuth, requiredRoles }, { token, user }) => {
  const roleId = user?.roleId || ''
  const isLoggedIn = !!token
  const hasValidRole = validRoleIds.has(roleId)

  if (requiresAuth && (!isLoggedIn || !hasValidRole)) {
    return { allow: false, redirect: '/', reason: 'invalid-auth', clearAuth: isLoggedIn || !!user }
  }
  if (requiresAuth && path === '/home') {
    return { allow: false, redirect: getDefaultHomePath(roleId) || '/', reason: 'default-home' }
  }
  if (requiresAuth && requiredRoles?.length && !requiredRoles.includes(roleId)) {
    return { allow: false, redirect: getDefaultHomePath(roleId) || '/', reason: 'forbidden' }
  }
  if (path === '/' && isLoggedIn && hasValidRole) {
    return { allow: false, redirect: '/home', reason: 'already-authenticated' }
  }
  if (path === '/' && isLoggedIn && !hasValidRole) {
    return { allow: false, redirect: '/', reason: 'invalid-auth', clearAuth: true }
  }
  return { allow: true }
}

// 执行路由跳转前校验
router.beforeEach((to, from, next) => {
  const decision = resolveRouteAccess({
    path: to.path,
    requiresAuth: to.matched.some(record => record.meta.requiresAuth),
    requiredRoles: to.meta.roles
  }, store.state)

  if (decision.clearAuth) store.dispatch('logout')
  if (decision.reason === 'forbidden') ElMessage.warning('当前账号无权访问该页面，已返回角色工作台')
  if (decision.allow) next()
  else next(decision.redirect)
})

export default router
