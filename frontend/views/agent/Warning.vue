<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-title"><el-icon><Warning /></el-icon> 学业预警 Agent</div>
        <div class="page-desc">基于成绩、考勤与毕业审核规则识别学生风险，并为管理员提供干预依据。</div>
      </div>
      <div class="header-actions">
        <el-tag effect="plain" :type="agentStatus.type">{{ agentStatus.label }}</el-tag>
        <el-button type="primary" :loading="loading" @click="refreshWarnings">
          <el-icon><Refresh /></el-icon> 刷新预警
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <el-input v-model="filters.keyword" clearable placeholder="搜索学号或姓名" class="filter-control" @keyup.enter="searchWarnings" />
        <el-select v-model="filters.level" clearable placeholder="风险等级" class="filter-control">
          <el-option label="高风险" value="high" />
          <el-option label="中风险" value="medium" />
          <el-option label="低风险" value="low" />
        </el-select>
        <el-select v-model="filters.semesterId" clearable placeholder="全部学期" class="filter-control">
          <el-option v-for="semester in semesterOptions" :key="semester.semesterId" :label="semester.name" :value="semester.semesterId" />
        </el-select>
        <el-button type="primary" @click="searchWarnings">筛选</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
      <div class="source-hint">
        <el-icon><InfoFilled /></el-icon>
        {{ sourceDescription }}
      </div>
    </el-card>

    <el-card shadow="never">
      <el-skeleton v-if="loading && !hasLoaded" :rows="6" animated />
      <el-result v-else-if="listError" icon="error" title="预警数据加载失败" sub-title="请检查网络连接后重试。">
        <template #extra><el-button type="primary" @click="loadWarnings">重新加载</el-button></template>
      </el-result>
      <el-empty v-else-if="hasLoaded && warnings.length === 0" description="当前筛选条件下暂无预警学生" />
      <template v-else>
        <el-table v-loading="loading" :data="warnings" stripe>
          <el-table-column prop="studentId" label="学号" min-width="130" />
          <el-table-column prop="studentName" label="姓名" min-width="100" />
          <el-table-column prop="majorName" label="专业" min-width="150" show-overflow-tooltip />
          <el-table-column prop="className" label="班级" min-width="150" show-overflow-tooltip />
          <el-table-column label="风险等级" width="110">
            <template #default="{ row }"><el-tag :type="levelType(row.riskLevel)">{{ levelText(row.riskLevel) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="riskScore" label="风险分" width="90" />
          <el-table-column label="命中规则" min-width="210" show-overflow-tooltip>
            <template #default="{ row }">
              <el-space wrap :size="4">
                <el-tag v-for="rule in row.triggeredRules || []" :key="rule" size="small" type="warning" effect="plain">{{ rule }}</el-tag>
                <span v-if="!row.triggeredRules?.length">{{ row.triggeredRuleCount || 0 }} 条</span>
              </el-space>
            </template>
          </el-table-column>
          <el-table-column prop="riskReason" label="风险原因" min-width="260" show-overflow-tooltip />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">学生详情</el-button>
              <el-button link @click="openHistory(row)">预警历史</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="total > 0" class="pager">
          <el-pagination v-model:current-page="filters.page" v-model:page-size="filters.pageSize" background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" :total="total" @current-change="loadWarnings" @size-change="changePageSize" />
        </div>
      </template>
    </el-card>

    <el-dialog v-model="detailVisible" :title="`${selectedStudentName} · 学业预警`" width="780px" destroy-on-close>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="风险详情" name="detail">
          <el-skeleton v-if="detailLoading" :rows="8" animated />
          <el-result v-else-if="detailError" icon="error" title="详情加载失败"><template #extra><el-button type="primary" @click="loadDetail">重试</el-button></template></el-result>
          <template v-else-if="detail">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="学号">{{ detail.studentId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ detail.studentName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="专业">{{ detail.majorName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="班级">{{ detail.className || '-' }}</el-descriptions-item>
              <el-descriptions-item label="学期">{{ semesterName(detail.semesterId) }}</el-descriptions-item>
              <el-descriptions-item label="风险等级"><el-tag :type="levelType(detail.riskLevel)">{{ levelText(detail.riskLevel) }}</el-tag></el-descriptions-item>
              <el-descriptions-item label="风险分">{{ detail.riskScore ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="毕业审核">{{ auditStatusText(detail.graduationAuditStatus) }}</el-descriptions-item>
              <el-descriptions-item label="不及格课程">{{ detail.failedCourseCount ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="缺勤 / 迟到">{{ detail.absentCount ?? 0 }} / {{ detail.lateCount ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="风险原因" :span="2">{{ detail.riskReason || '暂无' }}</el-descriptions-item>
              <el-descriptions-item label="命中规则" :span="2">
                <el-space wrap><el-tag v-for="rule in detail.triggeredRules || []" :key="rule" type="warning" effect="plain">{{ rule }}</el-tag><span v-if="!detail.triggeredRules?.length">暂无</span></el-space>
              </el-descriptions-item>
              <el-descriptions-item label="干预建议" :span="2">{{ detail.interventionSuggestion || '请结合学生实际情况进行辅导。' }}</el-descriptions-item>
            </el-descriptions>
            <el-card v-if="trend" shadow="never" class="trend-card">
              <template #header>成绩趋势</template>
              <div class="trend-summary"><span>趋势结论：{{ trend.trendConclusion || '-' }}</span><span>风险变化：{{ trend.riskChange || '-' }}</span></div>
              <el-table :data="trend.semesters || []" size="small"><el-table-column prop="semesterId" label="学期" /><el-table-column prop="averageScore" label="平均分" /><el-table-column prop="failedCourseCount" label="不及格数" /><el-table-column prop="courseCount" label="课程数" /></el-table>
            </el-card>
          </template>
          <el-empty v-else description="暂无详情" />
        </el-tab-pane>
        <el-tab-pane label="预警历史" name="history">
          <el-skeleton v-if="historyLoading" :rows="5" animated />
          <el-result v-else-if="historyError" icon="error" title="历史记录加载失败"><template #extra><el-button type="primary" @click="loadHistory">重试</el-button></template></el-result>
          <el-empty v-else-if="historyLoaded && history.length === 0" description="暂无预警历史" />
          <el-table v-else :data="history" size="small">
            <el-table-column prop="semesterId" label="学期" width="110" />
            <el-table-column label="等级" width="90"><template #default="{ row }"><el-tag size="small" :type="levelType(row.riskLevel)">{{ levelText(row.riskLevel) }}</el-tag></template></el-table-column>
            <el-table-column prop="riskReason" label="风险原因" min-width="180" show-overflow-tooltip />
            <el-table-column prop="processStatus" label="处理状态" width="110"><template #default="{ row }">{{ processStatusText(row.processStatus) }}</template></el-table-column>
            <el-table-column prop="calculatedAt" label="生成时间" min-width="160" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { InfoFilled, Refresh, Warning } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const route = useRoute()
const hasLoaded = ref(false)
const listError = ref(false)
const warnings = ref([])
const total = ref(0)
const semesterOptions = ref([])
const capabilities = ref(null)
const detailVisible = ref(false)
const activeTab = ref('detail')
const selectedStudent = ref(null)
const detail = ref(null)
const trend = ref(null)
const detailLoading = ref(false)
const detailError = ref(false)
const history = ref([])
const historyLoading = ref(false)
const historyLoaded = ref(false)
const historyError = ref(false)
const filters = reactive({ keyword: '', level: '', semesterId: '', majorId: '', page: 1, pageSize: 10 })

const selectedStudentName = computed(() => selectedStudent.value?.studentName || '学生')
const agentStatus = computed(() => {
  if (!capabilities.value) return { label: '智能来源状态加载中', type: 'info' }
  return capabilities.value.llmProvider === 'deepseek'
    ? { label: 'DeepSeek + 关键词回退', type: 'success' }
    : { label: '关键词回退模式', type: 'warning' }
})
const sourceDescription = computed(() => capabilities.value?.llmProvider === 'deepseek'
  ? '当前支持 DeepSeek 辅助分析；服务不可用时自动回退为关键词匹配，风险结论仍由固定规则生成。'
  : '当前使用关键词回退模式，风险结论由固定规则生成。')

onMounted(async () => {
  // 从管理概览页带入范围，确保点击“查看预警明细”后不丢失当前筛选条件。
  filters.semesterId = route.query.semesterId || ''
  filters.majorId = route.query.majorId || ''
  filters.level = route.query.level || ''
  filters.keyword = route.query.studentId || ''
  await Promise.all([loadSemesters(), loadCapabilities()])
  await loadWarnings()
  if (route.query.studentId && warnings.value[0]?.studentId === route.query.studentId) openDetail(warnings.value[0])
})

async function loadSemesters() {
  try {
    const res = await request.get('/semester/list')
    semesterOptions.value = res.data || []
  } catch (error) {
    semesterOptions.value = []
  }
}

async function loadCapabilities() {
  try {
    const res = await request.get('/agent/capabilities')
    capabilities.value = res.data || {}
  } catch (error) {
    // 能力接口不可用时明确显示保守的关键词回退状态，避免将未知来源标记为模型输出。
    capabilities.value = { fallback: 'keyword_matching' }
  }
}

async function loadWarnings() {
  loading.value = true
  listError.value = false
  try {
    // 列表请求只提交筛选参数，风险等级和分数由后端规则统一计算。
    const res = await request.get('/agent/warnings/students', { params: buildListParams() })
    // 后端返回 MyBatis 分页结构，这里只抽取表格记录和总数。
    warnings.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
    hasLoaded.value = true
  } catch (error) {
    // 请求失败时清空旧数据并展示错误态，避免误读过期预警结果。
    warnings.value = []
    total.value = 0
    listError.value = true
    hasLoaded.value = true
  } finally {
    loading.value = false
  }
}

function buildListParams() {
  const params = { page: filters.page, pageSize: filters.pageSize }
  // 空筛选项不下发，避免后端把空字符串当作有效条件。
  ;['keyword', 'level', 'semesterId', 'majorId'].forEach((key) => { if (filters[key]) params[key] = filters[key] })
  return params
}

function searchWarnings() { filters.page = 1; loadWarnings() }
function refreshWarnings() { loadWarnings() }
function resetFilters() { Object.assign(filters, { keyword: '', level: '', semesterId: '', majorId: '', page: 1 }); loadWarnings() }
// 修改每页条数时回到第一页，避免当前页超出新的总页数。
function changePageSize() { filters.page = 1; loadWarnings() }

function openDetail(row) {
  selectedStudent.value = row
  detailVisible.value = true
  activeTab.value = 'detail'
  loadDetail()
}

function openHistory(row) {
  selectedStudent.value = row
  detailVisible.value = true
  activeTab.value = 'history'
  loadHistory()
}

async function loadDetail() {
  if (!selectedStudent.value?.studentId) return
  detailLoading.value = true
  detailError.value = false
  detail.value = selectedStudent.value
  trend.value = null
  try {
    const params = filters.semesterId ? { semesterId: filters.semesterId } : {}
    // 详情接口是完整风险证据的唯一来源；成绩趋势失败不应遮蔽详情。
    const detailRes = await request.get(`/agent/warnings/students/${selectedStudent.value.studentId}`, { params })
    detail.value = detailRes.data || selectedStudent.value
    try {
      // 趋势为空或接口异常时保留详情，并以空态提示，避免展示过期趋势数据。
      const trendRes = await request.get(`/agent/warnings/students/${selectedStudent.value.studentId}/grade-trend`)
      trend.value = trendRes.data || null
    } catch (error) {
      trend.value = null
    }
  } catch (error) {
    // 详情加载失败保留列表行基础信息，同时提示用户重试。
    detailError.value = true
  } finally {
    detailLoading.value = false
  }
}

async function loadHistory() {
  if (!selectedStudent.value?.studentId) return
  historyLoading.value = true
  historyLoaded.value = false
  historyError.value = false
  try {
    const params = { page: 1, pageSize: 20 }
    if (filters.semesterId) params.semesterId = filters.semesterId
    // 历史接口的 studentId 是学生内部 String 主键；后端统一 Result 的 data.records 为分页记录数组。
    const res = await request.get(`/agent/warnings/students/${selectedStudent.value.studentId}/records`, { params })
    history.value = res.data?.records || []
    historyLoaded.value = true
  } catch (error) {
    // 历史失败不影响当前详情，只切换历史页错误态。
    history.value = []
    historyError.value = true
  } finally {
    historyLoading.value = false
  }
}

function levelText(level) { return ({ high: '高风险', medium: '中风险', low: '低风险' }[level] || '待评估') }
function levelType(level) { return ({ high: 'danger', medium: 'warning', low: 'success' }[level] || 'info') }
function semesterName(id) { return semesterOptions.value.find(item => item.semesterId === id)?.name || id || '跨学期汇总' }
function auditStatusText(status) { return ({ approved: '通过', rejected: '未通过', pending: '待审核', unaudited: '未审核' }[status] || status || '未审核') }
function processStatusText(status) { return ({ pending: '待处理', processing: '处理中', completed: '已完成', ignored: '已忽略' }[status] || status || '待处理') }
</script>

<style scoped>
.page-container { width: 100%; }
.page-header, .header-actions, .filter-bar, .source-hint { display: flex; align-items: center; }
.page-header { justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.header-actions { gap: 12px; }
.page-title { display: flex; align-items: center; gap: 8px; color: var(--ink); font-family: var(--display-font); font-size: 22px; font-weight: 700; }
.page-desc { margin-top: 6px; color: var(--text-secondary); font-size: 13px; }
.filter-card { margin-bottom: 16px; }
.filter-bar { flex-wrap: wrap; gap: 12px; }
.filter-control { width: 220px; }
.source-hint { gap: 6px; margin-top: 12px; color: var(--text-secondary); font-size: 12px; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
.trend-card { margin-top: 16px; }
.trend-summary { display: grid; gap: 8px; margin-bottom: 12px; color: var(--text-secondary); }
@media (max-width: 640px) { .page-header { align-items: stretch; flex-direction: column; } .header-actions { justify-content: space-between; } .filter-control { width: 100%; } }
</style>
