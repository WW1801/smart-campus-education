<template>
  <div class="analysis-overview">
    <header class="overview-header">
      <div>
        <div class="overview-eyebrow">AI 智能教务</div>
        <h1>教务数据总览</h1>
        <p>将全校核心指标、风险信号与待处理事项集中呈现。</p>
      </div>
      <el-button type="primary" size="large" :loading="asking" @click="generateBrief">生成分析简报</el-button>
    </header>

    <section class="overview-filters" aria-label="数据筛选">
      <el-select v-model="filters.semesterId" clearable placeholder="全部学期" @change="loadDashboard">
        <el-option v-for="semester in semesterOptions" :key="semester.semesterId" :label="semester.name" :value="semester.semesterId" />
      </el-select>
      <el-select v-model="filters.departmentId" clearable placeholder="全部院系" @change="handleDepartmentChange">
        <el-option v-for="department in departmentOptions" :key="department.departmentId" :label="department.name" :value="department.departmentId" />
      </el-select>
      <el-select v-model="filters.majorId" clearable placeholder="全部专业" @change="loadDashboard">
        <el-option v-for="major in filteredMajorOptions" :key="major.majorId" :label="major.name" :value="major.majorId" />
      </el-select>
      <el-button size="large" @click="resetFilters">重置</el-button>
    </section>

    <el-alert v-if="filterError" class="partial-alert" type="warning" title="部分筛选项未加载，可直接查看当前范围或稍后重试。" :closable="false" show-icon />
    <el-alert v-if="partialError" class="partial-alert" type="warning" title="部分分析数据未加载，页面仅保留本次成功返回的内容。" :closable="false" show-icon />

    <el-skeleton v-if="loading && !loaded" :rows="10" animated />
    <PageErrorState v-else-if="loadError" :retrying="loading" title="数据总览加载失败" @retry="loadDashboard" />

    <template v-else>
      <section class="overview-stats" aria-label="核心指标">
        <article v-for="item in statCards" :key="item.key" class="overview-stat-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small :class="item.noteType">{{ item.note }}</small>
        </article>
      </section>

      <section class="overview-grid overview-grid-top">
        <el-card shadow="never" class="overview-card major-card">
          <template #header><div class="overview-card-header"><h2>各专业学生规模</h2><el-button link type="primary" @click="openWarnings()">查看明细 →</el-button></div></template>
          <el-empty v-if="majorSummary.length === 0" description="当前筛选范围暂无专业数据" :image-size="90" />
          <div v-else class="major-list">
            <button v-for="major in visibleMajorSummary" :key="major.majorId || major.majorName" type="button" class="major-row" @click="openWarnings(major.majorId)">
              <span>{{ major.majorName }}</span>
              <span class="major-track"><i :style="{ width: `${studentPercent(major.studentCount)}%` }"></i></span>
              <strong>{{ major.studentCount }}</strong>
            </button>
          </div>
        </el-card>

        <el-card shadow="never" class="overview-card risk-card">
          <template #header><h2>学业风险分布</h2></template>
          <div v-if="warningTotal > 0" class="risk-content" aria-label="学业风险人数分布">
            <div v-for="item in riskRows" :key="item.key" class="risk-row" :class="`risk-${item.key}`">
              <span class="risk-label"><i aria-hidden="true"></i>{{ item.label }}</span>
              <span class="risk-track" aria-hidden="true"><i :style="{ width: `${item.percent}%` }"></i></span>
              <strong>{{ item.value }} 人</strong>
            </div>
            <div class="risk-total"><span>总体预警率</span><strong>{{ overallWarningRate }}</strong></div>
          </div>
          <el-empty v-else description="当前筛选范围暂无预警学生" :image-size="90" />
        </el-card>
      </section>

      <section class="overview-grid overview-grid-bottom">
        <el-card shadow="never" class="overview-card intervention-card">
          <template #header><div class="overview-card-header"><h2>优先干预学生</h2><el-button link type="primary" @click="openWarnings('', 'high')">进入预警 Agent →</el-button></div></template>
          <el-empty v-if="highRiskStudents.length === 0" description="当前筛选范围暂无高风险学生" :image-size="90" />
          <el-table v-else :data="highRiskStudents" size="small">
            <el-table-column prop="studentName" label="学生" min-width="90" />
            <el-table-column prop="majorName" label="专业" min-width="130" />
            <el-table-column prop="riskReason" label="风险原因" min-width="220" show-overflow-tooltip />
            <el-table-column label="等级" width="90"><template #default><el-tag type="danger" size="small">高风险</el-tag></template></el-table-column>
            <el-table-column label="操作" width="70"><template #default="{ row }"><el-button link type="primary" @click="openWarnings('', 'high', row.studentId)">详情</el-button></template></el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="overview-card ai-card">
          <template #header><div class="overview-card-header"><h2>向 AI 提问</h2><el-tag size="small" :type="answerSource === 'deepseek' ? 'success' : 'info'">{{ sourceText }}</el-tag></div></template>
          <div class="question-row">
            <el-input v-model="question" clearable placeholder="例如：各专业分别有多少学生？" @keyup.enter="askAgent" />
            <el-button type="primary" :loading="asking" @click="askAgent">分析</el-button>
          </div>
          <div v-if="answer" class="answer-box"><strong>结果摘要：</strong>{{ answer }}</div>
          <div v-else class="question-examples">可询问专业人数、成绩通过率、异常考勤或学业预警情况。</div>
          <div class="risk-signal"><el-icon><WarningFilled /></el-icon><span>{{ riskSignal.message || '当前筛选范围暂无明显风险信号。' }}</span></div>
        </el-card>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import request from '../../utils/request'

