/** 角色导航配置与编号逻辑保持独立，供应用外壳和权限测试共用。 */
import { markRaw } from 'vue'
import {
  Avatar,
  Calendar,
  Checked,
  Clock,
  DataAnalysis,
  Document,
  Files,
  List,
  MagicStick,
  Reading,
  Setting,
  User
} from '@element-plus/icons-vue'

export const navSections = [
  { label: '工作台', items: [{ path: '/home/dashboard', label: '数据看板', roles: ['1'], icon: markRaw(DataAnalysis) }] },
  { label: '账号', items: [
    { path: '/home/profile', label: '个人中心', roles: ['1', '2', '3', '4', '5'], icon: markRaw(User) }
  ] },
  { label: '系统管理', items: [
    { path: '/home/system/user', label: '用户管理', roles: ['1'], icon: markRaw(User) },
    { path: '/home/system/role', label: '角色管理', roles: ['1'], icon: markRaw(Setting) },
    { path: '/home/system/permission', label: '权限管理', roles: ['1'], icon: markRaw(Files) }
  ] },
  { label: '基础档案', items: [
    { path: '/home/student/info', label: '学生信息', roles: ['1', '2', '3'], icon: markRaw(User) },
    { path: '/home/student/register', label: '学生注册', roles: ['1', '2', '3'], icon: markRaw(List) },
    { path: '/home/teacher/info', label: '教师档案', roles: ['1', '2', '3'], icon: markRaw(Avatar) }
  ] },
  { label: '教务运行', items: [
    { path: '/home/course/info', label: '课程管理', roles: ['1', '2', '3'], icon: markRaw(Reading) },
    { path: '/home/course/plan', label: '教学计划', roles: ['1', '2', '3'], icon: markRaw(Document) },
    { path: '/home/schedule/arrange', label: '排课管理', roles: ['1', '2'], icon: markRaw(Calendar) },
    { path: '/home/schedule/query', label: '课表查询', roles: ['1', '2', '4', '5'], icon: markRaw(Calendar) }
  ] },
  { label: '教学过程', items: [
    { path: '/home/selection', label: '选课系统', roles: ['5'], icon: markRaw(List) },
    { path: '/home/leave/my', label: '我的请假', roles: ['5'], icon: markRaw(Document) },
    { path: '/home/leave/approval', label: '请假审批', roles: ['1', '2', '3'], icon: markRaw(Checked) },
    { path: '/home/grade/input', label: '成绩录入', roles: ['1', '2', '3', '4'], icon: markRaw(Document) },
    { path: '/home/grade/query', label: '成绩查询', roles: ['1', '2', '3', '4', '5'], icon: markRaw(Document) },
    { path: '/home/grade/statistics', label: '成绩统计', roles: ['1', '2', '3'], icon: markRaw(DataAnalysis) },
    { path: '/home/attendance/record', label: '考勤记录', roles: ['1', '2', '3', '4'], icon: markRaw(Clock) },
    { path: '/home/attendance/statistics', label: '考勤统计', roles: ['1', '2', '3'], icon: markRaw(Clock) },
    { path: '/home/graduation', label: '毕业审核', roles: ['1', '2', '3'], icon: markRaw(Checked) }
  ] },
  { label: '智能教务', items: [
    { path: '/home/agent/analysis', label: 'AI 数据分析', roles: ['1', '2', '3'], icon: markRaw(DataAnalysis) },
    { path: '/home/agent/warning', label: '学业预警', roles: ['1'], icon: markRaw(MagicStick) }
  ] }
]

export const getVisibleNavigation = (roleId) => {
  let number = 0
  return navSections
    .map(section => ({
      ...section,
      items: section.items
        .filter(item => item.roles.includes(roleId))
        .map(item => ({ ...item, number: String(++number).padStart(2, '0') }))
    }))
    .filter(section => section.items.length)
}
