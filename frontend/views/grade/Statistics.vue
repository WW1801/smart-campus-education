<!-- 成绩统计页面组件，负责处理成绩模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #6366f1, #818cf8);">
          <el-icon :size="22"><DataAnalysis /></el-icon>
        </div>
        <div>
          <div class="page-header-title">成绩统计</div>
          <div class="page-header-desc">成绩分布与趋势分析</div>
        </div>
      </div>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #4361ee, #6b83f2);">
          <el-icon :size="24"><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalStudents }}</div>
          <div class="stat-label">参考人数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #2ec4b6, #3dd5c6);">
          <el-icon :size="24"><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.avgScore }}</div>
          <div class="stat-label">平均分</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f59e0b, #fbbf24);">
          <el-icon :size="24"><Trophy /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.passRate }}</div>
          <div class="stat-label">及格率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #8b5cf6, #a78bfa);">
          <el-icon :size="24"><Star /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.excellentRate }}</div>
          <div class="stat-label">优秀率</div>
        </div>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="chart-title">成绩分布</span>
          </template>
          <div ref="distributionChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="chart-title">各课程平均分</span>
          </template>
          <div ref="courseAvgChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { User, DataAnalysis, TrendCharts, Trophy, Star } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '../../utils/request'

const distributionChartRef = ref(null)
const courseAvgChartRef = ref(null)
let distributionChart = null
let courseAvgChart = null

const stats = ref({ totalStudents: 0, avgScore: 0, passRate: '0%', excellentRate: '0%' })

const distributionData = ref([
  { range: '0-59', count: 0 }, { range: '60-69', count: 0 },
  { range: '70-79', count: 0 }, { range: '80-89', count: 0 },
  { range: '90-100', count: 0 }
])

const courseAvgData = ref([])

// 加载统计
const loadStatistics = async () => {
  try {
    const res = await request.get('/grade/statistics')
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
  } catch {}
  nextTick(() => {
    initDistributionChart()
    initCourseAvgChart()
  })
}

// 初始化分布图表
const initDistributionChart = () => {
  if (!distributionChartRef.value) return
  distributionChart = echarts.init(distributionChartRef.value)
  distributionChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: distributionData.value.map(d => d.range), axisLabel: { color: '#8d99ae' }, axisLine: { lineStyle: { color: '#e8ecf1' } } },
    yAxis: { type: 'value', axisLabel: { color: '#8d99ae' }, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    series: [{
      type: 'bar',
      data: distributionData.value.map(d => d.count),
      barWidth: '50%',
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#4361ee' }, { offset: 1, color: '#6b83f2' }
        ])
      }
    }]
  })
}

// 初始化课程平均图表
const initCourseAvgChart = () => {
  if (!courseAvgChartRef.value) return
  courseAvgChart = echarts.init(courseAvgChartRef.value)
  courseAvgChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: courseAvgData.value.map(d => d.name), axisLabel: { color: '#8d99ae', rotate: 15 }, axisLine: { lineStyle: { color: '#e8ecf1' } } },
    yAxis: { type: 'value', min: 50, max: 100, axisLabel: { color: '#8d99ae' }, splitLine: { lineStyle: { color: '#f0f2f5' } } },
    series: [{
      type: 'bar',
      data: courseAvgData.value.map(d => d.avg),
      barWidth: '40%',
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        // 处理颜色
        color: (params) => {
          const colors = [['#2ec4b6', '#3dd5c6'], ['#4361ee', '#6b83f2'], ['#f59e0b', '#fbbf24'], ['#8b5cf6', '#a78bfa'], ['#ec4899', '#f472b6']]
          return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: colors[params.dataIndex % colors.length][0] },
            { offset: 1, color: colors[params.dataIndex % colors.length][1] }
          ])
        }
      }
    }]
  })
}

// 处理resize
const handleResize = () => {
  distributionChart?.resize()
  courseAvgChart?.resize()
}

// 页面挂载时初始化成绩数据
onMounted(() => {
  loadStatistics()
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

.stat-card:hover {
  transform: translateY(-2px);
}

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
</style>
