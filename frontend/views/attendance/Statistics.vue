<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #06b6d4, #22d3ee);">
          <el-icon :size="22"><DataAnalysis /></el-icon>
        </div>
        <div>
          <div class="page-header-title">考勤统计</div>
          <div class="page-header-desc">考勤数据分析与趋势</div>
        </div>
      </div>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #2ec4b6, #3dd5c6);">
          <el-icon :size="24"><Check /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.presentRate }}%</div>
          <div class="stat-label">出勤率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f59e0b, #fbbf24);">
          <el-icon :size="24"><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.lateRate }}%</div>
          <div class="stat-label">迟到率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #ef4444, #f87171);">
          <el-icon :size="24"><Close /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.absentRate }}%</div>
          <div class="stat-label">缺勤率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #8b5cf6, #a78bfa);">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.leaveRate }}%</div>
          <div class="stat-label">请假率</div>
        </div>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="chart-title">考勤状态分布</span>
          </template>
          <div ref="pieChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="chart-title">近7天出勤趋势</span>
          </template>
          <div ref="trendChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { DataAnalysis, Check, Warning, Close, Document } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '../../utils/request'

const pieChartRef = ref(null)
const trendChartRef = ref(null)
let pieChart = null
let trendChart = null

const stats = ref({ presentRate: 0, lateRate: 0, absentRate: 0, leaveRate: 0 })
const trendData = ref([])

const loadStatistics = async () => {
  try {
    const res = await request.get('/attendance/statistics')
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
  } catch {}
  nextTick(() => {
    initPieChart()
    initTrendChart()
  })
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: '5%', left: 'center', textStyle: { color: '#8d99ae' } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: [
        { value: parseFloat(stats.value.presentRate) || 0, name: '出勤', itemStyle: { color: '#2ec4b6' } },
        { value: parseFloat(stats.value.lateRate) || 0, name: '迟到', itemStyle: { color: '#f59e0b' } },
        { value: parseFloat(stats.value.absentRate) || 0, name: '缺勤', itemStyle: { color: '#ef4444' } },
        { value: parseFloat(stats.value.leaveRate) || 0, name: '请假', itemStyle: { color: '#8b5cf6' } }
      ]
    }]
  })
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const trendValues = trendData.value.length > 0
    ? trendData.value.map(d => d.rate)
    : [0, 0, 0, 0, 0, 0, 0]
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['出勤率'], textStyle: { color: '#8d99ae' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: days, axisLabel: { color: '#8d99ae' }, axisLine: { lineStyle: { color: '#e8ecf1' } } },
    yAxis: { type: 'value', min: 0, max: 100, axisLabel: { color: '#8d99ae', formatter: '{value}%' }, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    series: [{
      name: '出勤率',
      type: 'line',
      data: trendValues,
      smooth: true,
      lineStyle: { color: '#4361ee', width: 3 },
      itemStyle: { color: '#4361ee' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(67, 97, 238, 0.3)' },
          { offset: 1, color: 'rgba(67, 97, 238, 0.02)' }
        ])
      }
    }]
  })
}

const handleResize = () => { pieChart?.resize(); trendChart?.resize() }

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
  transition: transform 0.2s ease;
}

.stat-card:hover { transform: translateY(-2px); }

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-value { font-size: 24px; font-weight: 700; color: var(--text-primary); }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }
.chart-title { font-weight: 600; font-size: 15px; color: var(--text-primary); }
</style>
