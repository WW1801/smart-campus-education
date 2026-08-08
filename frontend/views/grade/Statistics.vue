<!-- 成绩统计页面组件，负责处理成绩模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><DataAnalysis /></el-icon>
        </div>
        <div>
          <div class="page-header-title">成绩统计</div>
          <div class="page-header-desc">成绩分布与趋势分析</div>
        </div>
      </div>
    </div>

    <el-card class="filter-card">
      <div class="search-bar">
        <el-select v-model="filters.semesterId" clearable placeholder="全部学期" style="width: 200px">
          <el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.name" :value="item.semesterId" />
        </el-select>
        <el-select v-model="filters.courseId" clearable filterable placeholder="全部课程" style="width: 200px">
          <el-option v-for="item in courseOptions" :key="item.courseId" :label="`${item.name}（${item.code || item.courseId}）`" :value="item.courseId" />
        </el-select>
        <el-select v-model="filters.classId" clearable filterable placeholder="全部班级" style="width: 180px">
          <el-option v-for="item in classOptions" :key="item.classId" :label="item.name" :value="item.classId" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="loadStatistics">查询</el-button>
        <el-button :disabled="loading" @click="resetFilters">重置</el-button>
        <el-button :loading="loading" @click="loadStatistics">刷新数据</el-button>
      </div>
    </el-card>

    <PageErrorState v-if="loadError" :retrying="loading" title="成绩统计加载失败" @retry="loadStatistics" />

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--blue);">
          <el-icon :size="24"><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalStudents }}</div>
          <div class="stat-label">参考人数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--sage);">
          <el-icon :size="24"><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.avgScore }}</div>
          <div class="stat-label">平均分</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--vermilion);">
          <el-icon :size="24"><Trophy /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.passRate }}</div>
          <div class="stat-label">及格率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--ink);">
          <el-icon :size="24"><Star /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.excellentRate }}</div>
          <div class="stat-label">优秀率</div>
        </div>
      </div>
    </div>

    <el-row :gutter="20" class="chart-grid">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card>
          <template #header>
            <span class="chart-title">成绩分布</span>
          </template>
          <div
            v-if="hasGradeData"
            ref="distributionChartRef"
            class="chart-canvas"
            role="img"
            aria-label="成绩分布柱状图"
            :aria-description="distributionSummary"
          ></div>
          <el-empty v-else description="当前筛选范围暂无成绩分布数据" :image-size="72" />
          <table class="visually-hidden">
            <caption>成绩分布数据</caption>
            <thead><tr><th>分数区间</th><th>人数</th></tr></thead>
            <tbody><tr v-for="item in distributionData" :key="item.range"><td>{{ item.range }}</td><td>{{ item.count }}</td></tr></tbody>
          </table>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12">
        <el-card>
          <template #header>
            <span class="chart-title">各课程平均分</span>
          </template>
          <div
            v-if="courseAvgData.length"
            ref="courseAvgChartRef"
            class="chart-canvas"
            role="img"
            aria-label="各课程平均分柱状图"
            :aria-description="courseAverageSummary"
          ></div>
          <el-empty v-else description="当前筛选范围暂无课程平均分数据" :image-size="72" />
          <table class="visually-hidden">
            <caption>各课程平均分</caption>
            <thead><tr><th>课程</th><th>平均分</th></tr></thead>
            <tbody><tr v-for="item in courseAvgData" :key="item.courseId || item.name"><td>{{ item.name }}</td><td>{{ item.avg }}</td></tr></tbody>
          </table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { User, DataAnalysis, TrendCharts, Trophy, Star } from '@element-plus/icons-vue'
