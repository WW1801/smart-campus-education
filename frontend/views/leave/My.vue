<!--
THESIS: 学生在一张连续台账中完成申请并追踪状态，不制造分散卡片。
OWN-WORLD: 继承墨蓝索引、冷灰筛选区、白色台账与清晰状态章。
STORY: 先提交可核对的课程请假，再查看审批与考勤同步结果。
FIRST VIEWPORT: 标题与提交动作在上，状态筛选和申请表紧随其后。
FORM: 既有 Operate 台账结构的窄功能扩展，不改变全局视觉系统。
-->
<template>
  <div class="page-container leave-page">
    <header class="page-header">
      <div class="page-header-left">
        <span class="page-header-icon"><el-icon><Document /></el-icon></span>
        <div>
          <h1 class="page-header-title">我的请假</h1>
          <p class="page-header-desc">提交课程请假，查看审批与考勤同步状态</p>
        </div>
      </div>
      <el-button type="primary" @click="openCreate">提交请假</el-button>
    </header>

    <el-card>
      <div class="search-bar" role="search" aria-label="请假申请筛选">
        <el-select v-model="filters.status" clearable placeholder="全部状态" aria-label="申请状态" @change="loadData(1)">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button :loading="loading" @click="loadData(1)">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" empty-text="暂无请假申请" row-key="requestId">
        <el-table-column prop="courseName" label="课程" min-width="150">
          <template #default="{ row }"><strong>{{ row.courseName || row.courseId }}</strong></template>
        </el-table-column>
        <el-table-column prop="semesterName" label="学期" min-width="170" />
        <el-table-column label="请假日期" min-width="190">
          <template #default="{ row }">{{ row.startDate }} 至 {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column prop="leaveType" label="类型" width="90">
          <template #default="{ row }">{{ leaveTypeLabel(row.leaveType) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="processOpinion" label="审批意见" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.processOpinion || '—' }}</template>
        </el-table-column>
        <el-table-column label="审批状态" width="110">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="考勤同步" min-width="150">
          <template #default="{ row }">
            <span>{{ syncLabel(row.attendanceSyncStatus) }}</span>
            <p v-if="row.syncFailureReason" class="failure-copy">{{ row.syncFailureReason }}</p>
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" min-width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" link type="danger" :loading="cancellingId === row.requestId" :disabled="!!cancellingId" @click="cancelRequest(row)">撤销</el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.limit"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="createVisible" title="提交请假申请" width="560px" :close-on-click-modal="false">
      <el-alert title="审批通过后，系统会按日期自动同步为“请假”考勤。" type="info" :closable="false" show-icon />
      <el-alert v-if="semesterNotice" :title="semesterNotice" type="warning" :closable="false" show-icon class="form-alert" />
      <el-alert v-if="submitError" :title="submitError" type="error" :closable="false" show-icon class="form-alert" />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="leave-form">
        <div class="form-grid">
          <el-form-item label="学期" prop="semesterId">
            <el-select v-model="form.semesterId" placeholder="请选择学期" @change="handleSemesterChange">
              <el-option v-for="item in semesters" :key="item.semesterId" :label="item.name" :value="item.semesterId" />
            </el-select>
          </el-form-item>
          <el-form-item label="课程" prop="courseId">
            <el-select v-model="form.courseId" filterable :loading="optionsLoading" :disabled="!form.semesterId || optionsLoading" :placeholder="coursePlaceholder">
              <el-option v-for="item in courses" :key="item.courseId" :label="`${item.code ? `${item.code} · ` : ''}${item.name}`" :value="item.courseId" />
            </el-select>
          </el-form-item>
        </div>
        <el-alert v-if="optionsError" :title="optionsError" type="warning" :closable="false" show-icon class="form-alert compact-alert" />
        <div class="form-grid">
          <el-form-item label="请假日期" prop="dateRange">
            <el-date-picker v-model="form.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" :disabled="!selectedSemester" :disabled-date="isDateDisabled" />
            <p v-if="selectedSemester" class="form-hint">可申请日期：{{ selectedSemester.startDate }} 至 {{ selectedSemester.endDate }}</p>
          </el-form-item>
          <el-form-item label="请假类型" prop="leaveType">
            <el-select v-model="form.leaveType" placeholder="请选择类型">
              <el-option v-for="item in leaveTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="请假原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="请说明请假原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="optionsLoading || !courses.length" @click="submitRequest">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useStore } from 'vuex'