const router = useRouter()
const loading = ref(false)
const loaded = ref(false)
const loadError = ref(false)
const partialError = ref(false)
const filterError = ref(false)
const asking = ref(false)
const question = ref('')
const answer = ref('')
const answerSource = ref('')
const analysis = ref({})
const majorSummary = ref([])
const highRiskStudents = ref([])
const highRiskTotal = ref(0)
const mediumRiskTotal = ref(0)
const lowRiskTotal = ref(0)
const semesterOptions = ref([])
const departmentOptions = ref([])
const majorOptions = ref([])
const filters = reactive({ semesterId: '', departmentId: '', majorId: '' })

const overview = computed(() => analysis.value.dataOverview || {})
const gradeAnalysis = computed(() => analysis.value.gradeAnalysis || {})
const riskSignal = computed(() => overview.value.riskSignal || {})
const warningTotal = computed(() => highRiskTotal.value + mediumRiskTotal.value + lowRiskTotal.value)
const visibleMajorSummary = computed(() => majorSummary.value.slice(0, 5))
const maxStudentCount = computed(() => Math.max(...majorSummary.value.map(item => Number(item.studentCount) || 0), 1))
const overallWarningRate = computed(() => formatPercent(overview.value.studentCount ? warningTotal.value * 100 / overview.value.studentCount : 0))
const sourceText = computed(() => !answer.value ? '等待提问' : answerSource.value === 'deepseek' ? 'DeepSeek 结果解释' : '固定查询结果')
const filteredMajorOptions = computed(() => filters.departmentId
  ? majorOptions.value.filter(item => item.departmentId === filters.departmentId)
  : majorOptions.value)
const riskRows = computed(() => {
  const total = warningTotal.value || 1
  return [
    { key: 'high', label: '高风险', value: highRiskTotal.value, percent: highRiskTotal.value * 100 / total },
    { key: 'medium', label: '中风险', value: mediumRiskTotal.value, percent: mediumRiskTotal.value * 100 / total },
    { key: 'low', label: '低风险', value: lowRiskTotal.value, percent: lowRiskTotal.value * 100 / total }
  ]
})
const statCards = computed(() => [
  { key: 'active', label: '在读学生', value: formatNumber(overview.value.activeStudentCount), note: '数据库实时统计', noteType: 'success-note' },
  { key: 'pass', label: '成绩通过率', value: formatPercent(gradeAnalysis.value.passRate), note: '基于已审核成绩', noteType: 'success-note' },
  { key: 'attendance', label: '异常考勤', value: formatNumber(overview.value.abnormalAttendanceCount), note: '缺勤、迟到及请假记录', noteType: 'warning-note' },
  { key: 'risk', label: '高风险预警', value: formatNumber(highRiskTotal.value), note: '需要优先干预', noteType: 'danger-note' }
])

onMounted(async () => {
  // 先加载筛选项并选中当前学期，再查询统计数据，避免首屏口径与筛选器不一致。
  await loadSelectOptions()
  await loadDashboard()
})

async function loadSelectOptions() {
  filterError.value = false
  const results = await Promise.allSettled([
    request.get('/semester/list', { skipErrorMessage: true }),
    request.get('/department/list', { skipErrorMessage: true }),
    request.get('/major/list', { skipErrorMessage: true })
  ])
  const valueOf = index => results[index].status === 'fulfilled' ? results[index].value.data || [] : []
  semesterOptions.value = valueOf(0)
  departmentOptions.value = valueOf(1)
  majorOptions.value = valueOf(2)
  filterError.value = results.some(result => result.status === 'rejected')
  const currentSemester = semesterOptions.value.find(item => ['current', 'active'].includes(item.status))
  filters.semesterId = currentSemester?.semesterId || ''
}

