<!-- 系统管理用户管理页面组件，负责处理系统管理模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><User /></el-icon>
        </div>
        <div>
          <div class="page-header-title">用户管理</div>
          <div class="page-header-desc">管理系统用户账号与角色分配</div>
        </div>
      </div>
      <el-button type="primary" @click="openDialog(null)">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
      <el-button @click="provisionDialogVisible = true">批量开通账号</el-button>
    </div>

    <PageErrorState v-if="listError" :retrying="loading" title="用户列表加载失败" @retry="loadData" />
    <el-card v-else>
      <div class="search-bar">
        <el-input v-model="searchForm.username" placeholder="搜索用户名" style="width: 180px" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.roleId" placeholder="筛选角色" clearable style="width: 150px">
          <el-option v-for="r in roleList" :key="r.roleId" :label="r.name" :value="r.roleId" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="userId" label="用户ID" width="120" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="roleId" label="角色" width="120">
          <template #default="{ row }">
            <el-tag>{{ getRoleName(row.roleId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="relatedId" label="关联ID" width="120" />
        <el-table-column prop="lastLogin" label="最后登录" width="170" />
        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" link @click="resetPassword(row)">重置密码</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :total="page.total"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
      />
    </el-card>

    <Transition name="reset-feedback">
      <div v-if="resetFeedback" class="reset-feedback" role="status" aria-live="polite">
        <el-icon class="reset-feedback-icon"><CircleCheckFilled /></el-icon>
        <div>
          <div class="reset-feedback-title">密码已重置</div>
          <div class="reset-feedback-desc">已设置为系统初始密码</div>
        </div>
      </div>
    </Transition>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.userId || isBusinessAccount" :placeholder="accountPlaceholder" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.userId">
          <el-input v-model="form.password" type="password" placeholder="默认123456" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" :disabled="isBusinessAccount" :placeholder="isBusinessAccount ? '将从关联人员信息自动带出' : '请输入姓名'" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" style="width: 100%" placeholder="请选择角色" @change="handleRoleChange">
            <el-option v-for="r in roleList" :key="r.roleId" :label="r.name" :value="r.roleId" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联ID" prop="relatedId">
          <el-input v-model="form.relatedId" :placeholder="relatedIdPlaceholder" />
        </el-form-item>
        <el-alert
          v-if="isBusinessAccount"
          type="info"
          :closable="false"
          :title="businessAccountTip"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="provisionDialogVisible" title="批量开通缺失账号" width="680px">
      <el-alert type="info" :closable="false" title="只为尚未开通账号的人员创建账号；已有账号不会被覆盖。" />
      <el-form :model="provisionForm" label-width="90px" style="margin-top: 16px">
        <el-form-item label="人员类型">
          <el-radio-group v-model="provisionForm.personType"><el-radio label="teacher">教师</el-radio><el-radio label="student">学生</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="院系 ID"><el-input v-model="provisionForm.departmentId" clearable placeholder="不填则包含全部院系" /></el-form-item>
        <el-form-item v-if="provisionForm.personType === 'student'" label="班级 ID"><el-input v-model="provisionForm.classId" clearable placeholder="不填则包含全部班级" /></el-form-item>
      </el-form>
      <el-table v-if="provisionResult" :data="provisionResult.failureDetails" max-height="240" empty-text="没有失败人员">
        <el-table-column prop="personId" label="人员编号" /><el-table-column prop="name" label="姓名" />
        <el-table-column prop="reason" label="失败原因" /><el-table-column prop="suggestion" label="处理建议" />
      </el-table>
      <template #footer>
        <span v-if="provisionResult" class="provision-summary">成功 {{ provisionResult.successCount }} 人，失败 {{ provisionResult.failedCount }} 人</span>
        <el-button @click="provisionDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="provisioning" @click="batchProvision">{{ provisionResult?.failedCount ? '重试失败人员' : '开始开通' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Plus, Search, CircleCheckFilled } from '@element-plus/icons-vue'

const loading = ref(false)
const listError = ref(false)
const tableData = ref([])
const roleList = ref([])
const dialogVisible = ref(false)
const provisionDialogVisible = ref(false)
const provisioning = ref(false)
const provisionResult = ref(null)
const formRef = ref(null)
const resetFeedback = ref(false)
let resetFeedbackTimer = null

const searchForm = reactive({ username: '', roleId: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const form = reactive({
  userId: '', username: '', password: '', name: '', roleId: '', relatedId: ''
})

const rules = {
  username: [{ validator: validateUsername, trigger: 'blur' }],
  name: [{ validator: validateName, trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  relatedId: [{ validator: validateRelatedId, trigger: 'blur' }]
}

const dialogTitle = ref('新增用户')
const isBusinessAccount = computed(() => form.roleId === '4' || form.roleId === '5')
const accountPlaceholder = computed(() => {
  if (form.roleId === '4') return '将自动使用关联教师的工号'
  if (form.roleId === '5') return '将自动使用关联学生的学号'
  return '请输入登录账号'
})
const provisionForm = reactive({ personType: 'teacher', departmentId: '', classId: '' })
const relatedIdPlaceholder = computed(() => {
  if (form.roleId === '4') return '请输入教师工号，例如 T001'
  if (form.roleId === '5') return '请输入学生ID，例如 S001'
  return '教师或学生账号请填写关联ID'
})
const businessAccountTip = computed(() => form.roleId === '4'
  ? '教师使用工号登录，账号和姓名将根据关联教师自动生成。'
  : '学生使用学号登录，账号和姓名将根据关联学生自动生成。')

function validateUsername(rule, value, callback) {
  if (isBusinessAccount.value || (value && value.trim())) return callback()
  callback(new Error('请输入登录账号'))
}

function validateName(rule, value, callback) {
  if (isBusinessAccount.value || (value && value.trim())) return callback()
  callback(new Error('请输入姓名'))
}

function validateRelatedId(rule, value, callback) {
  if (!isBusinessAccount.value || (value && value.trim())) return callback()
  callback(new Error(form.roleId === '4' ? '请输入教师工号' : '请输入学生ID'))
}

// 获取角色name
const getRoleName = (roleId) => {
  const role = roleList.value.find(r => r.roleId === roleId)
  return role ? role.name : roleId
}

// 重置查询条件
const resetSearch = () => {
  searchForm.username = ''
  searchForm.roleId = ''
  page.current = 1
  loadData()
}

// 加载角色
const loadRoles = async () => {
  const res = await request.get('/system/role/list')
  roleList.value = res.data
}

// 加载数据
const loadData = async () => {
  loading.value = true
  listError.value = false
  try {
    const res = await request.get('/system/user/page', {
      params: { current: page.current, size: page.size, ...searchForm },
      skipErrorMessage: true
    })
    tableData.value = res.data.records
    page.total = res.data.total
  } catch {
    tableData.value = []
    page.total = 0
    listError.value = true
  } finally {
    loading.value = false
  }
}

// 处理opendialog
const openDialog = (row) => {
  dialogTitle.value = row ? '编辑用户' : '新增用户'
  Object.assign(form, row || { userId: '', username: '', password: '', name: '', roleId: '', relatedId: '' })
  dialogVisible.value = true
}

// 切换为师生角色时清空手填账号，提交后由后端根据关联人员信息统一生成。
const handleRoleChange = () => {
  if (isBusinessAccount.value) {
    form.username = ''
    form.name = ''
  }
}

// 处理提交
const handleSubmit = async () => {
  await formRef.value.validate()
  if (form.userId) {
    await request.put('/system/user', form)
  } else {
    await request.post('/system/user', form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

const batchProvision = async () => {
  provisioning.value = true
  try {
    const res = await request.post('/system/account-provisioning/batch', provisionForm)
    provisionResult.value = res.data
    const { successCount, failedCount } = res.data
    ElMessage[failedCount ? (successCount ? 'warning' : 'error') : 'success'](failedCount ? `已开通 ${successCount} 人，${failedCount} 人需要处理后重试` : `已成功开通 ${successCount} 人账号`)
    loadData()
  } finally { provisioning.value = false }
}

// 处理删除
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
  await request.delete(`/system/user/${row.userId}`)
  ElMessage.success('删除成功')
  loadData()
}

// 重置密码
const resetPassword = async (row) => {
  await ElMessageBox.confirm('确认重置密码为默认密码？', '提示', { type: 'warning' })
  await request.put(`/system/user/${row.userId}/reset-password`)
  resetFeedback.value = true
  if (resetFeedbackTimer) clearTimeout(resetFeedbackTimer)
  resetFeedbackTimer = setTimeout(() => {
    resetFeedback.value = false
    resetFeedbackTimer = null
  }, 1600)
}

onBeforeUnmount(() => {
  if (resetFeedbackTimer) clearTimeout(resetFeedbackTimer)
})

// 页面挂载时初始化用户数据
onMounted(() => {
  loadRoles()
  loadData()
})
</script>

<style scoped>
.page-container {
  width: 100%;
}
.provision-summary { margin-right: 16px; color: var(--el-text-color-regular); }
.reset-feedback {
  position: fixed;
  top: 50%;
  left: 50%;
  z-index: 3000;
  display: flex;
  align-items: center;
  min-width: 250px;
  padding: 18px 24px;
  color: #fff;
  background: rgba(31, 41, 55, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.24);
  transform: translate(-50%, -50%);
}
.reset-feedback-icon {
  margin-right: 12px;
  color: #67c23a;
  font-size: 28px;
}
.reset-feedback-title {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.4;
}
.reset-feedback-desc {
  margin-top: 3px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  line-height: 1.4;
}
.reset-feedback-enter-active,
.reset-feedback-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.reset-feedback-enter-from,
.reset-feedback-leave-to {
  opacity: 0;
  transform: translate(-50%, -44%);
}
</style>
