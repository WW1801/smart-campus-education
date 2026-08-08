<!-- 排课查询页面组件，负责处理排课模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><Calendar /></el-icon>
        </div>
        <div>
          <div class="page-header-title">课表查询</div>
          <div class="page-header-desc">按教师、班级或学生查询课表</div>
        </div>
      </div>
    </div>

    <el-card v-loading="loading">
      <div class="search-bar">
        <el-select v-model="searchForm.semesterId" placeholder="选择学期" style="width: 220px">
          <el-option
            v-for="semester in semesterList"
            :key="semester.semesterId"
            :label="semester.name"
            :value="semester.semesterId"
          />
        </el-select>

        <el-select v-model="searchForm.queryType" placeholder="查询类型" style="width: 140px" @change="handleQueryTypeChange">
          <el-option label="按教师" value="teacher" />
          <el-option label="按班级" value="class" />
          <el-option label="按学生" value="student" />
        </el-select>

        <el-select
          v-if="searchForm.queryType === 'teacher'"
          v-model="searchForm.teacherId"
          placeholder="选择教师"
          filterable
          style="width: 180px"
        >
          <el-option v-for="teacher in teacherOptions" :key="teacher.teacherId" :label="teacher.name" :value="teacher.teacherId" />
        </el-select>

        <el-select
          v-if="searchForm.queryType === 'class'"
          v-model="searchForm.classId"
          placeholder="选择班级"
          filterable
          style="width: 180px"
        >
          <el-option v-for="item in classOptions" :key="item.classId" :label="item.name" :value="item.classId" />
        </el-select>

        <el-select
          v-if="searchForm.queryType === 'student'"
          v-model="searchForm.studentId"
          placeholder="选择学生"
          filterable
          style="width: 180px"
        >
          <el-option v-for="student in studentOptions" :key="student.studentId" :label="student.name" :value="student.studentId" />
        </el-select>

        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <PageErrorState v-if="listError" :retrying="loading" title="课表加载失败" @retry="search" />
      <div v-else-if="scheduleData.length > 0" class="schedule-grid-wrapper">
        <div class="schedule-grid">
          <div class="grid-header">
            <div class="grid-cell header-cell">节次/星期</div>
            <div v-for="day in weekDays" :key="day.value" class="grid-cell header-cell">{{ day.label }}</div>
          </div>

          <div v-for="period in periodGroups" :key="period.start" class="grid-row">
            <div class="grid-cell period-cell">{{ period.label }}</div>
            <div v-for="day in weekDays" :key="`${day.value}-${period.start}`" class="grid-cell content-cell">
              <div v-for="course in getCourseByDayAndPeriod(day.value, period.start)" :key="course.scheduleId" class="course-card">
                <div class="course-name">{{ course.courseName }}</div>
                <div class="course-detail">{{ course.teacherName }}</div>
                <div class="course-detail">{{ course.classroomName }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else :description="searched ? '未查询到课表' : '暂无课表数据'" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Calendar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useStore } from 'vuex'
import request from '../../utils/request'
import appData from '../../config/appData.json'

const store = useStore()

const loading = ref(false)
const listError = ref(false)
const searched = ref(false)
const scheduleData = ref([])
const semesterList = ref([])
const teacherList = ref([])
const classList = ref([])
const studentList = ref([])
const courseList = ref([])
const classroomList = ref([])

const searchForm = reactive({
  semesterId: '',
  queryType: 'teacher',
  teacherId: '',
  classId: '',
  studentId: ''
})

const weekDays = appData.schedule.weekDays
const periodGroups = appData.schedule.periodGroups

const currentRoleId = computed(() => store.state.user?.roleId || '')
const relatedId = computed(() => store.state.user?.relatedId || '')

const teacherOptions = computed(() => teacherList.value)
const classOptions = computed(() => classList.value)
const studentOptions = computed(() => studentList.value)

const courseMap = computed(() => Object.fromEntries(courseList.value.map(item => [item.courseId, item.name])))
const teacherMap = computed(() => Object.fromEntries(teacherList.value.map(item => [item.teacherId, item.name])))
const classroomMap = computed(() => Object.fromEntries(classroomList.value.map(item => [item.classroomId, item.name])))

// 获取默认查询类型
const getDefaultQueryType = () => {
  if (currentRoleId.value === '5') {
    return 'student'
  }
  if (currentRoleId.value === '4') {
    return 'teacher'
  }
  return 'teacher'
}

// 选取默认学期编号
const pickDefaultSemesterId = () => {
  const activeSemester = semesterList.value.find(item => item.status === 'active')
  return activeSemester?.semesterId || semesterList.value[0]?.semesterId || ''
}

// 查找默认选项编号
const findDefaultOptionId = (type, preferredId = '') => {
  if (type === 'teacher') {
    if (preferredId && teacherList.value.some(item => item.teacherId === preferredId)) {
      return preferredId
    }
    return teacherList.value[0]?.teacherId || ''
  }

  if (type === 'class') {
    return classList.value[0]?.classId || ''
  }

  if (preferredId && studentList.value.some(item => item.studentId === preferredId)) {
    return preferredId
  }
  return studentList.value[0]?.studentId || ''
}