async function loadDashboard() {
  loading.value = true
  loadError.value = false
  partialError.value = false
  analysis.value = {}
  majorSummary.value = []
  highRiskStudents.value = []
  highRiskTotal.value = 0
  mediumRiskTotal.value = 0
  lowRiskTotal.value = 0
  // 所有板块共享同一组筛选参数，并允许局部接口失败时保留本次成功结果。
  const params = buildParams()
  const results = await Promise.allSettled([
    request.get('/agent/analysis', { params, skipErrorMessage: true }),
    request.get('/agent/analysis/major-summary', { params, skipErrorMessage: true }),
    request.get('/agent/warnings/students', { params: { ...params, level: 'high', page: 1, pageSize: 5 }, skipErrorMessage: true }),
    request.get('/agent/warnings/students', { params: { ...params, level: 'medium', page: 1, pageSize: 1 }, skipErrorMessage: true }),
    request.get('/agent/warnings/students', { params: { ...params, level: 'low', page: 1, pageSize: 1 }, skipErrorMessage: true })
  ])
  const fulfilled = index => results[index].status === 'fulfilled' ? results[index].value : null
  const analysisRes = fulfilled(0)
  const majorRes = fulfilled(1)
  const highRes = fulfilled(2)
  const mediumRes = fulfilled(3)
  const lowRes = fulfilled(4)
  analysis.value = analysisRes?.data || {}
  majorSummary.value = majorRes?.data || []
  highRiskStudents.value = highRes?.data?.records || []
  highRiskTotal.value = Number(highRes?.data?.total || 0)
  mediumRiskTotal.value = Number(mediumRes?.data?.total || 0)
  lowRiskTotal.value = Number(lowRes?.data?.total || 0)
  const failures = results.filter(result => result.status === 'rejected').length
  loadError.value = failures === results.length
  partialError.value = failures > 0 && !loadError.value
  loaded.value = true
  loading.value = false
}

function buildParams() {
  const params = {}
  ;['semesterId', 'departmentId', 'majorId'].forEach((key) => { if (filters[key]) params[key] = filters[key] })
  return params
}

function handleDepartmentChange() {
  if (filters.majorId && !filteredMajorOptions.value.some(item => item.majorId === filters.majorId)) filters.majorId = ''
  loadDashboard()
}

function resetFilters() { filters.semesterId = ''; filters.departmentId = ''; filters.majorId = ''; loadDashboard() }
function studentPercent(count) { return Math.round((Number(count) || 0) * 100 / maxStudentCount.value) }
function formatPercent(value) { return `${Number(value || 0).toFixed(1)}%` }
function formatNumber(value) { return Number(value || 0).toLocaleString('zh-CN') }

function openWarnings(majorId = '', level = '', studentId = '') {
  // 将总览范围传给预警页，形成“发现风险—定位专业—查看学生”的操作闭环。
  router.push({ path: '/home/agent/warning', query: { ...buildParams(), majorId: majorId || filters.majorId || undefined, level: level || undefined, studentId: studentId || undefined } })
}

async function generateBrief() {
  question.value = '请概括当前范围的成绩、考勤和学业预警情况'
  await askAgent()
}

async function askAgent() {
  if (!question.value.trim()) return ElMessage.warning('请输入教务问题')
  asking.value = true
  try {
    const res = await request.post('/agent/chat', { question: question.value.trim(), ...buildParams() }, { skipErrorMessage: true })
    answer.value = res.data?.answer || 'Agent 暂未返回文字结果'
    // 来源缺失时按固定查询展示，避免将未知答案误标为模型生成。
    answerSource.value = res.data?.answerSource || 'keyword'
  } catch (error) {
    answer.value = '当前问答接口不可用，请先使用页面中的固定统计数据。'
    answerSource.value = 'keyword'
  } finally {
    asking.value = false
  }
}
</script>

