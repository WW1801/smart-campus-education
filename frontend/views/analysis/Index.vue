<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><DataAnalysis /></el-icon>
        </div>
        <div>
          <div class="page-header-title">智能教务数据分析 Agent</div>
          <div class="page-header-desc">{{ analysis.summary || '聚合教务数据，生成运行概览和分析洞察' }}</div>
        </div>
      </div>
      <el-button type="primary" :loading="loading" @click="loadAnalysis">刷新分析</el-button>
    </div>

    <el-card class="filter-card">
      <div class="filter-bar">
        <el-select v-model="filters.semesterId" placeholder="学期" clearable style="width: 180px">
          <el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.name" :value="item.semesterId" />
        </el-select>
        <el-select v-model="filters.majorId" placeholder="专业" clearable style="width: 180px" @change="filters.classId = ''">
          <el-option v-for="item in majorOptions" :key="item.majorId" :label="item.name" :value="item.majorId" />
        </el-select>
        <el-select v-model="filters.classId" placeholder="班级" clearable style="width: 180px">
          <el-option v-for="item in filteredClassOptions" :key="item.classId" :label="item.name" :value="item.classId" />
        </el-select>
        <el-button type="primary" @click="loadAnalysis">分析</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <div class="stats-grid" v-loading="loading">
      <div class="stat-card">
        <div class="stat-label">学生总数</div>
        <div class="stat-value">{{ overview.studentCount || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">在读学生</div>
        <div class="stat-value primary">{{ overview.activeStudentCount || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">成绩记录</div>
        <div class="stat-value">{{ overview.gradeRecordCount || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">挂科记录</div>
        <div class="stat-value danger">{{ overview.failedCourseCount || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">考勤记录</div>
        <div class="stat-value">{{ overview.attendanceRecordCount || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">异常考勤</div>
        <div class="stat-value warning">{{ overview.abnormalAttendanceCount || 0 }}</div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card class="section-card">
          <template #header>
            <span class="section-title">Agent 洞察</span>
          </template>
          <el-empty v-if="insights.length === 0" description="暂无分析洞察" />
          <div v-else class="insight-list">
            <div v-for="(item, index) in insights" :key="index" class="insight-item">
              <el-icon><MagicStick /></el-icon>
              <span>{{ item }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card class="section-card">
          <template #header>
            <span class="section-title">风险信号</span>
          </template>
          <el-alert
            :title="riskSignal.message || '暂无明显预警信号'"
            :type="riskSignal.level === 'attention' ? 'warning' : 'success'"
            :closable="false"
            show-icon
          />
          <div class="module-list">
            <div v-for="item in modules" :key="item.code" class="module-item">
              <div class="module-name">{{ item.name }}</div>
              <div class="module-desc">{{ item.description }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :span="12">
        <el-card class="section-card">
          <template #header>
            <span class="section-title">成绩分析</span>
          </template>
          <div class="metric-row">
            <div>平均分：<b>{{ gradeAnalysis.average || 0 }}</b></div>
            <div>通过率：<b>{{ gradeAnalysis.passRate || '0%' }}</b></div>
            <div>优秀率：<b>{{ gradeAnalysis.excellentRate || '0%' }}</b></div>
          </div>
          <div class="distribution">
            <div v-for="item in distributionRows" :key="item.label" class="distribution-row">
              <span>{{ item.label }}</span>
              <el-progress :percentage="item.percent" :stroke-width="10" :show-text="false" />
              <b>{{ item.count }}</b>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="section-card">
          <template #header>
            <span class="section-title">毕业分析</span>
          </template>
          <div class="graduation-grid">
            <div>
              <span>审核学生</span>
              <b>{{ graduationAnalysis.totalStudents || 0 }}</b>
            </div>
            <div>
              <span>审核通过</span>
              <b>{{ graduationAnalysis.approvedCount || 0 }}</b>
            </div>
            <div>
              <span>未通过</span>
              <b>{{ graduationAnalysis.rejectedCount || 0 }}</b>
            </div>
            <div>
              <span>通过率</span>
              <b>{{ graduationAnalysis.approvalRate || '0.00%' }}</b>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card content-row">
      <template #header>
        <span class="section-title">课程均分</span>
      </template>
      <el-empty v-if="courseAvgList.length === 0" description="暂无课程均分数据" />
      <el-table v-else :data="courseAvgList" stripe>
        <el-table-column prop="name" label="课程" min-width="180" />
        <el-table-column prop="avg" label="平均分" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DataAnalysis, MagicStick } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const analysis = ref({})
const semesterOptions = ref([])
const majorOptions = ref([])
const classOptions = ref([])
const filters = reactive({
  semesterId: '',
  majorId: '',
  classId: ''
})

const overview = computed(() => analysis.value.dataOverview || {})
const gradeAnalysis = computed(() => analysis.value.gradeAnalysis || {})
const graduationAnalysis = computed(() => analysis.value.graduationAnalysis || {})
const insights = computed(() => analysis.value.insights || [])
const modules = computed(() => analysis.value.modules || [])
const riskSignal = computed(() => overview.value.riskSignal || {})
const courseAvgList = computed(() => gradeAnalysis.value.courseAvgList || [])

const filteredClassOptions = computed(() => {
  if (!filters.majorId) return classOptions.value
  return classOptions.value.filter(item => item.majorId === filters.majorId)
})

const distributionRows = computed(() => {
  const distribution = gradeAnalysis.value.distribution || {}
  const total = Number(gradeAnalysis.value.total || 0)
  const rows = [
    { label: '优秀 90-100', count: distribution.excellent || 0 },
    { label: '良好 80-89', count: distribution.good || 0 },
    { label: '中等 70-79', count: distribution.medium || 0 },
    { label: '及格 60-69', count: distribution.pass || 0 },
    { label: '不及格 0-59', count: distribution.fail || 0 }
  ]
  return rows.map(item => ({
    ...item,
    percent: total > 0 ? Math.round(item.count / total * 100) : 0
  }))
})

onMounted(async () => {
  await loadOptions()
  await loadAnalysis()
})

const loadOptions = async () => {
  const [semesterRes, majorRes, classRes] = await Promise.all([
    request.get('/semester/list'),
    request.get('/major/list'),
    request.get('/class/list')
  ])
  semesterOptions.value = semesterRes.data || []
  majorOptions.value = majorRes.data || []
  classOptions.value = classRes.data || []
}

const loadAnalysis = async () => {
  loading.value = true
  try {
    const res = await request.get('/agent/analysis', {
      params: {
        semesterId: filters.semesterId || undefined,
        majorId: filters.majorId || undefined,
        classId: filters.classId || undefined
      }
    })
    analysis.value = res.data || {}
  } catch (error) {
    ElMessage.error('数据分析 Agent 加载失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.semesterId = ''
  filters.majorId = ''
  filters.classId = ''
  loadAnalysis()
}
</script>

<style scoped>
.page-container {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--surface);
  background: var(--button-grad);
}

.page-header-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--ink);
}

.page-header-desc {
  margin-top: 4px;
  color: var(--text-secondary);
}

.filter-card,
.content-row {
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  background: var(--surface);
  border-radius: 8px;
  padding: 16px;
  box-shadow: var(--shadow-sm);
}

.stat-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: var(--ink);
}

.stat-value.primary {
  color: var(--blue);
}

.stat-value.danger {
  color: var(--vermilion);
}

.stat-value.warning {
  color: var(--text-secondary);
}

.section-card {
  border-radius: 8px;
}

.section-title {
  font-weight: 700;
  color: var(--ink);
}

.insight-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.insight-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  color: var(--text-secondary);
  line-height: 1.6;
}

.module-list {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.module-item {
  padding: 10px 0;
  border-top: 1px solid var(--border-light);
}

.module-name {
  font-weight: 700;
  color: var(--ink);
}

.module-desc {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 13px;
}

.metric-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.distribution {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.distribution-row {
  display: grid;
  grid-template-columns: 100px 1fr 40px;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
}

.graduation-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.graduation-grid div {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 14px;
}

.graduation-grid span {
  display: block;
  color: var(--text-secondary);
  font-size: 13px;
}

.graduation-grid b {
  display: block;
  margin-top: 8px;
  font-size: 22px;
  color: var(--ink);
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
