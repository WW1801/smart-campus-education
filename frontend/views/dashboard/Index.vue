<!--
  THESIS: 以正在运转的教务台账作为首屏主体，拒绝指标卡墙先于任务的后台惯例。
  OWN-WORLD: 深墨蓝索引、冷灰雾蓝工作区、白色纸面与钴蓝细标记；朱红只表示紧急与高风险。
  STORY: 管理员先处理需要介入的事项，再核对近期数据摘要和运行指标，并进入对应业务模块。
  FIRST VIEWPORT: 中央待办台账占据主空间，右侧承载数据摘要、快捷入口和逾期摘要，指标连续分栏置后。
  FORM: 用户锁定的三栏校园控制台原型；数据来自现有分析与预警接口，不把原型示例值冒充业务事实。
-->
<template>
  <div class="dashboard-page">
    <header class="dashboard-header">
      <div>
        <h1>数据看板</h1>
        <p>待办事项与教务运行概览 · {{ semesterLabel }}</p>
      </div>
      <div class="header-actions">
        <span v-if="lastUpdated" class="updated-at" aria-live="polite">更新于 {{ lastUpdated }}</span>
        <el-button :loading="loading" @click="loadDashboard">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button type="primary" @click="router.push('/home/system/user')">
          <el-icon><Plus /></el-icon>
          新增账号
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="partialError && !dashboardError"
      class="partial-alert"
      type="warning"
      title="部分数据暂未加载，已保留可用内容；可点击刷新重试。"
      :closable="false"
      show-icon
    />

    <div class="dashboard-grid">
      <div class="primary-column">
        <section class="ledger-panel" aria-labelledby="ledger-title">
          <div class="panel-heading">
            <div>
              <span class="section-mark" aria-hidden="true"></span>
              <h2 id="ledger-title">待办台账</h2>
            </div>
            <p v-if="!loading && !dashboardError">
              当前展示 {{ ledgerItems.length }} 项
              <strong v-if="warningTotal"> · 高风险预警共 {{ warningTotal }} 人</strong>
            </p>
          </div>

          <div v-if="loading" class="state-panel" aria-live="polite" aria-label="正在加载待办台账">
            <el-skeleton :rows="6" animated />
          </div>

          <div v-else-if="dashboardError" class="state-panel state-message" role="alert">
            <span class="state-code">ED-503</span>
            <h3>暂时无法取得教务台账</h3>
            <p>教务分析与预警服务均未返回数据，当前页面未展示过期缓存。</p>
            <el-button type="primary" @click="loadDashboard">重新加载</el-button>
          </div>

          <div v-else-if="ledgerItems.length === 0" class="state-panel state-message">
            <span class="empty-seal" aria-hidden="true">清</span>
            <h3>当前范围暂无待办</h3>
            <p>未发现不及格成绩、异常考勤、毕业审核未通过或高风险学业预警。</p>
            <el-button @click="router.push('/home/agent/warning')">查看预警台账</el-button>
          </div>

          <template v-else>
            <div class="ledger-table-wrap">
              <el-table :data="ledgerItems" class="ledger-table" row-key="id">
                <el-table-column prop="id" label="编号" width="116">
                  <template #default="{ row }"><span class="ledger-id">{{ row.id }}</span></template>
                </el-table-column>
                <el-table-column prop="matter" label="事项" min-width="250">
                  <template #default="{ row }"><strong class="ledger-matter">{{ row.matter }}</strong></template>
                </el-table-column>
                <el-table-column prop="owner" label="责任范围" min-width="130" />
                <el-table-column prop="priority" label="处置优先级" width="120" />
                <el-table-column label="状态" width="108">
                  <template #default="{ row }">
                    <span class="status-stamp" :class="`status-${row.tone}`">{{ row.status }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="92" fixed="right">
                  <template #default="{ row }">
                    <el-button link :type="row.tone === 'urgent' ? 'danger' : 'primary'" @click="openTask(row)">
                      {{ row.action }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="mobile-ledger" aria-label="移动端待办台账">
              <article v-for="item in ledgerItems" :key="item.id" class="mobile-ledger-item">
                <div class="mobile-ledger-top">
                  <span class="ledger-id">{{ item.id }}</span>
                  <span class="status-stamp" :class="`status-${item.tone}`">{{ item.status }}</span>
                </div>
                <h3>{{ item.matter }}</h3>
                <dl>
                  <div><dt>责任范围</dt><dd>{{ item.owner }}</dd></div>
                  <div><dt>处置优先级</dt><dd>{{ item.priority }}</dd></div>
                </dl>
                <el-button :type="item.tone === 'urgent' ? 'danger' : 'primary'" plain @click="openTask(item)">
                  {{ item.action }}
                </el-button>
              </article>
            </div>
          </template>
        </section>

        <section class="metrics-panel" aria-labelledby="metrics-title">
          <div class="metrics-heading">
            <div>
              <span class="section-mark" aria-hidden="true"></span>
              <h2 id="metrics-title">运行指标</h2>
            </div>
            <span>当前筛选范围</span>
          </div>
          <div class="metrics-strip">
            <article v-for="metric in metrics" :key="metric.key" class="metric" :class="{ urgent: metric.tone === 'urgent' }">
              <span class="metric-label">{{ metric.label }}</span>
              <strong class="metric-value">{{ loading || dashboardError ? '—' : metric.value }}</strong>
              <div class="metric-foot">
                <span>{{ dashboardError ? '数据不可用' : metric.note }}</span>
                <svg class="sparkline" viewBox="0 0 76 24" role="img" :aria-label="`${metric.label}暂无历史时序数据`">
                  <title>{{ metric.label }}暂无历史时序数据</title>
                  <path d="M3 14 L15 14 L27 14 L39 14 L51 14 L63 14 L73 14" />
                  <circle cx="73" cy="14" r="2.5" />
                </svg>
              </div>
            </article>
          </div>
          <p class="trend-note">迷你趋势线为“等待时序数据”状态；现有接口仅返回当前汇总值。</p>
        </section>
      </div>

      <aside class="right-rail" aria-label="数据摘要与快捷入口">
        <section class="rail-section">
          <div class="rail-title-row">
            <span class="section-mark" aria-hidden="true"></span>
            <h2>数据摘要</h2>
          </div>
          <ol v-if="activityItems.length" class="activity-list">
            <li v-for="(item, index) in activityItems" :key="`${item}-${index}`">
              <span class="activity-dot" :class="{ urgent: index > 0 && /不及格|异常|风险/.test(item) }"></span>
              <p>{{ item }}</p>
              <span class="activity-source">分析接口摘要</span>
            </li>
          </ol>
          <div v-else class="rail-empty">暂无可展示的数据摘要</div>
        </section>

        <section class="rail-section">
          <div class="rail-title-row">
            <span class="section-mark" aria-hidden="true"></span>
            <h2>快捷入口</h2>
          </div>
          <div class="quick-links">
            <button v-for="link in quickLinks" :key="link.path" type="button" @click="router.push(link.path)">
              <span aria-hidden="true">→</span>
              {{ link.label }}
            </button>
          </div>
        </section>

        <section class="rail-section overdue-summary" aria-labelledby="overdue-title">
          <span class="overdue-value">—</span>
          <div>
            <h2 id="overdue-title">逾期摘要</h2>
            <p>当前接口未返回截止时间字段，暂不能判定逾期；高风险事项已在台账中单独标记。</p>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Refresh } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useCurrentSemester } from '../../composables/useCurrentSemester'

