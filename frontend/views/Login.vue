<!-- 登录页面组件，负责用户登录入口与身份认证交互。 -->
<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
      <div class="bg-shape shape-3"></div>
    </div>
    <div class="login-wrapper">
      <div class="login-left">
        <div class="brand-content">
          <div class="brand-icon">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
            </svg>
          </div>
          <h1 class="brand-title">校园教务系统</h1>
          <p class="brand-desc">智慧校园 · 高效管理 · 便捷服务</p>
          <div class="brand-features">
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>全方位教务管理</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>智能排课与冲突检测</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>在线选课与成绩管理</span>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="login-card">
          <div class="login-header">
            <h2>欢迎登录</h2>
            <p>请输入您的账号信息</p>
          </div>
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                size="large"
                @keyup.enter="login"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                show-password
                @keyup.enter="login"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                @click="login"
                :loading="loginLoading"
                class="login-btn"
              >
                {{ loginLoading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const store = useStore()
const loginLoading = ref(false)
const loginForm = ref({ username: '', password: '' })
const loginFormRef = ref(null)

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 获取默认首页路径
const getDefaultHomePath = (roleId) => {
  const map = {
    '1': '/home/system/user',
    '2': '/home/student/info',
    '3': '/home/student/info',
    '4': '/home/grade/query',
    '5': '/home/selection'
  }
  return map[roleId] || '/home'
}

// 处理登录
const login = async () => {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch { return }

  loginLoading.value = true
  try {
    const res = await request.post('/login', loginForm.value)
    const { token, user } = res.data
    store.dispatch('login', { user, token })
    ElMessage.success('登录成功')
    router.push(getDefaultHomePath(user?.roleId))
  } catch {
  } finally {
    loginLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
  background: #fff;
}

.shape-1 {
  width: 600px;
  height: 600px;
  top: -200px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.shape-2 {
  width: 400px;
  height: 400px;
  bottom: -150px;
  left: -100px;
  animation: float 6s ease-in-out infinite reverse;
}

.shape-3 {
  width: 300px;
  height: 300px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float 10s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-30px); }
}

.login-wrapper {
  display: flex;
  width: 900px;
  min-height: 500px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
  backdrop-filter: blur(20px);
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #4361ee 0%, #6b83f2 50%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  position: relative;
  overflow: hidden;
}

.login-left::before {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  top: -80px;
  right: -80px;
}

.login-left::after {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  bottom: -60px;
  left: -60px;
}

.brand-content {
  position: relative;
  z-index: 1;
  color: #fff;
  text-align: center;
}

.brand-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 12px;
  letter-spacing: 2px;
}

.brand-desc {
  font-size: 14px;
  opacity: 0.85;
  margin-bottom: 40px;
  letter-spacing: 1px;
}

.brand-features {
  text-align: left;
  display: inline-block;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 14px;
  opacity: 0.9;
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2ec4b6;
  flex-shrink: 0;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 50px;
}

.login-card {
  width: 100%;
  max-width: 360px;
}

.login-header {
  margin-bottom: 36px;
}

.login-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: var(--text-secondary);
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px !important;
  padding: 4px 12px !important;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
  transition: all 0.3s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary-color) inset !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary-color) inset, 0 0 0 3px rgba(67, 97, 238, 0.1) !important;
}

.login-form :deep(.el-input__prefix .el-icon) {
  color: var(--text-secondary);
  font-size: 18px;
}

.login-btn {
  width: 100%;
  height: 46px;
  border-radius: 10px !important;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light)) !important;
  border: none !important;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(67, 97, 238, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
    width: 90%;
    min-height: auto;
  }
  .login-left {
    padding: 40px 30px;
  }
  .login-right {
    padding: 40px 30px;
  }
}
</style>