<style scoped>
.analysis-overview { width: 100%; max-width: 1480px; min-height: 100%; margin: 0 auto; padding: 8px; color: var(--ink); }
.overview-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; margin-bottom: 28px; padding: 8px 2px 0; }
.overview-eyebrow { margin-bottom: 8px; color: var(--blue); font-family: var(--latin-font); font-size: 13px; font-weight: 700; letter-spacing: .08em; }
.overview-header h1 { margin: 0; color: var(--ink); font-family: var(--display-font); font-size: 30px; font-weight: 700; letter-spacing: -.02em; line-height: 1.25; }
.overview-header p { margin: 8px 0 0; color: var(--text-secondary); font-size: 14px; }
.overview-filters { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 24px; }
.overview-filters :deep(.el-select) { width: 220px; }
.overview-filters :deep(.el-select__wrapper), .overview-filters :deep(.el-button) { min-height: 44px; border-radius: 9px; }
.overview-filters :deep(.el-select__wrapper) { box-shadow: var(--shadow-sm); }
.partial-alert { margin-bottom: 16px; }
.overview-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0; margin-bottom: 18px; overflow: hidden; background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-md); box-shadow: var(--shadow-card); }
.overview-stat-card { min-width: 0; min-height: 142px; padding: 22px 24px; background: var(--surface); border-right: 1px solid var(--border-light); }
.overview-stat-card:last-child { border-right: 0; }
.overview-stat-card span, .overview-stat-card small { display: block; }
.overview-stat-card span { color: var(--text-secondary); font-size: 14px; }
.overview-stat-card strong { display: block; margin: 12px 0 4px; color: var(--ink); font-family: var(--display-font); font-size: 34px; line-height: 1.2; }
.overview-stat-card small { color: var(--text-secondary); font-size: 12px; }
.overview-stat-card .success-note { color: var(--sage); }.overview-stat-card .warning-note { color: var(--text-secondary); }.overview-stat-card .danger-note { color: var(--vermilion); }
.overview-grid { display: grid; gap: 18px; }.overview-grid-top { grid-template-columns: 1.48fr 1fr; }.overview-grid-bottom { grid-template-columns: 1.48fr 1fr; }
.overview-card { margin-bottom: 18px; border: 1px solid var(--line) !important; border-radius: var(--radius-md) !important; box-shadow: var(--shadow-card) !important; }
.overview-card :deep(.el-card__header) { padding: 20px 26px !important; }.overview-card :deep(.el-card__body) { padding: 24px 26px !important; }
.overview-card h2 { margin: 0; color: var(--ink); font-size: 18px; }.overview-card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.major-list { display: grid; gap: 18px; }.major-row { display: grid; grid-template-columns: 130px 1fr 52px; align-items: center; gap: 14px; width: 100%; padding: 4px 0; color: var(--ink); font: inherit; text-align: left; background: transparent; border: 0; border-radius: 6px; cursor: pointer; }.major-row:hover { color: var(--blue); }.major-track { height: 10px; overflow: hidden; background: var(--bg-soft); border-radius: 999px; }.major-track i { display: block; height: 100%; background: var(--blue); border-radius: inherit; }.major-row strong { text-align: right; }
.risk-content { display: grid; align-content: center; gap: 18px; min-height: 225px; }.risk-row { display: grid; grid-template-columns: 82px minmax(0, 1fr) 58px; align-items: center; gap: 12px; }.risk-label { display: flex; align-items: center; gap: 8px; }.risk-label i { width: 10px; height: 10px; border: 1px solid currentColor; border-radius: 2px; }.risk-track { height: 10px; overflow: hidden; background: var(--bg-soft); border: 1px solid var(--line); border-radius: 3px; }.risk-track i { display: block; height: 100%; background: currentColor; }.risk-high { color: var(--vermilion); }.risk-medium { color: var(--text-secondary); }.risk-low { color: var(--sage); }.risk-row strong { color: var(--ink); text-align: right; }.risk-total { display: flex; justify-content: space-between; padding-top: 14px; color: var(--ink); border-top: 1px solid var(--border-light); }
.question-row { display: flex; gap: 10px; }.question-row :deep(.el-input) { flex: 1; }.answer-box { margin-top: 16px; padding: 15px 16px; color: var(--text-regular); line-height: 1.7; background: var(--primary-bg); border: 1px solid var(--line); border-radius: 8px; }.question-examples { margin-top: 16px; color: var(--text-regular); font-size: 13px; }.risk-signal { display: flex; align-items: flex-start; gap: 8px; margin-top: 16px; padding-top: 14px; color: var(--vermilion); font-size: 12px; border-top: 1px solid var(--border-light); }.risk-signal .el-icon { margin-top: 3px; flex: none; }
@media (max-width: 1100px) { .overview-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }.overview-stat-card:nth-child(2) { border-right: 0; }.overview-stat-card:nth-child(-n + 2) { border-bottom: 1px solid var(--border-light); }.overview-grid-top,.overview-grid-bottom { grid-template-columns: 1fr; } }
@media (max-width: 640px) { .analysis-overview { padding: 0; }.overview-header { flex-direction: column; }.overview-header h1 { font-size: 26px; }.overview-filters :deep(.el-select),.overview-filters :deep(.el-button) { width: 100%; }.overview-stats { grid-template-columns: 1fr; }.overview-stat-card { min-height: 120px; border-right: 0; border-bottom: 1px solid var(--border-light); }.overview-stat-card:nth-child(3) { border-bottom: 1px solid var(--border-light); }.overview-stat-card:last-child { border-bottom: 0; }.major-row { grid-template-columns: minmax(82px, 100px) minmax(70px, 1fr) 42px; }.question-row { flex-direction: column; }.risk-row { grid-template-columns: 74px minmax(0, 1fr) 54px; } }
</style>