import request from '../../utils/request'

const store = useStore()
const loading = ref(false)
const submitting = ref(false)
const optionsLoading = ref(false)
const cancellingId = ref('')
const createVisible = ref(false)
const submitError = ref('')
const optionsError = ref('')
const formRef = ref(null)
const rows = ref([])
const courses = ref([])
const courseCatalog = ref([])
const semesters = ref([])
const filters = reactive({ status: '' })
const pagination = reactive({ page: 1, limit: 10, total: 0 })
const form = reactive({ courseId: '', semesterId: '', dateRange: [], leaveType: '', reason: '' })

const statusOptions = [
  { value: 'pending', label: '待审批' }, { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已拒绝' }, { value: 'cancelled', label: '已撤销' }
]
const leaveTypes = [
  { value: 'sick', label: '病假' }, { value: 'personal', label: '事假' },
  { value: 'official', label: '公假' }, { value: 'other', label: '其他' }
]
const selectedSemester = computed(() => semesters.value.find(item => item.semesterId === form.semesterId))
const semesterNotice = computed(() => {
  if (!semesters.value.length) return '系统尚未配置学期，暂时不能提交请假申请。'
  const today = toLocalDateString(new Date())
  const coversToday = semesters.value.some(item => item.startDate <= today && item.endDate >= today)
  return coversToday ? '' : '系统没有覆盖今天的学期；如需申请当前日期，请联系教务管理员更新学期与排课。'
})
const coursePlaceholder = computed(() => {
  if (!form.semesterId) return '请先选择学期'
  if (optionsLoading.value) return '正在读取个人课表'
  return courses.value.length ? '请选择课程' : '该学期没有可申请课程'
})

const toLocalDateString = date => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const isDateDisabled = date => {
  const semester = selectedSemester.value
  if (!semester?.startDate || !semester?.endDate) return true
  const value = toLocalDateString(date)
  return value < semester.startDate || value > semester.endDate
}

const validateDateRange = (rule, value, callback) => {
  if (!Array.isArray(value) || value.length !== 2) return callback(new Error('请选择请假日期'))
  const semester = selectedSemester.value
  if (!semester?.startDate || !semester?.endDate) return callback(new Error('请先选择有效学期'))
  if (value[0] < semester.startDate || value[1] > semester.endDate) {
    return callback(new Error(`请假日期必须在 ${semester.startDate} 至 ${semester.endDate} 之间`))
  }
  callback()
}

const rules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  dateRange: [{ validator: validateDateRange, trigger: 'change' }],
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  reason: [{ required: true, message: '请填写请假原因', trigger: 'blur' }]
}

const statusLabel = value => statusOptions.find(item => item.value === value)?.label || value
const statusType = value => ({ approved: 'success', rejected: 'danger', cancelled: 'info', pending: '' }[value] ?? 'info')
const leaveTypeLabel = value => leaveTypes.find(item => item.value === value)?.label || value
const syncLabel = value => ({ pending: '待同步', synced: '已同步考勤', failed: '同步失败', not_required: '无需同步' }[value] || value || '—')

