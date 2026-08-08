<!-- 管理端应用外壳：保留路由、角色权限、AI 分析、改密和退出调用链。 -->
<template>
  <div class="campus-shell" :class="{ 'is-collapsed': isCollapsed, 'nav-open': mobileNavOpen }">
    <aside
      ref="sidebarRef"
      class="sidebar"
      aria-label="教务系统主导航"
      :aria-hidden="isMobile && !mobileNavOpen"
      :inert="isMobile && !mobileNavOpen"
    >
      <router-link class="brand" :to="homePath" aria-label="教务管理工作台">
        <span class="brand-mark" aria-hidden="true">教</span>
        <span v-show="!isCollapsed" class="brand-copy">
          <strong>教务管理</strong>
          <small>CAMPUS CONSOLE</small>
        </span>
      </router-link>

      <nav id="campus-navigation" class="nav-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          :collapse-transition="false"
          router
          class="side-menu"
        >
          <template v-for="section in visibleSections" :key="section.label">
            <li v-if="!isCollapsed" class="nav-section-title">{{ section.label }}</li>
            <el-menu-item v-for="item in section.items" :key="item.path" :index="item.path">
              <span class="nav-number" aria-hidden="true">{{ item.number }}</span>
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title><span>{{ item.label }}</span></template>
            </el-menu-item>
          </template>
        </el-menu>
      </nav>

      <div class="term-panel">
        <span class="term-seal" aria-hidden="true">期</span>
        <span v-show="!isCollapsed" class="term-copy">
          <strong>{{ semesterLabel }}</strong>
          <small>{{ semesterStatusText }}</small>
        </span>
      </div>
    </aside>

    <button
      v-if="mobileNavOpen"
      class="nav-backdrop"
      type="button"
      aria-label="关闭导航"
      @click="closeMobileNav"
    ></button>

    <header class="topbar" :inert="isMobile && mobileNavOpen">
      <div class="topbar-leading">
        <button
          ref="menuToggleRef"
          class="menu-toggle"
          type="button"
          :aria-expanded="mobileNavOpen"
          aria-controls="campus-navigation"
          :aria-label="mobileNavOpen ? '关闭导航' : '打开导航'"
          @click="mobileNavOpen = !mobileNavOpen"
        >
          <el-icon><Menu /></el-icon>
        </button>
        <div class="breadcrumb" aria-label="当前位置">
          <span>{{ currentSection }}</span>
          <span class="breadcrumb-separator">/</span>
          <strong>{{ currentPageTitle }}</strong>
        </div>
      </div>

      <div class="desktop-account-actions">
        <el-button v-if="canUseAiAnalysis" class="ai-button" type="primary" :loading="aiLoading" @click="handleAiAnalysis">
          <el-icon><MagicStick /></el-icon>
          AI 分析
        </el-button>
        <span class="user-info" :title="loginIdentity">
          <strong>{{ user?.username || '账号未设置' }}</strong>
          <span v-if="user?.name">· {{ user.name }}</span>
        </span>
        <span class="role-stamp">{{ roleName }}</span>
        <el-button @click="openProfileCenter">个人中心</el-button>
        <el-button @click="openPasswordDialog">修改密码</el-button>
        <el-button class="logout-button" @click="handleLogout">退出登录</el-button>
      </div>

      <div class="mobile-account-actions">
        <el-button v-if="canUseAiAnalysis" class="mobile-ai-button" type="primary" circle :loading="aiLoading" aria-label="AI 分析" @click="handleAiAnalysis">
          <el-icon><MagicStick /></el-icon>
        </el-button>
        <el-dropdown trigger="click" @command="handleAccountCommand">
          <button class="account-trigger" type="button" aria-label="打开账号菜单">
            <span>{{ accountInitial }}</span>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                {{ user?.username || '账号未设置' }}<template v-if="user?.name"> · {{ user.name }}</template>
              </el-dropdown-item>
              <el-dropdown-item disabled>{{ roleName }}</el-dropdown-item>
              <el-dropdown-item divided command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="workspace" tabindex="-1" :inert="isMobile && mobileNavOpen">
      <router-view />
    </main>

    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="440px"
      :close-on-click-modal="false"
      @opened="focusPasswordField"
    >
      <el-alert
        title="新密码需为 8 至 64 位，并同时包含字母和数字；提交失败时会保留已填写内容。"
        type="info"
        :closable="false"
        show-icon
      />
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-position="top"
        class="password-form"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            ref="oldPasswordInput"
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请输入 8 至 64 位，包含字母和数字"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请再次输入新密码"
            @keyup.enter="submitPasswordChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="submitPasswordChange">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message-box/style/css'