const router = useRouter()
const { currentSemester, semesterLabel, loadCurrentSemester } = useCurrentSemester()
const loading = ref(true)
const dashboardError = ref(false)
const partialError = ref(false)
const analysis = ref({})
const warnings = ref([])
const warningTotal = ref(0)
const lastUpdated = ref('')

const overview = computed(() => analysis.value?.dataOverview || {})
const activityItems = computed(() => Array.isArray(analysis.value?.insights) ? analysis.value.insights.slice(0, 5) : [])

const formatNumber = value => Number(value || 0).toLocaleString('zh-CN')
const formatPercent = value => `${Number(value || 0).toFixed(1)}%`

const metrics = computed(() => [
  {
    key: 'active-students',
    label: '在读学生',
    value: formatNumber(overview.value.activeStudentCount),
    note: `统计学生 ${formatNumber(overview.value.studentCount)} 人`,
    tone: 'normal'
  },
  {
    key: 'grade-pass',
    label: '成绩通过率',
    value: formatPercent(overview.value.gradePassRate),
    note: `${formatNumber(overview.value.failedCourseCount)} 条不及格记录`,
    tone: Number(overview.value.failedCourseCount || 0) > 0 ? 'urgent' : 'normal'
  },
  {
    key: 'attendance',
    label: '异常考勤',
    value: formatNumber(overview.value.abnormalAttendanceCount),
    note: `共 ${formatNumber(overview.value.attendanceRecordCount)} 条考勤`,
    tone: Number(overview.value.abnormalAttendanceCount || 0) > 0 ? 'urgent' : 'normal'
  },
  {
    key: 'graduation',
    label: '毕业审核通过率',
    value: formatPercent(overview.value.graduationApprovalRate),
    note: `${formatNumber(overview.value.approvedGraduationCount)} / ${formatNumber(overview.value.graduationAuditCount)} 项通过`,
    tone: Number(overview.value.rejectedGraduationCount || 0) > 0 ? 'urgent' : 'normal'
  }
])