import { init, use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { AriaComponent, GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import request from '../../utils/request'
import { getChartTheme } from '../../utils/chartTheme'

use([BarChart, AriaComponent, GridComponent, TooltipComponent, CanvasRenderer])

const distributionChartRef = ref(null)
const courseAvgChartRef = ref(null)
let distributionChart = null
let courseAvgChart = null

const stats = ref({ totalStudents: 0, avgScore: 0, passRate: '0%', excellentRate: '0%' })
const loading = ref(false)
const loadError = ref(false)
const filters = ref({ semesterId: '', courseId: '', classId: '' })
const semesterOptions = ref([])
const courseOptions = ref([])
const classOptions = ref([])

const distributionData = ref([
  { range: '0-59', count: 0 }, { range: '60-69', count: 0 },
  { range: '70-79', count: 0 }, { range: '80-89', count: 0 },
  { range: '90-100', count: 0 }
])

const courseAvgData = ref([])
const hasGradeData = computed(() => Number(stats.value.totalStudents || 0) > 0)
const distributionSummary = computed(() => distributionData.value.map(item => `${item.range} 分 ${item.count} 人`).join('；'))
const courseAverageSummary = computed(() => courseAvgData.value.map(item => `${item.name}平均 ${item.avg} 分`).join('；'))

// 加载统计
const loadStatistics = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await request.get('/grade/statistics', { params: filters.value, skipErrorMessage: true })
    if (res?.data) {
      const data = res.data
      stats.value = {
        totalStudents: data.total || 0,
        avgScore: data.average || 0,
        passRate: data.passRate || '0%',
        excellentRate: data.excellentRate || '0%'
      }
      if (data.distribution) {
        const d = data.distribution
        distributionData.value = [
          { range: '0-59', count: d.fail || 0 },
          { range: '60-69', count: d.pass || 0 },
          { range: '70-79', count: d.medium || 0 },
          { range: '80-89', count: d.good || 0 },
          { range: '90-100', count: d.excellent || 0 }
        ]
      }
      if (data.courseAvgList) {
        courseAvgData.value = data.courseAvgList
      }
    }
  } catch {
    stats.value = { totalStudents: 0, avgScore: 0, passRate: '0%', excellentRate: '0%' }
    courseAvgData.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
  nextTick(() => {
    initDistributionChart()
    initCourseAvgChart()
  })
}

const loadFilterOptions = async () => {
  const [semesterRes, courseRes, classRes] = await Promise.all([
    request.get('/semester/list').catch(() => ({ data: [] })),
    request.get('/course/list').catch(() => ({ data: [] })),
    request.get('/class/list').catch(() => ({ data: [] }))
  ])
  semesterOptions.value = semesterRes.data || []
  courseOptions.value = courseRes.data || []
  classOptions.value = classRes.data || []
}

const resetFilters = () => {
  filters.value = { semesterId: '', courseId: '', classId: '' }
  loadStatistics()
}

// 初始化分布图表
const initDistributionChart = () => {
  if (!distributionChartRef.value) return
  distributionChart?.dispose()
  distributionChart = init(distributionChartRef.value)
  const theme = getChartTheme()
  distributionChart.setOption({
    aria: { enabled: true, decal: { show: true } },
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: distributionData.value.map(d => d.range), axisLabel: { color: theme.text }, axisLine: { lineStyle: { color: theme.line } } },
    yAxis: { type: 'value', axisLabel: { color: theme.text }, splitLine: { lineStyle: { color: theme.line } } },
    series: [{
      type: 'bar',
      data: distributionData.value.map(d => d.count),
      barWidth: '50%',
      itemStyle: { borderRadius: [6, 6, 0, 0], color: theme.blue }
    }]
  })
}

// 初始化课程平均图表
const initCourseAvgChart = () => {
  if (!courseAvgChartRef.value) return
  courseAvgChart?.dispose()
  courseAvgChart = init(courseAvgChartRef.value)
  const theme = getChartTheme()
  courseAvgChart.setOption({
    aria: { enabled: true, decal: { show: true } },
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: courseAvgData.value.map(d => d.name), axisLabel: { color: theme.text, rotate: 15 }, axisLine: { lineStyle: { color: theme.line } } },
    yAxis: { type: 'value', min: 50, max: 100, axisLabel: { color: theme.text }, splitLine: { lineStyle: { color: theme.line } } },
    series: [{
      type: 'bar',
      data: courseAvgData.value.map(d => d.avg),
      barWidth: '40%',
      itemStyle: { borderRadius: [6, 6, 0, 0], color: theme.blue }
    }]
  })
}

// 处理resize
const handleResize = () => {
  distributionChart?.resize()
  courseAvgChart?.resize()
}

// 页面挂载时初始化成绩数据
onMounted(async () => {
  await loadFilterOptions()
  await loadStatistics()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  distributionChart?.dispose()
  courseAvgChart?.dispose()
})
</script>

<style scoped>
.page-container { width: 100%; }
.filter-card { margin-bottom: 20px; }

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-card);
}

.stat-card:hover {
  box-shadow: var(--shadow-card);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--surface);
  flex-shrink: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.chart-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.chart-canvas {
  width: 100%;
  height: 350px;
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid :deep(.el-col + .el-col) {
    margin-top: 16px;
  }
}

@media (max-width: 480px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>