import {
  ArrowDown,
  MagicStick,
  Menu
} from '@element-plus/icons-vue'
import appData from '../config/appData.json'
import request from '../utils/request'
import { useCurrentSemester } from '../composables/useCurrentSemester'
import { getVisibleNavigation } from '../config/navigation'

const store = useStore()
const router = useRouter()
const route = useRoute()
const { semesterLabel, semesterLoading, semesterError, loadCurrentSemester } = useCurrentSemester()

const user = computed(() => store.state.user)
const roleId = computed(() => store.state.user?.roleId || '')
const viewportWidth = ref(window.innerWidth)
const mobileNavOpen = ref(false)
const sidebarRef = ref(null)
const menuToggleRef = ref(null)
const aiLoading = ref(false)
const passwordDialogVisible = ref(false)
const passwordSaving = ref(false)
const passwordFormRef = ref(null)
const oldPasswordInput = ref(null)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const roleMap = appData.roles.names
const roleName = computed(() => roleMap[roleId.value] || '未知角色')
const loginIdentity = computed(() => [user.value?.username, user.value?.name].filter(Boolean).join(' · '))
const accountInitial = computed(() => (user.value?.name || user.value?.username || '账').slice(0, 1))
const semesterStatusText = computed(() => {
  if (semesterLoading.value) return '正在读取当前学期'
  if (semesterError.value) return '学期服务不可用'
  return '教务运行中'
})
const isCollapsed = computed(() => viewportWidth.value <= 1024 && viewportWidth.value > 768)
const isMobile = computed(() => viewportWidth.value <= 768)
const canUseAiAnalysis = computed(() => ['1', '2', '3'].includes(roleId.value))
const activeMenu = computed(() => route.path)
const homePath = computed(() => roleId.value === '1' ? '/home/dashboard' : '/home')

const visibleSections = computed(() => getVisibleNavigation(roleId.value))

const currentNavigationItem = computed(() => visibleSections.value
  .flatMap(section => section.items.map(item => ({ ...item, section: section.label })))
  .find(item => item.path === route.path))
const currentSection = computed(() => currentNavigationItem.value?.section || '工作台')
const currentPageTitle = computed(() => currentNavigationItem.value?.label || route.meta.title || '教务管理')

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入新密码'))
  if (value !== passwordForm.newPassword) return callback(new Error('两次输入的新密码不一致'))
  callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '新密码长度应为 8 至 64 位', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) return callback(new Error('新密码需同时包含字母和数字'))
        callback()
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const updateViewport = () => {
  viewportWidth.value = window.innerWidth
  if (!isMobile.value) mobileNavOpen.value = false
}

const closeMobileNav = () => {
  mobileNavOpen.value = false
}

const handleEscape = (event) => {
  if (event.key === 'Escape' && mobileNavOpen.value) closeMobileNav()
}

const openPasswordDialog = () => {
  Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  passwordFormRef.value?.clearValidate()
  passwordDialogVisible.value = true
}

const focusPasswordField = () => {
  oldPasswordInput.value?.focus()
}

const submitPasswordChange = async () => {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }

  passwordSaving.value = true
  try {
    const res = await request.put('/account/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success(res.message || '密码修改成功')
    passwordDialogVisible.value = false
  } catch {
    // 错误提示由请求拦截器统一展示；这里消化拒绝，避免事件处理器产生未处理异常。
  } finally {
    // 请求失败时仅恢复按钮状态，不清空用户已经输入的密码。
    passwordSaving.value = false
  }
}