const ledgerItems = computed(() => {
  const items = []
  const failedCourseCount = Number(overview.value.failedCourseCount || 0)
  const abnormalAttendanceCount = Number(overview.value.abnormalAttendanceCount || 0)
  const rejectedGraduationCount = Number(overview.value.rejectedGraduationCount || 0)

  warnings.value.slice(0, 3).forEach((warning, index) => {
    items.push({
      id: `AW-${warning.studentId || String(index + 1).padStart(3, '0')}`,
      matter: `复核 ${warning.studentName || warning.studentId || '学生'} 的学业预警`,
      owner: warning.className || warning.majorName || '学业预警',
      priority: '优先',
      status: '高风险',
      tone: 'urgent',
      action: '复核',
      route: { path: '/home/agent/warning', query: { studentId: warning.studentId, level: 'high' } }
    })
  })

  if (rejectedGraduationCount > 0) {
    items.push({
      id: 'GA-001',
      matter: `复核毕业审核未通过记录 · ${rejectedGraduationCount} 项`,
      owner: '毕业审核',
      priority: '优先',
      status: '紧急',
      tone: 'urgent',
      action: '处理',
      route: '/home/graduation'
    })
  }

  if (failedCourseCount > 0) {
    items.push({
      id: 'GR-001',
      matter: `核对已审核不及格成绩 · ${failedCourseCount} 条`,
      owner: '成绩管理',
      priority: '常规',
      status: '待处理',
      tone: 'pending',
      action: '核对',
      route: '/home/grade/query'
    })
  }

  if (abnormalAttendanceCount > 0) {
    items.push({
      id: 'AT-001',
      matter: `核查缺勤、迟到及请假记录 · ${abnormalAttendanceCount} 条`,
      owner: '考勤管理',
      priority: '常规',
      status: '待处理',
      tone: 'pending',
      action: '核查',
      route: '/home/attendance/statistics'
    })
  }

  return items
})

const quickLinks = [
  { label: '用户与账号管理', path: '/home/system/user' },
  { label: '排课调整', path: '/home/schedule/arrange' },
  { label: '学业预警详情', path: '/home/agent/warning' },
  { label: '毕业审核进度', path: '/home/graduation' }
]

const openTask = item => router.push(item.route)

const loadDashboard = async () => {
  loading.value = true
  dashboardError.value = false
  partialError.value = false
  // 新一轮请求开始时清空旧响应，失败状态绝不回显过期指标或预警记录。
  analysis.value = {}
  warnings.value = []
  warningTotal.value = 0

  const semesterParams = currentSemester.value?.semesterId
    ? { semesterId: currentSemester.value.semesterId }
    : {}
  const [analysisResult, warningsResult] = await Promise.allSettled([
    request.get('/agent/analysis', { params: semesterParams, skipErrorMessage: true }),
    request.get('/agent/warnings/students', {
      params: { ...semesterParams, level: 'high', page: 1, pageSize: 6 },
      skipErrorMessage: true
    })
  ])

  if (analysisResult.status === 'fulfilled') {
    analysis.value = analysisResult.value.data || {}
  }
  if (warningsResult.status === 'fulfilled') {
    warnings.value = warningsResult.value.data?.records || []
    warningTotal.value = Number(warningsResult.value.data?.total || warnings.value.length)
  }

  const failures = [analysisResult, warningsResult].filter(result => result.status === 'rejected').length
  dashboardError.value = failures === 2
  partialError.value = failures === 1
  if (!dashboardError.value) {
    lastUpdated.value = new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date())
  }
  loading.value = false
}

onMounted(async () => {
  await loadCurrentSemester()
  await loadDashboard()
})
</script>

