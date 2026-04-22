<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #0ea5e9, #38bdf8);">
          <el-icon :size="22"><Calendar /></el-icon>
        </div>
        <div>
          <div class="page-header-title">课表查询</div>
          <div class="page-header-desc">按教师、班级或学生查询课表</div>
        </div>
      </div>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="searchForm.semesterId" placeholder="选择学期" clearable style="width: 220px">
          <el-option v-for="s in semesterList" :key="s.semesterId" :label="s.name" :value="s.semesterId" />
        </el-select>
        <el-select v-model="searchForm.queryType" placeholder="查询类型" style="width: 130px">
          <el-option label="按教师" value="teacher" />
          <el-option label="按班级" value="class" />
          <el-option label="按学生" value="student" />
        </el-select>
        <el-select v-if="searchForm.queryType === 'teacher'" v-model="searchForm.teacherId" placeholder="选择教师" style="width: 160px">
          <el-option v-for="t in teacherList" :key="t.teacherId" :label="t.name" :value="t.teacherId" />
        </el-select>
        <el-select v-if="searchForm.queryType === 'class'" v-model="searchForm.classId" placeholder="选择班级" style="width: 160px">
          <el-option v-for="c in classList" :key="c.classId" :label="c.name" :value="c.classId" />
        </el-select>
        <el-input v-if="searchForm.queryType === 'student'" v-model="searchForm.studentId" placeholder="输入学生ID" style="width: 160px" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <div v-if="scheduleData.length > 0" class="schedule-grid-wrapper">
        <div class="schedule-grid">
          <div class="grid-header">
            <div class="grid-cell header-cell">节次\星期</div>
            <div class="grid-cell header-cell" v-for="day in weekDays" :key="day">{{ day }}</div>
          </div>
          <div class="grid-row" v-for="period in periods" :key="period">
            <div class="grid-cell period-cell">{{ period }}-{{ period + 1 }}节</div>
            <div class="grid-cell content-cell" v-for="day in weekDays" :key="day + period">
              <div class="course-card" v-for="course in getCourseByDayAndPeriod(day, period)" :key="course.id">
                <div class="course-name">{{ course.courseName }}</div>
                <div class="course-detail">{{ course.teacherName }}</div>
                <div class="course-detail">{{ course.classroomName }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="请选择查询条件后查询课表" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Calendar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const searchForm = ref({ semesterId: '', queryType: 'teacher', teacherId: '', classId: '', studentId: '' })
const scheduleData = ref([])
const semesterList = ref([])
const teacherList = ref([])
const classList = ref([])
const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const periods = [1, 3, 5, 7, 9]

const dayOfWeekMap = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }

onMounted(() => {
  loadBasicData()
})

const loadBasicData = async () => {
  const [semRes, teacherRes, classRes] = await Promise.all([
    request.get('/semester/list').catch(() => ({ data: [] })),
    request.get('/teacher/list').catch(() => ({ data: [] })),
    request.get('/class/list').catch(() => ({ data: [] }))
  ])
  semesterList.value = semRes.data || []
  teacherList.value = teacherRes.data || []
  classList.value = classRes.data || []
}

const search = async () => {
  if (!searchForm.value.semesterId) {
    ElMessage.warning('请选择学期')
    return
  }
  const qType = searchForm.value.queryType
  if (qType === 'teacher' && !searchForm.value.teacherId) { ElMessage.warning('请选择教师'); return }
  if (qType === 'class' && !searchForm.value.classId) { ElMessage.warning('请选择班级'); return }
  if (qType === 'student' && !searchForm.value.studentId) { ElMessage.warning('请输入学生ID'); return }

  try {
    let res
    if (qType === 'teacher') {
      res = await request.get('/schedule/query/by-teacher', {
        params: { teacherId: searchForm.value.teacherId, semesterId: searchForm.value.semesterId }
      })
    } else if (qType === 'class') {
      res = await request.get('/schedule/query/by-class', {
        params: { classId: searchForm.value.classId, semesterId: searchForm.value.semesterId }
      })
    } else if (qType === 'student') {
      res = await request.get('/selection/my-schedule', {
        params: { studentId: searchForm.value.studentId, semesterId: searchForm.value.semesterId }
      })
    }

    let data = res?.data || []

    if (qType === 'teacher' || qType === 'class') {
      const courseIds = [...new Set(data.map(d => d.courseId).filter(Boolean))]
      const teacherIds = [...new Set(data.map(d => d.teacherId).filter(Boolean))]
      const classroomIds = [...new Set(data.map(d => d.classroomId).filter(Boolean))]

      const courseMap = {}
      const teacherMap = {}
      const classroomMap = {}

      await Promise.all([
        courseIds.length > 0 ? request.get('/course/list').then(r => (r.data || []).forEach(c => { courseMap[c.courseId] = c.name })).catch(() => {}) : Promise.resolve(),
        teacherIds.length > 0 ? request.get('/teacher/list').then(r => (r.data || []).forEach(t => { teacherMap[t.teacherId] = t.name })).catch(() => {}) : Promise.resolve(),
        classroomIds.length > 0 ? request.get('/classroom/list').then(r => (r.data || []).forEach(c => { classroomMap[c.classroomId] = c.name })).catch(() => {}) : Promise.resolve()
      ])

      data = data.map(d => ({
        ...d,
        courseName: courseMap[d.courseId] || d.courseId,
        teacherName: teacherMap[d.teacherId] || d.teacherId,
        classroomName: classroomMap[d.classroomId] || d.classroomId
      }))
    }

    scheduleData.value = data
  } catch {
    scheduleData.value = []
  }
}

const getCourseByDayAndPeriod = (day, period) => {
  return scheduleData.value.filter(item => {
    const itemDay = dayOfWeekMap[item.dayOfWeek] || item.dayOfWeek
    return itemDay === day && item.startPeriod === period
  })
}
</script>

<style scoped>
.page-container { width: 100%; }

.schedule-grid-wrapper {
  margin-top: 16px;
  overflow-x: auto;
}

.schedule-grid {
  display: grid;
  grid-template-columns: 80px repeat(7, 1fr);
  gap: 1px;
  background: var(--border-color);
  border-radius: var(--radius-sm);
  overflow: hidden;
  min-width: 800px;
}

.grid-header {
  display: contents;
}

.grid-row {
  display: contents;
}

.grid-cell {
  background: var(--bg-card);
  padding: 10px 8px;
  text-align: center;
  min-height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-cell {
  background: #f0f4ff;
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
  min-height: 44px;
}

.period-cell {
  font-weight: 600;
  font-size: 12px;
  color: var(--text-secondary);
  background: #f8f9fc;
}

.content-cell {
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  padding: 6px;
}

.course-card {
  background: linear-gradient(135deg, #eef1ff, #e8ecff);
  border-left: 3px solid var(--primary-color);
  border-radius: 4px;
  padding: 6px 8px;
  margin-bottom: 4px;
  width: 100%;
}

.course-name {
  font-weight: 600;
  font-size: 12px;
  color: var(--primary-color);
  margin-bottom: 2px;
}

.course-detail {
  font-size: 11px;
  color: var(--text-secondary);
}
</style>