const applyDefaultTarget = queryType => {
  if (queryType === 'teacher') {
    searchForm.teacherId = findDefaultOptionId('teacher', currentRoleId.value === '4' ? relatedId.value : '')
    searchForm.classId = ''
    searchForm.studentId = ''
    return
  }

  if (queryType === 'class') {
    searchForm.classId = findDefaultOptionId('class')
    searchForm.teacherId = ''
    searchForm.studentId = ''
    return
  }

  searchForm.studentId = findDefaultOptionId('student', currentRoleId.value === '5' ? relatedId.value : '')
  searchForm.teacherId = ''
  searchForm.classId = ''
}

const enrichSchedules = rows =>
  (rows || []).map(item => ({
    ...item,
    courseName: courseMap.value[item.courseId] || item.courseName || item.courseId,
    teacherName: teacherMap.value[item.teacherId] || item.teacherName || item.teacherId,
    classroomName: classroomMap.value[item.classroomId] || item.classroomName || item.classroomId || '-'
  }))

// 加载基础数据
const loadLookupData = async () => {
  const [semesterRes, teacherRes, classRes, studentRes, courseRes, classroomRes] = await Promise.all([
    request.get('/semester/list').catch(() => ({ data: [] })),
    request.get('/teacher/list').catch(() => ({ data: [] })),
    request.get('/class/list').catch(() => ({ data: [] })),
    request.get('/student/list').catch(() => ({ data: [] })),
    request.get('/course/list').catch(() => ({ data: [] })),
    request.get('/classroom/list').catch(() => ({ data: [] }))
  ])

  semesterList.value = semesterRes.data || []
  teacherList.value = teacherRes.data || []
  classList.value = classRes.data || []
  studentList.value = studentRes.data || []
  courseList.value = courseRes.data || []
  classroomList.value = classroomRes.data || []
}

// 按条件查询课表
const search = async () => {
  if (!searchForm.semesterId) {
    ElMessage.warning('请选择学期')
    return
  }

  if (searchForm.queryType === 'teacher' && !searchForm.teacherId) {
    ElMessage.warning('请选择教师')
    return
  }

  if (searchForm.queryType === 'class' && !searchForm.classId) {
    ElMessage.warning('请选择班级')
    return
  }

  if (searchForm.queryType === 'student' && !searchForm.studentId) {
    ElMessage.warning('请选择学生')
    return
  }

  loading.value = true
  listError.value = false
  try {
    let res
    if (searchForm.queryType === 'teacher') {
      res = await request.get('/schedule/query/by-teacher', {
        params: {
          teacherId: searchForm.teacherId,
          semesterId: searchForm.semesterId
        },
        skipErrorMessage: true
      })
    } else if (searchForm.queryType === 'class') {
      res = await request.get('/schedule/query/by-class', {
        params: {
          classId: searchForm.classId,
          semesterId: searchForm.semesterId
        },
        skipErrorMessage: true
      })
    } else {
      res = await request.get('/schedule/query/by-student', {
        params: {
          studentId: searchForm.studentId,
          semesterId: searchForm.semesterId
        },
        skipErrorMessage: true
      })
    }

    scheduleData.value = enrichSchedules(res.data || [])
    searched.value = true
  } catch {
    scheduleData.value = []
    searched.value = true
    listError.value = true
  } finally {
    loading.value = false
  }
}

const handleQueryTypeChange = value => {
  applyDefaultTarget(value)
}

// 获取课程按日期and节次
const getCourseByDayAndPeriod = (dayOfWeek, startPeriod) =>
  scheduleData.value.filter(item => item.dayOfWeek === dayOfWeek && item.startPeriod === startPeriod)

// 页面挂载时初始化课表数据
onMounted(async () => {
  loading.value = true
  try {
    await loadLookupData()
    searchForm.queryType = getDefaultQueryType()
    searchForm.semesterId = pickDefaultSemesterId()
    applyDefaultTarget(searchForm.queryType)
    if (searchForm.semesterId) {
      await search()
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-container {
  width: 100%;
}

.schedule-grid-wrapper {
  margin-top: 16px;
  overflow-x: auto;
}

.schedule-grid {
  display: grid;
  grid-template-columns: 88px repeat(7, 1fr);
  gap: 1px;
  background: var(--border-color);
  border-radius: var(--radius-sm);
  overflow: hidden;
  min-width: 860px;
}

.grid-header,
.grid-row {
  display: contents;
}

.grid-cell {
  background: var(--bg-card);
  padding: 10px 8px;
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.header-cell {
  background: var(--bg-soft);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  min-height: 46px;
}

.period-cell {
  background: var(--paper);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.content-cell {
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  padding: 6px;
}

.course-card {
  width: 100%;
  margin-bottom: 4px;
  padding: 6px 8px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: var(--bg-soft);
}

.course-name {
  margin-bottom: 2px;
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 600;
}

.course-detail {
  color: var(--text-secondary);
  font-size: 11px;
}
</style>