<style scoped>
.dashboard-page {
  width: 100%;
  max-width: 1540px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.dashboard-header h1 {
  margin: 0;
  color: var(--ink);
  font-family: var(--display-font);
  font-size: clamp(28px, 3vw, 38px);
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.2;
}

.dashboard-header p {
  margin: 5px 0 0;
  color: var(--text-regular);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 9px;
}

.header-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.updated-at {
  margin-right: 4px;
  color: var(--text-regular);
  font-size: 12px;
}

.partial-alert {
  margin-bottom: 16px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 304px;
  gap: 20px;
  align-items: start;
}

.primary-column {
  display: grid;
  min-width: 0;
  gap: 18px;
}

.ledger-panel,
.metrics-panel,
.right-rail {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}

.ledger-panel {
  min-width: 0;
  overflow: hidden;
}

.panel-heading,
.metrics-heading {
  display: flex;
  min-height: 64px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-light);
}

.panel-heading > div,
.metrics-heading > div,
.rail-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-mark {
  display: block;
  width: 3px;
  height: 22px;
  flex: 0 0 3px;
  background: var(--blue);
  border-radius: 1px;
}

.panel-heading h2,
.metrics-heading h2,
.rail-title-row h2,
.overdue-summary h2 {
  margin: 0;
  color: var(--ink);
  font-family: var(--display-font);
  font-size: 18px;
  font-weight: 700;
}

.panel-heading p,
.metrics-heading > span {
  margin: 0;
  color: var(--text-regular);
  font-size: 12px;
}

.panel-heading strong {
  color: var(--vermilion);
}

.state-panel {
  min-height: 330px;
  padding: 26px 22px;
}

.state-message {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  flex-direction: column;
}

.state-code,
.empty-seal {
  display: grid;
  min-width: 48px;
  min-height: 48px;
  margin-bottom: 14px;
  place-items: center;
  color: var(--vermilion);
  border: 1px solid rgba(183, 53, 42, 0.46);
  border-radius: 50%;
  font-family: var(--display-font);
  font-size: 13px;
  font-weight: 700;
}

.empty-seal {
  color: var(--sage);
  border-color: rgba(84, 120, 94, 0.48);
  font-size: 20px;
}

.state-message h3 {
  margin: 0;
  color: var(--ink);
  font-family: var(--display-font);
  font-size: 20px;
}

.state-message p {
  max-width: 470px;
  margin: 7px 0 18px;
  color: var(--text-regular);
}

.ledger-table-wrap {
  overflow-x: auto;
}

.ledger-table {
  width: 100%;
  min-width: 810px;
  border-radius: 0;
}

.ledger-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.ledger-table :deep(th.el-table__cell) {
  height: 48px;
  padding-inline: 10px;
}

.ledger-table :deep(td.el-table__cell) {
  height: 62px;
  padding-inline: 10px;
}

.ledger-id {
  color: var(--text-regular);
  font-size: 13px;
  white-space: nowrap;
}

.ledger-matter {
  color: var(--ink);
  font-weight: 700;
}

.status-stamp {
  display: inline-flex;
  min-height: 30px;
  align-items: center;
  padding: 3px 9px;
  background: var(--surface);
  border: 1px solid currentColor;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.status-urgent {
  color: var(--vermilion);
  background: var(--el-color-danger-light-9);
}

.status-pending {
  color: var(--blue);
  background: var(--primary-bg);
}

.mobile-ledger {
  display: none;
}

.metrics-heading {
  min-height: 58px;
}

.metrics-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.metric {
  min-width: 0;
  padding: 19px 20px 17px;
  border-right: 1px solid var(--border-light);
}

.metric:last-child {
  border-right: 0;
}

.metric-label {
  color: var(--text-regular);
  font-size: 12px;
  font-weight: 600;
}

.metric-value {
  display: block;
  margin: 6px 0 5px;
  color: var(--ink);
  font-size: clamp(24px, 2.5vw, 31px);
  line-height: 1.2;
}

.metric.urgent .metric-value,
.metric.urgent .metric-foot > span {
  color: var(--vermilion);
}

.metric-foot {
  display: flex;
  min-height: 28px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--text-regular);
  font-size: 10px;
}

.sparkline {
  width: 68px;
  height: 22px;
  flex: 0 0 68px;
  color: var(--text-muted);
}

.metric.urgent .sparkline {
  color: var(--vermilion);
}

.sparkline path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1.5;
  stroke-dasharray: 3 3;
}

.sparkline circle {
  fill: var(--surface);
  stroke: currentColor;
  stroke-width: 1.5;
}

.trend-note {
  margin: 0;
  padding: 8px 20px 11px;
  color: var(--text-regular);
  border-top: 1px solid var(--border-light);
  font-size: 10px;
}

