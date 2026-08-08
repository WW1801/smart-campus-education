<!--
THESIS: 审批台先暴露待处理与同步冲突，让管理员在同一张台账完成批量决策。
OWN-WORLD: 冷灰筛选带、白色连续表格、墨蓝主动作、朱红仅用于真实失败。
STORY: 筛选申请，核对详情，批准或拒绝，并当场处理逐日同步失败。
FIRST VIEWPORT: 标题、批量审批按钮、完整筛选带和申请台账连续出现。
FORM: 既有 Operate 台账结构的审批变体，保持权限路由与 Element Plus 交互。
-->
<template>
  <div class="page-container approval-page">
    <header class="page-header">
      <div class="page-header-left">
        <span class="page-header-icon"><el-icon><Checked /></el-icon></span>
        <div>
          <h1 class="page-header-title">请假审批</h1>
          <p class="page-header-desc">核对申请并将通过结果同步到考勤台账</p>
        </div>
      </div>
      <el-button type="primary" :disabled="!selectedRows.length" @click="openBatchApproval">
        批量审批{{ selectedRows.length ? `（${selectedRows.length}）` : '' }}
      </el-button>
    </header>

    <el-card>
      <div class="search-bar" role="search" aria-label="请假审批筛选">
        <el-input v-model="filters.studentId" clearable placeholder="学生ID" aria-label="学生ID" />
        <el-select v-model="filters.courseId" clearable filterable placeholder="全部课程" aria-label="课程">
          <el-option v-for="item in courses" :key="item.courseId" :label="item.name" :value="item.courseId" />
        </el-select>
        <el-select v-model="filters.semesterId" clearable placeholder="全部学期" aria-label="学期">
          <el-option v-for="item in semesters" :key="item.semesterId" :label="item.name" :value="item.semesterId" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="全部状态" aria-label="状态">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker v-model="filters.dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" aria-label="请假日期范围" />
        <el-button type="primary" :loading="loading" @click="loadData(1)">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        row-key="requestId"
        empty-text="暂无匹配的请假申请"
        @selection-change="selectedRows = $event"
      >
        <el-table-column type="selection" width="48" :selectable="row => row.status === 'pending'" />
        <el-table-column label="学生" min-width="145">
          <template #default="{ row }"><strong>{{ row.studentName || row.studentId }}</strong><small>{{ row.studentNo || row.studentId }}</small></template>
        </el-table-column>
        <el-table-column label="课程 / 学期" min-width="190">
          <template #default="{ row }"><strong>{{ row.courseName || row.courseId }}</strong><small>{{ row.semesterName || row.semesterId }}</small></template>
        </el-table-column>
        <el-table-column label="请假日期" min-width="185">
          <template #default="{ row }">{{ row.startDate }} 至 {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="同步结果" min-width="190">
          <template #default="{ row }">
            <el-tag :type="syncType(row.attendanceSyncStatus)" effect="plain">{{ syncLabel(row.attendanceSyncStatus) }}</el-tag>
            <p v-if="row.syncFailureReason" class="failure-copy">{{ row.syncFailureReason }}</p>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link @click="showDetail(row)">详情</el-button>
            <template v-if="row.status === 'pending'">
              <el-button link type="primary" @click="openApproval(row)">通过</el-button>
              <el-button link type="danger" @click="openReject(row)">拒绝</el-button>
            </template>
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

    <el-dialog v-model="detailVisible" title="请假申请详情" width="620px">
      <el-descriptions v-if="activeRow" :column="2" border>
        <el-descriptions-item label="申请单ID">{{ activeRow.requestId }}</el-descriptions-item>
        <el-descriptions-item label="学生">{{ activeRow.studentName }}（{{ activeRow.studentNo || activeRow.studentId }}）</el-descriptions-item>
        <el-descriptions-item label="课程">{{ activeRow.courseName || activeRow.courseId }}</el-descriptions-item>
        <el-descriptions-item label="学期">{{ activeRow.semesterName || activeRow.semesterId }}</el-descriptions-item>
        <el-descriptions-item label="日期" :span="2">{{ activeRow.startDate }} 至 {{ activeRow.endDate }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ leaveTypeLabel(activeRow.leaveType) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(activeRow.status) }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ activeRow.submittedAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="同步状态">{{ syncLabel(activeRow.attendanceSyncStatus) }}</el-descriptions-item>
        <el-descriptions-item label="原因" :span="2">{{ activeRow.reason }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ activeRow.processedByName || activeRow.processedBy || '—' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ activeRow.processedAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">{{ activeRow.processOpinion || '—' }}</el-descriptions-item>
        <el-descriptions-item label="同步失败" :span="2">{{ activeRow.syncFailureReason || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="processVisible" :title="processMode === 'reject' ? '拒绝请假申请' : processMode === 'batch' ? '批量通过请假申请' : '通过请假申请'" width="560px" :close-on-click-modal="false">
      <el-alert
        v-if="processMode !== 'reject'"
        title="系统会先检查全部日期；人工锁定或已有非请假考勤时不会覆盖。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-input v-model="processOpinion" class="opinion-input" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="审批意见（选填）" />

      <section v-if="failureRows.length" class="sync-failures" aria-live="polite">
        <h2>未完成同步的记录</h2>
        <el-table :data="failureRows" size="small">
          <el-table-column prop="requestId" label="申请单" min-width="110" />
          <el-table-column prop="date" label="日期" width="110" />
          <el-table-column prop="reason" label="原因" min-width="180" />
          <el-table-column prop="suggestion" label="建议" min-width="200" />
        </el-table>
      </section>
      <template #footer>
        <el-button @click="processVisible = false">关闭</el-button>
        <el-button :type="processMode === 'reject' ? 'danger' : 'primary'" :loading="processing" :disabled="processMode === 'batch' && !selectedRows.length" @click="submitProcess">
          {{ processMode === 'reject' ? '确认拒绝' : '确认通过' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Checked } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const processing = ref(false)
const rows = ref([])
const courses = ref([])
const semesters = ref([])
const selectedRows = ref([])
const activeRow = ref(null)
const detailVisible = ref(false)
const processVisible = ref(false)
const processMode = ref('approve')
const processOpinion = ref('')
const failureRows = ref([])
const pagination = reactive({ page: 1, limit: 10, total: 0 })
const filters = reactive({ studentId: '', courseId: '', semesterId: '', status: 'pending', dateRange: [] })

const statusOptions = [
  { value: 'pending', label: '待审批' }, { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已拒绝' }, { value: 'cancelled', label: '已撤销' }
]
const statusLabel = value => statusOptions.find(item => item.value === value)?.label || value
const statusType = value => ({ approved: 'success', rejected: 'danger', cancelled: 'info', pending: '' }[value] ?? 'info')
const syncLabel = value => ({ pending: '待同步', synced: '已同步', failed: '同步失败', not_required: '无需同步' }[value] || value || '—')
const syncType = value => ({ synced: 'success', failed: 'danger', not_required: 'info', pending: 'warning' }[value] || 'info')
const leaveTypeLabel = value => ({ sick: '病假', personal: '事假', official: '公假', other: '其他' }[value] || value)

const loadOptions = async () => {
  const [courseRes, semesterRes] = await Promise.all([
    request.get('/course/list').catch(() => ({ data: [] })),
    request.get('/semester/list').catch(() => ({ data: [] }))
  ])
  courses.value = courseRes.data || []
  semesters.value = semesterRes.data || []
}

const loadData = async (page = pagination.page) => {
  pagination.page = page
  loading.value = true
  try {
    const res = await request.get('/leave-requests/page', { params: {
      page, limit: pagination.limit,
      studentId: filters.studentId || undefined, courseId: filters.courseId || undefined,
      semesterId: filters.semesterId || undefined, status: filters.status || undefined,
      startDate: filters.dateRange?.[0], endDate: filters.dateRange?.[1]
    } })
    rows.value = res.data?.records || []
    pagination.total = res.data?.total || 0
    selectedRows.value = []
  } finally { loading.value = false }
}

const resetFilters = () => {
  Object.assign(filters, { studentId: '', courseId: '', semesterId: '', status: 'pending', dateRange: [] })
  loadData(1)
}
const showDetail = row => { activeRow.value = row; detailVisible.value = true }
const prepareProcess = mode => {
  processMode.value = mode
  processOpinion.value = ''
  failureRows.value = []
  processVisible.value = true
}
const openApproval = row => { activeRow.value = row; prepareProcess('approve') }
const openReject = row => { activeRow.value = row; prepareProcess('reject') }
const openBatchApproval = () => prepareProcess('batch')

const failuresFromError = error => error?.response?.data?.data?.failureDetails || []
const submitProcess = async () => {
  if (processMode.value === 'batch' && !selectedRows.value.length) {
    ElMessage.warning('请重新选择待审批申请')
    return
  }
  processing.value = true
  failureRows.value = []
  try {
    if (processMode.value === 'batch') {
      const res = await request.put('/leave-requests/batch-approve', {
        requestIds: selectedRows.value.map(item => item.requestId), processOpinion: processOpinion.value
      })
      failureRows.value = (res.data?.results || []).flatMap(item => item.failureDetails || [])
      if (failureRows.value.length) {
        const failedRequestIds = new Set(failureRows.value.map(item => item.requestId))
        selectedRows.value = selectedRows.value.filter(item => failedRequestIds.has(item.requestId))
        ElMessage.warning(`批量审批部分完成：成功 ${res.data?.successCount || 0} 条，失败 ${res.data?.failedCount || 0} 条`)
        return
      }
      ElMessage.success('批量审批完成，考勤已同步')
    } else if (processMode.value === 'reject') {
      await request.put(`/leave-requests/${activeRow.value.requestId}/reject`, { processOpinion: processOpinion.value })
      ElMessage.success('申请已拒绝')
    } else {
      try {
        await request.put(`/leave-requests/${activeRow.value.requestId}/approve`, { processOpinion: processOpinion.value }, { skipErrorMessage: true })
        ElMessage.success('审批通过，考勤已同步')
      } catch (error) {
        failureRows.value = failuresFromError(error)
        ElMessage.error(error?.message || '审批未完成，请根据失败明细处理后重试')
        return
      }
    }
    processVisible.value = false
    await loadData()
  } finally { processing.value = false }
}

onMounted(() => Promise.all([loadOptions(), loadData(1)]))
</script>

<style scoped>
.approval-page { width: 100%; }
.page-header-title, .page-header-desc { margin: 0; }
.search-bar .el-input { width: 150px; }
.search-bar .el-select { width: 170px; }
.search-bar .el-date-editor { width: 260px; }
.el-table strong, .el-table small { display: block; }
.el-table small { margin-top: 2px; color: var(--text-muted); }
.failure-copy { max-width: 280px; margin: 6px 0 0; color: var(--vermilion); font-size: 12px; line-height: 1.45; }
.opinion-input { margin-top: 18px; }
.sync-failures { margin-top: 20px; }
.sync-failures h2 { margin: 0 0 10px; color: var(--ink); font-family: var(--display-font); font-size: 17px; }
@media (max-width: 768px) {
  .search-bar .el-input, .search-bar .el-select, .search-bar .el-date-editor { width: 100%; }
}
</style>