const handleAiAnalysis = async () => {
  if (route.path === '/home/agent/analysis') return
  aiLoading.value = true
  try {
    await router.push('/home/agent/analysis')
  } finally {
    aiLoading.value = false
  }
}

const openProfileCenter = () => {
  if (route.path !== '/home/profile') router.push('/home/profile')
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确认退出当前账号？', '退出登录', {
      confirmButtonText: '退出登录',
      cancelButtonText: '继续使用',
      confirmButtonClass: 'logout-confirm-button',
      customClass: 'logout-confirm',
      closeOnClickModal: false,
      type: 'warning'
    })
  } catch {
    return
  }
  await store.dispatch('logout')
  await router.push('/')
}

const handleAccountCommand = (command) => {
  if (command === 'profile') openProfileCenter()
  if (command === 'password') openPasswordDialog()
  if (command === 'logout') handleLogout()
}

watch(() => route.path, closeMobileNav)
watch(mobileNavOpen, async (isOpen, wasOpen) => {
  if (!isMobile.value) return
  await nextTick()
  if (isOpen) {
    sidebarRef.value?.querySelector('.el-menu-item')?.focus()
  } else if (wasOpen) {
    menuToggleRef.value?.focus()
  }
})

onMounted(() => {
  loadCurrentSemester()
  window.addEventListener('resize', updateViewport, { passive: true })
  window.addEventListener('keydown', handleEscape)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
  window.removeEventListener('keydown', handleEscape)
})
</script>

<style scoped>
.campus-shell {
  min-height: 100vh;
  padding-left: var(--sidebar-width);
  background: var(--paper);
}

.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 40;
  display: flex;
  width: var(--sidebar-width);
  color: #fff;
  background: var(--sidebar-grad);
  flex-direction: column;
  transition: transform 180ms ease-out;
}

.brand {
  display: flex;
  min-height: 78px;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  text-decoration: none;
}

.brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  place-items: center;
  background: var(--button-grad);
  border-radius: 9px;
  box-shadow: 0 5px 12px rgba(8, 20, 36, 0.28);
  font-family: var(--display-font);
  font-size: 22px;
  font-weight: 700;
}

.brand-copy {
  min-width: 0;
}

.brand-copy strong,
.brand-copy small {
  display: block;
  white-space: nowrap;
}

.brand-copy strong {
  font-family: var(--display-font);
  font-size: 18px;
  letter-spacing: 0.03em;
}

.brand-copy small {
  margin-top: 1px;
  color: rgba(255, 255, 255, 0.56);
  font-family: var(--latin-font);
  font-size: 9px;
  letter-spacing: 0.18em;
}

.nav-scroll {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
}

.side-menu {
  border: 0;
  background: transparent;
}

.side-menu:not(.el-menu--collapse) {
  width: 100%;
}

.nav-section-title {
  padding: 22px 20px 7px;
  color: rgba(255, 255, 255, 0.42);
  font-size: 11px;
  font-weight: 600;
  list-style: none;
}

.side-menu :deep(.el-menu-item) {
  min-height: 46px;
  margin: 1px 10px;
  padding: 0 12px !important;
  color: rgba(255, 255, 255, 0.68);
  border-radius: 7px;
}

.side-menu :deep(.el-menu-item:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.07);
}

.side-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: rgba(74, 143, 214, 0.24);
  box-shadow: inset 3px 0 0 var(--blue-bright);
}

.side-menu :deep(.el-menu-item .el-icon) {
  width: 20px;
  margin-right: 9px;
  font-size: 17px;
}

.nav-number {
  width: 24px;
  flex: 0 0 24px;
  color: rgba(255, 255, 255, 0.36);
  font-family: var(--display-font);
  font-size: 12px;
}

.side-menu :deep(.el-menu-item.is-active) .nav-number {
  color: var(--blue-bright);
}

.term-panel {
  display: flex;
  min-height: 72px;
  align-items: center;
  gap: 11px;
  padding: 12px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.09);
}