.right-rail {
  overflow: hidden;
}

.rail-section {
  padding: 20px;
  border-bottom: 1px solid var(--border-light);
}

.rail-section:last-child {
  border-bottom: 0;
}

.activity-list {
  margin: 15px 0 0;
  padding: 0;
  list-style: none;
}

.activity-list li {
  position: relative;
  padding: 0 0 16px 18px;
}

.activity-list li:not(:last-child)::after {
  position: absolute;
  top: 12px;
  bottom: 0;
  left: 3px;
  width: 1px;
  background: var(--border-light);
  content: '';
}

.activity-dot {
  position: absolute;
  top: 6px;
  left: 0;
  z-index: 1;
  width: 7px;
  height: 7px;
  background: var(--blue);
  border-radius: 50%;
}

.activity-dot.urgent {
  background: var(--vermilion);
}

.activity-list p {
  margin: 0;
  color: var(--text-regular);
  font-size: 12px;
  line-height: 1.55;
}

.activity-source {
  display: block;
  margin-top: 2px;
  color: var(--text-regular);
  font-family: var(--latin-font);
  font-size: 10px;
}

.rail-empty {
  margin-top: 14px;
  padding: 16px;
  color: var(--text-regular);
  background: var(--paper);
  border: 1px dashed var(--line);
  border-radius: 8px;
  text-align: center;
}

.quick-links {
  display: grid;
  gap: 2px;
  margin-top: 10px;
}

.quick-links button {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 9px;
  padding: 8px 4px;
  color: var(--blue);
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
  text-align: left;
}

.quick-links button:last-child {
  border-bottom: 0;
}

.quick-links button:hover {
  color: var(--ink);
  background: var(--paper);
}

.overdue-summary {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 13px;
  background: var(--paper);
}

.overdue-value {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  color: var(--text-regular);
  border: 1px solid var(--line);
  border-radius: 50%;
  font-family: var(--display-font);
  font-size: 22px;
}

.overdue-summary p {
  margin: 4px 0 0;
  color: var(--text-regular);
  font-size: 11px;
  line-height: 1.55;
}

@media (max-width: 1180px) {
  .dashboard-grid {
    grid-template-columns: minmax(0, 1fr) 270px;
  }

  .metrics-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric:nth-child(2) {
    border-right: 0;
  }

  .metric:nth-child(-n + 2) {
    border-bottom: 1px solid var(--border-light);
  }
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .primary-column {
    display: contents;
  }

  .ledger-panel {
    order: 1;
  }

  .right-rail {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    order: 2;
  }

  .metrics-panel {
    order: 3;
  }

  .rail-section {
    border-right: 1px solid var(--border-light);
    border-bottom: 0;
  }

  .rail-section:last-child {
    border-right: 0;
  }
}

@media (max-width: 700px) {
  .dashboard-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .updated-at {
    width: 100%;
  }

  .header-actions :deep(.el-button) {
    flex: 1;
    margin-left: 0;
  }

  .ledger-table-wrap {
    display: none;
  }

  .mobile-ledger {
    display: grid;
    gap: 0;
  }

  .mobile-ledger-item {
    padding: 17px;
    border-bottom: 1px solid var(--border-light);
  }

  .mobile-ledger-item:last-child {
    border-bottom: 0;
  }

  .mobile-ledger-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .mobile-ledger-item h3 {
    margin: 10px 0;
    color: var(--ink);
    font-size: 15px;
  }

  .mobile-ledger-item dl {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    margin: 0 0 14px;
  }

  .mobile-ledger-item dl div {
    padding: 8px 10px;
    background: var(--paper);
    border-radius: 6px;
  }

  .mobile-ledger-item dt {
    color: var(--text-regular);
    font-size: 10px;
  }

  .mobile-ledger-item dd {
    margin: 2px 0 0;
    color: var(--text-regular);
    font-size: 12px;
  }

  .mobile-ledger-item :deep(.el-button) {
    width: 100%;
  }

  .metrics-strip {
    grid-template-columns: 1fr;
  }

  .metric,
  .metric:nth-child(2) {
    border-right: 0;
    border-bottom: 1px solid var(--border-light);
  }

  .metric:last-child {
    border-bottom: 0;
  }

  .right-rail {
    grid-template-columns: 1fr;
  }

  .rail-section {
    border-right: 0;
    border-bottom: 1px solid var(--border-light);
  }

  .panel-heading,
  .metrics-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }
}
</style>
