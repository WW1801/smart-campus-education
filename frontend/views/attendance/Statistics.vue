<!-- 考勤统计页面组件，负责处理考勤模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><DataAnalysis /></el-icon>
        </div>
        <div>
          <div class="page-header-title">考勤统计</div>
          <div class="page-header-desc">考勤数据分析与趋势</div>
        </div>
      </div>
    </div>

    <PageErrorState v-if="loadError" :retrying="loading" title="考勤统计加载失败" @retry="loadStatistics" />

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--sage);">
          <el-icon :size="24"><Check /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.presentRate }}%</div>
          <div class="stat-label">出勤率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--text-muted);">
          <el-icon :size="24"><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.lateRate }}%</div>
          <div class="stat-label">迟到率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--vermilion);">
          <el-icon :size="24"><Close /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.absentRate }}%</div>
          <div class="stat-label">缺勤率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: var(--ink);">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.leaveRate }}%</div>
          <div class="stat-label">请假率</div>
        </div>
      </div>
    </div>

    <el-row :gutter="20" class="chart-grid">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card>
          <template #header>
            <span class="chart-title">考勤状态分布</span>
          </template>
          <div
            v-if="hasAttendanceData"
            ref="pieChartRef"
            class="chart-canvas"
            role="img"
            aria-label="考勤状态分布图"
            :aria-description="attendanceSummary"
          ></div>
          <el-empty v-else description="当前范围暂无考勤统计数据" :image-size="72" />
          <table class="visually-hidden">
            <caption>考勤状态分布</caption>
            <thead><tr><th>状态</th><th>比例</th></tr></thead>
            <tbody><tr v-for="item in attendanceRows" :key="item.name"><td>{{ item.name }}</td><td>{{ item.value }}%</td></tr></tbody>
          </table>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12">
        <el-card>
          <template #header>
            <span class="chart-title">近7天出勤趋势</span>
          </template>
          <div
            v-if="trendData.length"
            ref="trendChartRef"
            class="chart-canvas"
            role="img"
            aria-label="近7天出勤趋势图"
            :aria-description="trendSummary"
          ></div>
          <el-empty v-else description="接口暂未返回近7天时序数据" :image-size="72" />
          <table class="visually-hidden">
            <caption>近7天出勤趋势</caption>
            <thead><tr><th>日期</th><th>出勤率</th></tr></thead>
            <tbody><tr v-for="(item, index) in trendRows" :key="item.label"><td>{{ item.label }}</td><td>{{ item.value }}%</td></tr></tbody>
          </table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { DataAnalysis, Check, Warning, Close, Document } from '@element-plus/icons-vue'
import { init, use } from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { AriaComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import request from '../../utils/request'
import { getChartTheme } from '../../utils/chartTheme'

use([LineChart, PieChart, AriaComponent, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const pieChartRef = ref(null)
const trendChartRef = ref(null)
let pieChart = null
let trendChart = null

const stats = ref({ presentRate: 0, lateRate: 0, absentRate: 0, leaveRate: 0 })
const trendData = ref([])
const loading = ref(false)
const loadError = ref(false)
const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const attendanceRows = computed(() => [
  { name: '出勤', value: stats.value.presentRate },
  { name: '迟到', value: stats.value.lateRate },
  { name: '缺勤', value: stats.value.absentRate },
  { name: '请假', value: stats.value.leaveRate }
])
const hasAttendanceData = computed(() => attendanceRows.value.some(item => Number(item.value) > 0))
const attendanceSummary = computed(() => attendanceRows.value.map(item => `${item.name}${item.value}%`).join('；'))
const trendRows = computed(() => trendData.value.map((item, index) => ({
  label: item.label || item.date || weekDays[index] || `第${index + 1}天`,
  value: item.rate ?? 0
})))
const trendSummary = computed(() => trendRows.value.map(item => `${item.label}${item.value}%`).join('；'))

// 加载统计
const loadStatistics = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await request.get('/attendance/statistics', { skipErrorMessage: true })
    if (res?.data) {
      const data = res.data
      const total = data.totalClasses || 1
      stats.value = {
        presentRate: ((data.presentCount || 0) / total * 100).toFixed(1),
        lateRate: ((data.lateCount || 0) / total * 100).toFixed(1),
        absentRate: ((data.absentCount || 0) / total * 100).toFixed(1),
        leaveRate: ((data.leaveCount || 0) / total * 100).toFixed(1)
      }
      if (data.trend) {
        trendData.value = data.trend
      }
    }
  } catch {
    stats.value = { presentRate: 0, lateRate: 0, absentRate: 0, leaveRate: 0 }
    trendData.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
  nextTick(() => {
    initPieChart()
    initTrendChart()
  })
}

// 初始化饼图图表
const initPieChart = () => {
  if (!pieChartRef.value) return
  pieChart?.dispose()
  pieChart = init(pieChartRef.value)
  const theme = getChartTheme()
  pieChart.setOption({
    aria: { enabled: true, decal: { show: true } },
    tooltip: { trigger: 'item' },
    legend: { bottom: '5%', left: 'center', textStyle: { color: theme.text } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: theme.surface, borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: [
        { value: parseFloat(stats.value.presentRate) || 0, name: '出勤', itemStyle: { color: theme.sage } },
        { value: parseFloat(stats.value.lateRate) || 0, name: '迟到', itemStyle: { color: theme.text } },
        { value: parseFloat(stats.value.absentRate) || 0, name: '缺勤', itemStyle: { color: theme.vermilion } },
        { value: parseFloat(stats.value.leaveRate) || 0, name: '请假', itemStyle: { color: theme.ink } }
      ]
    }]
  })
}

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart?.dispose()
  trendChart = init(trendChartRef.value)
  const theme = getChartTheme()
  trendChart.setOption({
    aria: { enabled: true, decal: { show: true } },
    tooltip: { trigger: 'axis' },
    legend: { data: ['出勤率'], textStyle: { color: theme.text } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: trendRows.value.map(item => item.label), axisLabel: { color: theme.text }, axisLine: { lineStyle: { color: theme.line } } },
    yAxis: { type: 'value', min: 0, max: 100, axisLabel: { color: theme.text, formatter: '{value}%' }, splitLine: { lineStyle: { color: theme.line } } },
    series: [{
      name: '出勤率',
      type: 'line',
      data: trendRows.value.map(item => item.value),
      smooth: false,
      lineStyle: { color: theme.blue, width: 3 },
      itemStyle: { color: theme.blue }
    }]
  })
}

// 处理resize
const handleResize = () => { pieChart?.resize(); trendChart?.resize() }

// 页面挂载时初始化考勤数据
onMounted(() => {
  loadStatistics()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.page-container { width: 100%; }

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

.stat-card:hover { box-shadow: var(--shadow-card); }

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

.stat-value { font-size: 24px; font-weight: 700; color: var(--text-primary); }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }
.chart-title { font-weight: 600; font-size: 15px; color: var(--text-primary); }

.chart-canvas { width: 100%; height: 350px; }

@media (max-width: 768px) {
  .stats-cards { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .chart-grid :deep(.el-col + .el-col) { margin-top: 16px; }
}

@media (max-width: 480px) {
  .stats-cards { grid-template-columns: 1fr; }
}
</style>
