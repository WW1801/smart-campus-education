<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #8b5cf6, #a78bfa);">
          <el-icon :size="22"><List /></el-icon>
        </div>
        <div>
          <div class="page-header-title">选课管理</div>
          <div class="page-header-desc">在线选课、退选与冲突检测</div>
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
          <el-table :data="filteredAvailableList" stripe v-loading="loading">
            <el-table-column prop="courseName" label="课程名称" width="160" />
            <el-table-column prop="teacherName" label="授课教师" width="100" />
            <el-table-column label="上课时间" width="140">
              <template #default="{ row }">周{{ row.dayOfWeek }} 第{{ row.startPeriod }}-{{ row.endPeriod }}节</template>
            </el-table-column>
            <el-table-column prop="credits" label="学分" width="60" />
            <el-table-column label="选课容量" width="100">
              <template #default="{ row }">{{ row.currentStudents }}/{{ row.maxStudents }}</template>
            </el-table-column>
            <el-table-column label="先修课程" width="120">
              <template #default="{ row }">
                <span v-if="row.prerequisites && row.prerequisites.length">{{ row.prerequisites.join(', ') }}</span>
                <span v-else style="color: #c0c4cc">无</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.hasTimeConflict ? 'danger' : row.prerequisitesMet === false ? 'warning' : row.alreadySelected ? 'info' : 'success'" size="small">
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
          <el-table :data="selectedList" stripe>
            <el-table-column prop="courseName" label="课程名称" width="160" />
            <el-table-column prop="teacherName" label="授课教师" width="100" />
            <el-table-column label="上课时间" width="140">
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
          <div class="schedule-grid-wrapper" v-if="selectedList.filter(s => s.status === 'selected').length > 0">
            <div class="schedule-grid">
              <div class="grid-header">
                <div class="grid-cell header-cell">节次\星期</div>
                <div class="grid-cell header-cell" v-for="day in weekDays" :key="day">{{ day }}</div>
              </div>
              <div class="grid-row" v-for="period in periods" :key="period">
                <div class="grid-cell period-cell">{{ period }}-{{ period + 1 }}节</div>
                <div class="grid-cell content-cell" v-for="day in weekDays" :key="day + period">
                  <div class="course-card" v-for="course in getCourseByDayAndPeriod(day, period)" :key="course.selectionId">
                    <div class="course-name">{{ course.courseName }}</div>
                    <div class="course-detail">{{ course.teacherName }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无已选课程" />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List, Search } from '@element-plus/icons-vue'
import { useStore } from 'vuex'
import request from '../../utils/request'

const store = useStore()

const activeTab = ref('available')
const loading = ref(false)
const searchForm = ref({ keyword: '' })
const availableList = ref([])
const selectedList = ref([])
const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const periods = [1, 3, 5, 7, 9]

const filteredAvailableList = computed(() => {
  if (!searchForm.value.keyword) return availableList.value
  const kw = searchForm.value.keyword.toLowerCase()
  return availableList.value.filter(c => (c.courseName || '').toLowerCase().includes(kw))
})

const selectedTotalCredits = computed(() => {
  return selectedList.value
    .filter(s => s.status === 'selected')
    .reduce((sum, s) => sum + (s.credits || 0), 0)
})

onMounted(() => { loadAvailable(); loadSelected() })

const loadAvailable = async () => {
  loading.value = true
  try {
    const res = await request.get('/selection/available', { params: { studentId: store.state.user?.relatedId } })
    availableList.value = res.data || []
  } catch {
    availableList.value = []
  } finally { loading.value = false }
}

const loadSelected = async () => {
  try {
    const res = await request.get('/selection/my-schedule', { params: { studentId: store.state.user?.relatedId } })
    selectedList.value = (res.data || []).map(s => ({ ...s, status: s.status || 'selected' }))
  } catch {
    selectedList.value = []
  }
}

const selectCourse = async (row) => {
  try {
    await ElMessageBox.confirm(`确认选课「${row.courseName}」？`, '选课确认', { type: 'info' })
  } catch { return }
  try {
    await request.post('/selection/select', { studentId: store.state.user?.relatedId, scheduleId: row.scheduleId })
    ElMessage.success('选课成功')
    loadAvailable()
    loadSelected()
  } catch (e) {
    ElMessage.error(e.message || '选课失败')
  }
}

const dropCourse = async (row) => {
  await ElMessageBox.confirm('确认退选该课程？', '提示', { type: 'warning' })
  try {
    await request.put(`/selection/${row.selectionId}/drop`)
    ElMessage.success('退选成功')
    loadSelected()
    loadAvailable()
  } catch (e) {
    ElMessage.error(e.message || '退选失败')
  }
}

const getCourseByDayAndPeriod = (day, period) => {
  const dayMap = { '周一': 1, '周二': 2, '周三': 3, '周四': 4, '周五': 5, '周六': 6, '周日': 7 }
  return selectedList.value.filter(s => s.status === 'selected' && s.dayOfWeek === dayMap[day] && s.startPeriod === period)
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

.header-cell { background: #f0f4ff; font-weight: 600; font-size: 13px; color: var(--text-primary); min-height: 44px; }
.period-cell { font-weight: 600; font-size: 12px; color: var(--text-secondary); background: #f8f9fc; }
.content-cell { flex-direction: column; align-items: stretch; justify-content: flex-start; padding: 6px; }

.course-card {
  background: linear-gradient(135deg, #eef1ff, #e8ecff);
  border-left: 3px solid var(--primary-color);
  border-radius: 4px;
  padding: 6px 8px;
  margin-bottom: 4px;
  width: 100%;
}

.course-name { font-weight: 600; font-size: 12px; color: var(--primary-color); margin-bottom: 2px; }
.course-detail { font-size: 11px; color: var(--text-secondary); }
</style>
