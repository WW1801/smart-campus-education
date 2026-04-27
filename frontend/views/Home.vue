<!-- 首页布局组件，负责承载系统主框架与导航布局。 -->
<template>
  <div class="layout-container">
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="logo">
            <el-icon :size="24" color="#409EFF"><School /></el-icon>
            <span class="logo-text">教务管理系统</span>
          </div>
        </div>
        <div class="header-right">
          <span class="user-info">{{ user?.username || '用户' }}</span>
          <el-tag :type="roleTagType" size="small" style="margin-right: 12px;">{{ roleName }}</el-tag>
          <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <el-container>
        <el-aside width="220px" class="aside">
          <el-menu
            :default-active="activeMenu"
            class="side-menu"
            router
            background-color="#001529"
            text-color="rgba(255,255,255,0.65)"
            active-text-color="#409EFF"
          >
            <el-sub-menu v-if="hasRole(['1'])" index="system">
              <template #title><el-icon><Setting /></el-icon><span>系统管理</span></template>
              <el-menu-item index="/home/system/user">用户管理</el-menu-item>
              <el-menu-item index="/home/system/role">角色管理</el-menu-item>
              <el-menu-item index="/home/system/permission">权限管理</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="hasRole(['1','2','3'])" index="student">
              <template #title><el-icon><User /></el-icon><span>学生管理</span></template>
              <el-menu-item index="/home/student/info">学生信息</el-menu-item>
              <el-menu-item index="/home/student/register">学生注册</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="hasRole(['1','2','3'])" index="teacher">
              <template #title><el-icon><Avatar /></el-icon><span>教师管理</span></template>
              <el-menu-item index="/home/teacher/info">教师信息</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="hasRole(['1','2','3'])" index="course">
              <template #title><el-icon><Reading /></el-icon><span>课程管理</span></template>
              <el-menu-item index="/home/course/info">课程信息</el-menu-item>
              <el-menu-item index="/home/course/plan">教学计划</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="hasRole(['1','2'])" index="schedule">
              <template #title><el-icon><Calendar /></el-icon><span>排课管理</span></template>
              <el-menu-item index="/home/schedule/arrange">排课安排</el-menu-item>
              <el-menu-item index="/home/schedule/query">课表查询</el-menu-item>
            </el-sub-menu>

            <el-menu-item v-if="hasRole(['5'])" index="/home/selection">
              <el-icon><List /></el-icon><span>选课系统</span>
            </el-menu-item>

            <el-sub-menu v-if="hasRole(['1','2','3','4'])" index="grade">
              <template #title><el-icon><Document /></el-icon><span>成绩管理</span></template>
              <el-menu-item v-if="hasRole(['1','2','3','4'])" index="/home/grade/input">成绩录入</el-menu-item>
              <el-menu-item index="/home/grade/query">成绩查询</el-menu-item>
              <el-menu-item v-if="hasRole(['1','2','3'])" index="/home/grade/statistics">成绩统计</el-menu-item>
            </el-sub-menu>

            <el-sub-menu v-if="hasRole(['1','2','3','4'])" index="attendance">
              <template #title><el-icon><Clock /></el-icon><span>考勤管理</span></template>
              <el-menu-item index="/home/attendance/record">考勤记录</el-menu-item>
              <el-menu-item v-if="hasRole(['1','2','3'])" index="/home/attendance/statistics">考勤统计</el-menu-item>
            </el-sub-menu>

            <el-menu-item v-if="hasRole(['1','2','3'])" index="/home/graduation">
              <el-icon><Checked /></el-icon><span>毕业管理</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { School, Setting, User, Avatar, Reading, Calendar, List, Document, Clock, Checked } from '@element-plus/icons-vue'

const store = useStore()
const router = useRouter()
const route = useRoute()

const user = computed(() => store.state.user)
const roleId = computed(() => store.state.user?.roleId || '')

const roleMap = { '1': '系统管理员', '2': '教务处管理员', '3': '院系管理员', '4': '教师', '5': '学生' }
const roleName = computed(() => roleMap[roleId.value] || '未知角色')
const roleTagType = computed(() => {
  const map = { '1': 'danger', '2': 'warning', '3': '', '4': 'success', '5': 'info' }
  return map[roleId.value] || 'info'
})

const activeMenu = computed(() => route.path)

// 判断是否具备角色
const hasRole = (roles) => roles.includes(roleId.value)

// 处理退出登录
const handleLogout = async () => {
  await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
  store.dispatch('logout')
  router.push('/')
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.el-container { height: 100%; }

.header {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-bottom: 1px solid #e8e8e8; padding: 0 24px; height: 60px;
}

.header-left { display: flex; align-items: center; }
.logo { display: flex; align-items: center; gap: 10px; }
.logo-text { font-size: 18px; font-weight: 700; color: #303133; }

.header-right { display: flex; align-items: center; gap: 8px; }
.user-info { font-size: 14px; color: #606266; }

.aside { background: #001529; overflow-y: auto; }
.side-menu { border-right: none; }
.side-menu .el-menu-item.is-active { background-color: rgba(64, 158, 255, 0.15) !important; }

.main { background: #f0f2f5; padding: 20px; overflow-y: auto; }
</style>
