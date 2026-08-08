<!-- 选课主页面组件，负责处理选课模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><List /></el-icon>
        </div>
        <div>
          <div class="page-header-title">选课管理</div>
          <div class="page-header-desc">在线选课、退选与个人课表</div>
        </div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="selection-tabs">
      <el-tab-pane label="可选课程" name="available">
        <el-card>
          <div class="search-bar">
            <el-input v-model="searchForm.keyword" placeholder="搜索课程名称" style="width: 200px" clearable>
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="loadAvailable">查询</el-button>
          </div>
          <PageErrorState v-if="availableError" :retrying="loading" title="可选课程加载失败" @retry="loadAvailable" />
          <el-table v-else :data="filteredAvailableList" stripe v-loading="loading">
            <el-table-column prop="courseName" label="课程名称" width="160" />
            <el-table-column prop="teacherName" label="授课教师" width="100" />
            <el-table-column label="上课时间" width="150">
              <template #default="{ row }">周{{ row.dayOfWeek }} 第{{ row.startPeriod }}-{{ row.endPeriod }}节</template>
            </el-table-column>
            <el-table-column prop="credits" label="学分" width="60" />
            <el-table-column label="选课容量" width="100">
              <template #default="{ row }">{{ row.currentStudents }}/{{ row.maxStudents }}</template>
            </el-table-column>
            <el-table-column label="先修课程" width="140">
              <template #default="{ row }">
                <span v-if="row.prerequisites && row.prerequisites.length">{{ row.prerequisites.join(', ') }}</span>
                <span v-else class="muted-value">无</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag
                  :type="row.hasTimeConflict ? 'danger' : row.prerequisitesMet === false ? 'warning' : row.alreadySelected ? 'info' : 'success'"
                  size="small"
                >
                  {{ row.hasTimeConflict ? '冲突' : row.prerequisitesMet === false ? '未满足' : row.alreadySelected ? '已选' : '可选' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  size="small"
                  type="primary"
                  link
                  :disabled="row.hasTimeConflict || row.prerequisitesMet === false || row.alreadySelected || row.currentStudents >= row.maxStudents"
                  @click="selectCourse(row)"
                >选课</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我的选课" name="selected">
        <el-card>
          <div v-if="selectedTotalCredits > 0" style="margin-bottom: 12px;">
            <el-tag type="success" size="large">已选学分：{{ selectedTotalCredits }}</el-tag>
          </div>
          <PageErrorState v-if="selectedError" title="已选课程加载失败" @retry="loadSelected" />
          <el-table v-else :data="selectedList" stripe>
            <el-table-column prop="courseName" label="课程名称" width="160" />
            <el-table-column prop="teacherName" label="授课教师" width="100" />
            <el-table-column label="上课时间" width="150">
              <template #default="{ row }">周{{ row.dayOfWeek }} 第{{ row.startPeriod }}-{{ row.endPeriod }}节</template>
            </el-table-column>
            <el-table-column prop="credits" label="学分" width="60" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'selected' ? 'success' : 'info'" size="small">
                  {{ row.status === 'selected' ? '已选' : '已退' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button v-if="row.status === 'selected'" size="small" type="danger" link @click="dropCourse(row)">退选</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我的课表" name="schedule">
        <el-card>
          <PageErrorState v-if="scheduleError" title="个人课表加载失败" @retry="loadSchedule" />
          <div class="schedule-grid-wrapper" v-else-if="scheduleList.length > 0">
            <div class="schedule-grid">
              <div class="grid-header">
                <div class="grid-cell header-cell">节次\星期</div>
                <div class="grid-cell header-cell" v-for="day in weekDays" :key="day">{{ day }}</div>
              </div>
              <div class="grid-row" v-for="period in periods" :key="period">
                <div class="grid-cell period-cell">{{ period }}-{{ period + 1 }}节</div>
                <div class="grid-cell content-cell" v-for="day in weekDays" :key="`${day}-${period}`">
                  <div class="course-card" v-for="course in getCourseByDayAndPeriod(day, period)" :key="course.scheduleId">
                    <div class="course-name">{{ course.courseName }}</div>
                    <div class="course-detail">{{ course.teacherName }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无课表" />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List, Search } from '@element-plus/icons-vue'
import { useStore } from 'vuex'
import request from '../../utils/request'
import appData from '../../config/appData.json'

const store = useStore()

const activeTab = ref('available')
const loading = ref(false)
const availableError = ref(false)
const selectedError = ref(false)
const scheduleError = ref(false)
const currentSemesterId = ref('')
const searchForm = ref({ keyword: '' })
const availableList = ref([])
const selectedList = ref([])
const scheduleList = ref([])
const weekDays = appData.schedule.weekDays.map(day => day.label)
const periods = appData.schedule.selectionPeriods

const filteredAvailableList = computed(() => {
  if (!searchForm.value.keyword) return availableList.value
  const keyword = searchForm.value.keyword.toLowerCase()
  return availableList.value.filter(item => (item.courseName || '').toLowerCase().includes(keyword))
})

const selectedTotalCredits = computed(() => {
  return selectedList.value
    .filter(item => item.status === 'selected')
    .reduce((sum, item) => sum + (item.credits || 0), 0)
})

// 页面挂载时初始化选课数据
onMounted(async () => {
  await loadSemesterContext()
  await Promise.all([loadAvailable(), loadSelected(), loadSchedule()])
})

// 加载学期上下文
const loadSemesterContext = async () => {
  try {
    const res = await request.get('/semester/list')
    const semesterList = res.data || []
    const currentSemester = semesterList.find(item => item.status === 'current') || semesterList[0]
    currentSemesterId.value = currentSemester?.semesterId || ''
  } catch {
    currentSemesterId.value = ''
  }
}

// 加载可选
const loadAvailable = async () => {
  loading.value = true
  availableError.value = false
  try {
    const res = await request.get('/selection/available', {
      params: {
        studentId: store.state.user?.relatedId,
        semesterId: currentSemesterId.value || undefined
      },
      skipErrorMessage: true
    })
    availableList.value = res.data || []
  } catch {
    availableList.value = []
    availableError.value = true
  } finally {
    loading.value = false
  }
}

// 加载已选课程
const loadSelected = async () => {
  selectedError.value = false
  try {
    const res = await request.get('/selection/my-schedule', {
      params: {
        studentId: store.state.user?.relatedId,
        semesterId: currentSemesterId.value || undefined
      },
      skipErrorMessage: true
    })
    selectedList.value = (res.data || []).map(item => ({ ...item, status: item.status || 'selected' }))
  } catch {
    selectedList.value = []
    selectedError.value = true
  }
}

// 加载课表
const loadSchedule = async () => {
  scheduleError.value = false
  try {
    const res = await request.get('/selection/timetable', {
      params: {
        studentId: store.state.user?.relatedId,
        semesterId: currentSemesterId.value || undefined
      },
      skipErrorMessage: true
    })
    scheduleList.value = res.data || []
  } catch {
    scheduleList.value = []
    scheduleError.value = true
  }
}

// 选择课程
const selectCourse = async (row) => {
  try {
    await ElMessageBox.confirm(`确认选课《${row.courseName}》？`, '选课确认', { type: 'info' })
  } catch {
    return
  }

  try {
    await request.post('/selection/select', { studentId: store.state.user?.relatedId, scheduleId: row.scheduleId })
    ElMessage.success('选课成功')
    await Promise.all([loadAvailable(), loadSelected(), loadSchedule()])
  } catch (error) {
    ElMessage.error(error.message || '选课失败')
  }
}

// 处理退选课程
const dropCourse = async (row) => {
  try {
    await ElMessageBox.confirm('确认退选该课程？', '提示', { type: 'warning' })
  } catch {
    return
  }

  try {
    await request.put(`/selection/${row.selectionId}/drop`)
    ElMessage.success('退选成功')
    await Promise.all([loadSelected(), loadAvailable(), loadSchedule()])
  } catch (error) {
    ElMessage.error(error.message || '退选失败')
  }
}

// 获取课程按日期and节次
const getCourseByDayAndPeriod = (day, period) => {
  const dayMap = Object.fromEntries(appData.schedule.weekDays.map(item => [item.label, item.value]))
  return scheduleList.value.filter(item =>
    item.dayOfWeek === dayMap[day] &&
    item.startPeriod <= period + 1 &&
    item.endPeriod >= period
  )
}
</script>

<style scoped>
.page-container { width: 100%; }

.selection-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.selection-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-color);
}

.schedule-grid-wrapper { margin-top: 16px; overflow-x: auto; }

.schedule-grid {
  display: grid;
  grid-template-columns: 80px repeat(7, 1fr);
  gap: 1px;
  background: var(--border-color);
  border-radius: var(--radius-sm);
  overflow: hidden;
  min-width: 800px;
}

.grid-header { display: contents; }
.grid-row { display: contents; }

.grid-cell {
  background: var(--bg-card);
  padding: 10px 8px;
  text-align: center;
  min-height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-cell { background: var(--bg-soft); font-weight: 600; font-size: 13px; color: var(--text-primary); min-height: 44px; }
.period-cell { font-weight: 600; font-size: 12px; color: var(--text-secondary); background: var(--paper); }
.muted-value { color: var(--text-secondary); }
.content-cell { flex-direction: column; align-items: stretch; justify-content: flex-start; padding: 6px; }

.course-card {
  background: var(--bg-soft);
  border-left: 3px solid var(--primary-color);
  border-radius: 4px;
  padding: 6px 8px;
  margin-bottom: 4px;
  width: 100%;
}

.course-name { font-weight: 600; font-size: 12px; color: var(--primary-color); margin-bottom: 2px; }
.course-detail { font-size: 11px; color: var(--text-secondary); }
</style>