const loadCourses = async semesterId => {
  courses.value = []
  optionsError.value = ''
  if (!semesterId) return
  optionsLoading.value = true
  try {
    const res = await request.get('/selection/timetable', {
      params: { studentId: store.state.user?.relatedId, semesterId },
      skipErrorMessage: true
    })
    const catalog = new Map(courseCatalog.value.map(item => [item.courseId, item]))
    const eligible = new Map()
    for (const item of res.data || []) {
      const details = catalog.get(item.courseId)
      if (!eligible.has(item.courseId)) {
        eligible.set(item.courseId, {
          courseId: item.courseId,
          code: details?.code || '',
          name: item.courseName || details?.name || item.courseId
        })
      }
    }
    courses.value = [...eligible.values()]
    if (!courses.value.length) optionsError.value = '所选学期没有你的排课或有效选课，暂时不能提交请假申请。'
  } catch (error) {
    optionsError.value = error?.message || '个人课表读取失败，请稍后重试。'
  } finally {
    optionsLoading.value = false
  }
}

const loadOptions = async () => {
  const [courseRes, semesterRes] = await Promise.all([
    request.get('/course/list').catch(() => ({ data: [] })),
    request.get('/semester/list').catch(() => ({ data: [] }))
  ])
  courseCatalog.value = courseRes.data || []
  semesters.value = semesterRes.data || []
  if (!form.semesterId) {
    const today = toLocalDateString(new Date())
    const activeByDate = semesters.value.find(item => item.startDate <= today && item.endDate >= today)
    form.semesterId = activeByDate?.semesterId || semesters.value.find(item => item.status === 'current')?.semesterId || ''
  }
  await loadCourses(form.semesterId)
}

const loadData = async (page = pagination.page) => {
  pagination.page = page
  loading.value = true
  try {
    const res = await request.get('/leave-requests/my', { params: { page, limit: pagination.limit, status: filters.status || undefined } })
    rows.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const openCreate = async () => {
  Object.assign(form, { courseId: '', dateRange: [], leaveType: '', reason: '' })
  submitError.value = ''
  formRef.value?.clearValidate()
  if (!semesters.value.length) await loadOptions()
  else await loadCourses(form.semesterId)
  createVisible.value = true
}

const handleSemesterChange = async semesterId => {
  form.courseId = ''
  form.dateRange = []
  submitError.value = ''
  await loadCourses(semesterId)
  formRef.value?.clearValidate(['courseId', 'dateRange'])
}

const submitRequest = async () => {
  submitError.value = ''
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  try {
    await request.post('/leave-requests', {
      courseId: form.courseId, semesterId: form.semesterId,
      startDate: form.dateRange[0], endDate: form.dateRange[1],
      leaveType: form.leaveType, reason: form.reason
    }, { skipErrorMessage: true })
    ElMessage.success('请假申请已提交')
    createVisible.value = false
    await loadData(1)
  } catch (error) {
    submitError.value = error?.message || '提交失败，已保留填写内容，请核对后重试。'
    ElMessage.error(submitError.value)
  } finally { submitting.value = false }
}

const cancelRequest = async row => {
  try { await ElMessageBox.confirm('撤销后不能恢复，确认撤销这条待审批申请？', '撤销请假', { type: 'warning' }) } catch { return }
  cancellingId.value = row.requestId
  try {
    await request.put(`/leave-requests/${row.requestId}/cancel`)
    ElMessage.success('请假申请已撤销')
    await loadData()
  } finally { cancellingId.value = '' }
}

onMounted(() => Promise.all([loadOptions(), loadData(1)]))
</script>

<style scoped>
.leave-page { width: 100%; }
.page-header-title, .page-header-desc { margin: 0; }
.search-bar .el-select { width: 190px; }
.leave-form { margin-top: 20px; }
.form-alert { margin-top: 14px; }
.compact-alert { margin: 0 0 16px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-grid :deep(.el-select), .form-grid :deep(.el-date-editor) { width: 100%; }
.form-hint { width: 100%; margin: 6px 0 0; color: var(--text-muted); font-size: 12px; line-height: 1.5; }
.muted { color: var(--text-muted); }
.failure-copy { margin: 5px 0 0; color: var(--vermilion); font-size: 12px; line-height: 1.45; overflow-wrap: anywhere; }
@media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; gap: 0; } }
</style>