.term-seal {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  font-family: var(--display-font);
  font-size: 16px;
}

.term-copy strong,
.term-copy small {
  display: block;
  white-space: nowrap;
}

.term-copy strong {
  font-size: 12px;
}

.term-copy small {
  color: rgba(255, 255, 255, 0.45);
  font-size: 11px;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  min-height: var(--header-height);
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 22px;
  background: rgba(255, 255, 255, 0.98);
  border-bottom: 1px solid var(--border-light);
}

.topbar-leading,
.desktop-account-actions,
.mobile-account-actions {
  display: flex;
  align-items: center;
}

.topbar-leading {
  min-width: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--text-regular);
  white-space: nowrap;
}

.breadcrumb strong {
  color: var(--ink);
}

.breadcrumb-separator {
  color: var(--line);
}

.menu-toggle,
.mobile-account-actions {
  display: none;
}

.desktop-account-actions {
  justify-content: flex-end;
  gap: 9px;
}

.desktop-account-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.ai-button {
  min-width: 102px;
  background: var(--button-grad) !important;
}

.user-info {
  display: block;
  max-width: 240px;
  margin-left: 2px;
  overflow: hidden;
  color: var(--text-regular);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-info strong {
  color: var(--ink);
}

.role-stamp {
  min-height: 32px;
  padding: 4px 10px;
  color: var(--ink);
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  line-height: 22px;
}

.logout-button {
  color: var(--vermilion);
  border-color: rgba(183, 53, 42, 0.32);
}

.workspace {
  min-height: calc(100vh - var(--header-height));
  padding: 26px;
  background: var(--paper);
}

.password-form {
  margin-top: 20px;
}

.password-form :deep(.el-form-item) {
  margin-bottom: 19px;
}

.nav-backdrop {
  position: fixed;
  inset: 0;
  z-index: 35;
  padding: 0;
  background: rgba(10, 25, 44, 0.46);
  border: 0;
}

.account-trigger {
  display: flex;
  min-width: 64px;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  color: var(--ink);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
  font-weight: 700;
}

.account-trigger > span {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  color: #fff;
  background: var(--ink);
  border-radius: 50%;
  font-family: var(--display-font);
}

@media (max-width: 1180px) {
  .user-info {
    display: none;
  }
}

@media (max-width: 1024px) and (min-width: 769px) {
  .campus-shell {
    padding-left: var(--sidebar-collapsed-width);
  }

  .sidebar {
    width: var(--sidebar-collapsed-width);
  }

  .brand,
  .term-panel {
    justify-content: center;
    padding-inline: 0;
  }

  .side-menu :deep(.el-menu-item) {
    justify-content: center;
    margin-inline: 9px;
    padding: 0 !important;
  }

  .side-menu :deep(.el-menu-item .el-icon) {
    margin: 0;
  }

  .nav-number {
    display: none;
  }

  .desktop-account-actions {
    display: none;
  }

  .mobile-account-actions {
    display: flex;
    gap: 8px;
  }
}

@media (max-width: 768px) {
  .campus-shell {
    padding-left: 0;
  }

  .nav-backdrop {
    left: min(82vw, 292px);
  }

  .sidebar {
    width: min(82vw, 292px);
    transform: translateX(-101%);
    box-shadow: 16px 0 36px rgba(8, 20, 36, 0.22);
  }

  .nav-open .sidebar {
    transform: translateX(0);
  }

  .topbar {
    min-height: 60px;
    gap: 12px;
    padding: 0 14px;
  }

  .menu-toggle {
    display: grid;
    width: 44px;
    height: 44px;
    margin-right: 8px;
    place-items: center;
    color: var(--ink);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
  }

  .breadcrumb > span:first-child,
  .breadcrumb-separator {
    display: none;
  }

  .desktop-account-actions {
    display: none;
  }

  .mobile-account-actions {
    display: flex;
    gap: 8px;
  }

  .mobile-ai-button {
    min-width: 44px;
    min-height: 44px;
  }

  .workspace {
    min-height: calc(100vh - 60px);
    padding: 18px 14px 30px;
  }
}
</style>
