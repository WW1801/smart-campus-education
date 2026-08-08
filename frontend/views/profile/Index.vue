<!-- 个人中心页面：仅展示当前账号与角色关联档案，不暴露密码、Token 或 Cookie。 -->
<template>
  <div class="profile-page">
    <div class="profile-header">
      <div>
        <div class="page-header-title">个人中心</div>
        <div class="page-header-desc">查看当前登录用户信息、关联档案并修改密码</div>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadProfile">刷新</el-button>
        <el-button class="logout-button" @click="handleLogout">退出登录</el-button>
      </div>
    </div>

    <PageErrorState v-if="error" :retrying="loading" title="个人信息加载失败" @retry="loadProfile" />

    <template v-else>
      <section class="profile-ledger" v-loading="loading">
        <div class="identity-block">
          <span class="identity-mark">{{ accountInitial }}</span>
          <div>
            <h1>{{ displayName }}</h1>
            <p>{{ account.username || '账号未设置' }} · {{ account.roleName || roleName }}</p>
          </div>
        </div>
        <div class="ledger-grid">
          <div class="ledger-item">
            <span>账号 ID</span>
            <strong>{{ account.userId || '-' }}</strong>
          </div>
          <div class="ledger-item">
            <span>角色</span>
            <strong>{{ account.roleName || roleName }}</strong>
          </div>
          <div class="ledger-item">
            <span>关联类型</span>
            <strong>{{ profileTypeText }}</strong>
          </div>
          <div class="ledger-item">
            <span>最近登录</span>
            <strong>{{ account.lastLogin || '-' }}</strong>
          </div>
        </div>
      </section>

      <div class="profile-columns">
        <section class="profile-section">
          <div class="section-heading">
            <h2>关联信息</h2>
            <span>{{ profileStatusText }}</span>
          </div>
          <dl class="detail-list">
            <template v-for="item in roleDetails" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value || '-' }}</dd>
            </template>
          </dl>
        </section>

        <section class="profile-section">
          <div class="section-heading">
            <h2>修改密码</h2>
            <span>8-64 位，包含字母和数字</span>
          </div>
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" @keyup.enter="submitPasswordChange" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="submitPasswordChange">确认修改</el-button>
          </el-form>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageErrorState from '../../components/PageErrorState.vue'
import appData from '../../config/appData.json'
import request from '../../utils/request'

const router = useRouter()
const store = useStore()
const loading = ref(false)
const error = ref(false)
const saving = ref(false)
const account = ref({})
const profile = ref({})
const profileType = ref('')
const passwordFormRef = ref(null)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const roleName = computed(() => appData.roles.names[store.state.user?.roleId] || '未知角色')
const displayName = computed(() => profile.value.name || account.value.name || account.value.username || '-')
const accountInitial = computed(() => displayName.value.slice(0, 1))
const profileTypeText = computed(() => ({ student: '学生档案', teacher: '教师档案', admin: '管理员账号' }[profileType.value] || '账号信息'))
const profileStatusText = computed(() => {
  if (profile.value.status === 'unbound') return '未绑定关联档案'
  return profile.value.status || 'active'
})

const roleDetails = computed(() => {
  if (profileType.value === 'student') {
    return [
      { label: '姓名', value: profile.value.name },
      { label: '学号', value: profile.value.studentNo },
      { label: '班级', value: profile.value.className },
      { label: '专业', value: profile.value.majorName },
      { label: '院系', value: profile.value.departmentName },
      { label: '手机号', value: profile.value.phone },
      { label: '邮箱', value: profile.value.email }
    ]
  }
  if (profileType.value === 'teacher') {
    return [
      { label: '姓名', value: profile.value.name },
      { label: '工号', value: profile.value.teacherId },
      { label: '院系', value: profile.value.departmentName },
      { label: '职称', value: profile.value.title },
      { label: '手机号', value: profile.value.phone },
      { label: '邮箱', value: profile.value.email }
    ]
  }
  return [
    { label: '账号', value: account.value.username },
    { label: '姓名', value: account.value.name },
    { label: '角色', value: account.value.roleName || roleName.value },
    { label: '状态', value: profile.value.status || 'active' }
  ]
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入新密码'))
  if (value !== passwordForm.newPassword) return callback(new Error('两次输入的新密码不一致'))
  callback()
}

const validatePasswordComplexity = (rule, value, callback) => {
  if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) return callback(new Error('新密码需同时包含字母和数字'))
  callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '新密码长度应为 8 至 64 位', trigger: 'blur' },
    { validator: validatePasswordComplexity, trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const loadProfile = async () => {
  loading.value = true
  error.value = false
  try {
    const res = await request.get('/account/me', { skipErrorMessage: true })
    account.value = res.data?.user || {}
    profile.value = res.data?.profile || {}
    profileType.value = res.data?.profileType || ''
  } catch {
    account.value = {}
    profile.value = {}
    profileType.value = ''
    error.value = true
  } finally {
    loading.value = false
  }
}

const submitPasswordChange = async () => {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const res = await request.put('/account/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success(res.message || '密码修改成功')
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    passwordFormRef.value?.clearValidate()
  } finally {
    saving.value = false
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确认退出当前账号？', '退出登录', {
      confirmButtonText: '退出登录',
      cancelButtonText: '继续使用',
      closeOnClickModal: false,
      type: 'warning'
    })
  } catch {
    return
  }
  await store.dispatch('logout')
  await router.push('/')
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  width: 100%;
}

.profile-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.header-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.logout-button {
  color: var(--vermilion);
  border-color: rgba(183, 53, 42, 0.32);
}

.profile-ledger,
.profile-section {
  background: var(--surface);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}

.profile-ledger {
  padding: 24px;
}

.identity-block {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}

.identity-mark {
  display: grid;
  width: 58px;
  height: 58px;
  flex: 0 0 58px;
  place-items: center;
  color: #fff;
  background: var(--ink);
  border-radius: 12px;
  font-family: var(--display-font);
  font-size: 28px;
  font-weight: 800;
}

.identity-block h1 {
  margin: 0;
  color: var(--ink);
  font-size: 28px;
  line-height: 1.2;
}

.identity-block p {
  margin: 6px 0 0;
  color: var(--text-secondary);
}

.ledger-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  padding-top: 18px;
}

.ledger-item span,
.ledger-item strong {
  display: block;
}

.ledger-item span {
  color: var(--text-muted);
  font-size: 13px;
}

.ledger-item strong {
  min-width: 0;
  margin-top: 5px;
  overflow-wrap: anywhere;
  color: var(--ink);
}

.profile-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 420px);
  gap: 18px;
  margin-top: 18px;
}

.profile-section {
  padding: 22px;
}

.section-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.section-heading h2 {
  margin: 0;
  color: var(--ink);
  font-size: 20px;
}

.section-heading span {
  color: var(--text-muted);
  font-size: 13px;
}

.detail-list {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 12px 18px;
  margin: 0;
}

.detail-list dt {
  color: var(--text-muted);
}

.detail-list dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--ink);
  font-weight: 700;
}

@media (max-width: 920px) {
  .profile-header,
  .section-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .ledger-grid,
  .profile-columns {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .profile-ledger,
  .profile-section {
    padding: 18px;
  }

  .identity-block {
    align-items: flex-start;
  }

  .identity-block h1 {
    font-size: 23px;
  }

  .detail-list {
    grid-template-columns: 1fr;
    gap: 4px 0;
  }

  .detail-list dd {
    margin-bottom: 10px;
  }
}
</style>
