<!-- 成绩录入页面组件，负责处理成绩模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><EditPen /></el-icon>
        </div>
        <div>
          <div class="page-header-title">成绩录入</div>
          <div class="page-header-desc">按课程名单录入学生成绩并提交审核</div>
        </div>
      </div>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select
          v-model="searchForm.semesterId"
          placeholder="全部学期"
          filterable
          clearable
          style="width: 220px"
          @change="handleSemesterChange"
        >
          <el-option label="全部学期" value="" />
          <el-option
            v-for="item in semesterOptions"
            :key="item.semesterId"
            :label="item.name"
            :value="item.semesterId"
          />
        </el-select>
        <el-select
          v-model="searchForm.courseContextId"
          :placeholder="coursePlaceholder"
          filterable
          clearable
          style="width: 220px"
          :disabled="courseLoading"
          @change="handleCourseChange"
        >
          <el-option
            v-for="item in courseOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="searchForm.scheduleId"
          :placeholder="classPlaceholder"
          filterable
          clearable
          style="width: 260px"
          :disabled="!searchForm.courseContextId || classLoading"
          @change="handleClassChange"
        >
          <el-option
            v-for="item in classOptions"
            :key="item.scheduleId"
            :label="item.label"
            :value="item.scheduleId"
          />
        </el-select>
        <el-button type="primary" :disabled="!canQuery" @click="handleQuery">查询</el-button>
      </div>

      <el-alert
        v-if="submitError"
        data-testid="grade-submit-error"
        :title="submitError"
        type="error"
        :closable="false"
        show-icon
        class="submit-alert"
      />

      <PageErrorState v-if="listError" :retrying="loading" title="成绩名单加载失败" @retry="loadData" />
      <el-table v-else :data="tableData" stripe v-loading="loading">
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="120">
          <template #default="{ row }">
            <span>{{ row.studentName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseId" label="课程ID" width="100" />
        <el-table-column prop="courseName" label="课程名称" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.courseName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="平时成绩" width="130">
          <template #default="{ row }">
            <el-input-number
              v-if="row.status === 'draft' || row.status === 'rejected'"
              v-model="row.usualScore"
              :min="0"
              :max="100"
              :precision="1"
              size="small"
              controls-position="right"
              :disabled="isRowSubmitting(row) || batchSubmitting"
            />
            <span v-else>{{ row.usualScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="考试成绩" width="130">
          <template #default="{ row }">
            <el-input-number
              v-if="row.status === 'draft' || row.status === 'rejected'"
              v-model="row.examScore"
              :min="0"
              :max="100"
              :precision="1"
              size="small"
              controls-position="right"
              :disabled="isRowSubmitting(row) || batchSubmitting"
            />
            <span v-else>{{ row.examScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总评" width="80">
          <template #default="{ row }">
            <span>{{ calcTotal(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small">
              {{ statusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'draft' || row.status === 'rejected'"
              size="small"
              type="primary"
              link
              :loading="isRowSubmitting(row)"
              :disabled="batchSubmitting"
              @click="submitGrade(row)"
            >提交</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="action-bar">
        <el-button type="primary" :loading="batchSubmitting" :disabled="loading || submittingIds.size > 0" @click="batchSubmit">批量提交</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { useStore } from 'vuex'
import request from '../../utils/request'

const store = useStore()
const loading = ref(false)
const listError = ref(false)
const submitError = ref('')
const submittingIds = ref(new Set())
const batchSubmitting = ref(false)
const tableData = ref([])
const searchForm = ref({ semesterId: '', courseContextId: '', scheduleId: '' })
const semesterOptions = ref([])
const courseOptions = ref([])
const classOptions = ref([])
const courseNameMap = ref({})
const teacherNameMap = ref({})
const classNameMap = ref({})
const majorNameMap = ref({})
const schedules = ref([])
const courseLoading = ref(false)
const classLoading = ref(false)

const currentTeacherId = computed(() => store.state.user?.relatedId || '')
const selectedCourse = computed(() => courseOptions.value.find(item => item.value === searchForm.value.courseContextId))
const selectedSchedule = computed(() => classOptions.value.find(item => item.scheduleId === searchForm.value.scheduleId))
const canQuery = computed(() => Boolean(selectedCourse.value && selectedSchedule.value) && !courseLoading.value && !classLoading.value && !loading.value)
const coursePlaceholder = computed(() => {
  if (courseLoading.value) return '正在加载课程'
  return '请选择课程'
})
const classPlaceholder = computed(() => {
  if (!searchForm.value.courseContextId) return '请选择课程'
  if (classLoading.value) return '正在加载班级'
  return '请选择班级'
})

// 页面挂载时初始化录入数据
onMounted(async () => {
  await loadFilterOptions()
})

// 计算总分
const calcTotal = (row) => {
  if (row.usualScore == null && row.examScore == null) return '-'
  const usual = row.usualScore ?? 0
  const exam = row.examScore ?? 0
  return (usual * 0.3 + exam * 0.7).toFixed(1)
}

const statusMeta = (status) => ({
  draft: { label: '待提交', type: 'info' },
  pending: { label: '已提交（待审核）', type: 'warning' },
  submitted: { label: '已提交（待审核）', type: 'warning' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已驳回', type: 'danger' }
}[status] || { label: '-', type: 'info' })

// 排序学期列表
const sortSemesters = (items) =>
  [...items].sort((left, right) => {
    return String(right.startDate || '').localeCompare(String(left.startDate || ''))
  })

const contextKey = item => `${item.semesterId}|${item.courseId}|${item.teacherId || ''}`
const teacherLabel = teacherId => teacherNameMap.value[teacherId] || teacherId || '未分配教师'
const semesterLabel = semesterId => semesterOptions.value.find(item => item.semesterId === semesterId)?.name || semesterId
const classLabel = classId => classNameMap.value[classId] || '选课班'

// 课程选项以“学期 + 课程 + 教师”为唯一上下文，不能仅按 courseId 合并。
const buildCourseOptions = items => {
  const optionMap = new Map()
  items.filter(item => item?.courseId && item?.semesterId && item?.scheduleId).forEach(item => {
    const value = contextKey(item)
    if (!optionMap.has(value)) {
      optionMap.set(value, {
        value,
        courseId: item.courseId,
        semesterId: item.semesterId,
        teacherId: item.teacherId || '',
        label: `${courseNameMap.value[item.courseId] || item.courseId} (${item.courseId})｜${semesterLabel(item.semesterId)}｜${teacherLabel(item.teacherId)}`
      })
    }
  })
  return Array.from(optionMap.values())
}

const buildClassOptions = items => {
  const labelCounts = new Map()
  const options = items.filter(item => item?.scheduleId).map(item => {
    const label = `${classLabel(item.classId)}｜${teacherLabel(item.teacherId)}`
    labelCounts.set(label, (labelCounts.get(label) || 0) + 1)
    return { ...item, label }
  })
  return options.map(item => labelCounts.get(item.label) > 1
    ? { ...item, label: `${item.label}｜排课${item.scheduleId}` }
    : item)
}

// 加载过滤选项
const loadFilterOptions = async () => {
  try {
    const [semesterRes, courseRes, teacherRes, classRes, majorRes] = await Promise.all([
      request.get('/semester/list').catch(() => ({ data: [] })),
      request.get('/course/list').catch(() => ({ data: [] })),
      request.get('/teacher/list').catch(() => ({ data: [] })),
      request.get('/class/list').catch(() => ({ data: [] })),
      request.get('/major/list').catch(() => ({ data: [] }))
    ])

    semesterOptions.value = sortSemesters(semesterRes.data || [])
    courseNameMap.value = Object.fromEntries((courseRes.data || []).map(item => [item.courseId, item.name]))
    teacherNameMap.value = Object.fromEntries((teacherRes.data || []).map(item => [item.teacherId, item.name]))
    majorNameMap.value = Object.fromEntries((majorRes.data || []).map(item => [item.majorId, item.name]))
    classNameMap.value = Object.fromEntries((classRes.data || []).map(item => [item.classId,
      `${majorNameMap.value[item.majorId] || ''}${item.grade || ''}${item.name || item.classId}`]))
    // 默认“全部学期”，不自动请求成绩名单。
    searchForm.value = { semesterId: '', courseContextId: '', scheduleId: '' }
    courseOptions.value = []
    classOptions.value = []
    schedules.value = []
    tableData.value = []
  } catch {
    semesterOptions.value = []
    courseOptions.value = []
    classOptions.value = []
    schedules.value = []
    tableData.value = []
  }
}

// 加载课程选项
const loadCourseOptions = async () => {
  courseOptions.value = []
  classOptions.value = []
  searchForm.value.courseContextId = ''
  searchForm.value.scheduleId = ''
  tableData.value = []
  listError.value = false
  submitError.value = ''

  courseLoading.value = true
  try {
    const res = currentTeacherId.value
      ? await request.get('/schedule/query/by-teacher', {
        params: {
          teacherId: currentTeacherId.value,
          semesterId: searchForm.value.semesterId || undefined
        }
      })
      : await request.get('/schedule/list', { params: { semesterId: searchForm.value.semesterId || undefined } })

    schedules.value = res.data || []
    courseOptions.value = buildCourseOptions(schedules.value)
    if (!courseOptions.value.length) ElMessage.info('当前筛选范围内暂无可录入成绩的课程')
  } catch {
    courseOptions.value = []
    schedules.value = []
  } finally {
    courseLoading.value = false
  }
}

// 处理学期变化
const handleSemesterChange = async () => {
  await loadCourseOptions()
}

const handleCourseChange = () => {
  searchForm.value.scheduleId = ''
  classOptions.value = []
  tableData.value = []
  listError.value = false
  submitError.value = ''
  const course = selectedCourse.value
  if (!course) return

  classLoading.value = true
  try {
    classOptions.value = buildClassOptions(schedules.value.filter(item => contextKey(item) === course.value))
    if (!classOptions.value.length) ElMessage.info('该课程当前筛选范围内暂无可录入成绩的班级')
  } finally {
    classLoading.value = false
  }
}

const handleClassChange = () => {
  tableData.value = []
  listError.value = false
  submitError.value = ''
}

const handleQuery = async () => {
  if (!searchForm.value.courseContextId) {
    ElMessage.warning('请选择课程')
    return
  }
  if (!searchForm.value.scheduleId) {
    ElMessage.warning('请选择班级')
    return
  }
  await loadData()
}

// 加载数据
const loadData = async () => {
  const schedule = selectedSchedule.value
  if (!selectedCourse.value || !schedule) {
    tableData.value = []
    listError.value = false
    return
  }

  loading.value = true
  listError.value = false
  try {
    const res = await request.get('/grade/roster', {
      params: {
        scheduleId: schedule.scheduleId,
        semesterId: schedule.semesterId,
        courseId: schedule.courseId,
        classId: schedule.classId || undefined,
        teacherId: currentTeacherId.value || undefined
      },
      skipErrorMessage: true
    })
    tableData.value = res.data || []
  } catch {
    tableData.value = []
    listError.value = true
  } finally {
    loading.value = false
  }
}

// 解析教师编号
const resolveTeacherId = (row) => {
  if (row.teacherId) {
    return row.teacherId
  }

  if (selectedSchedule.value?.teacherId) {
    return selectedSchedule.value.teacherId
  }

  return currentTeacherId.value
}

const rowKey = row => row.gradeId || `${row.studentId}-${row.courseId}-${row.semesterId}`
const isRowSubmitting = row => submittingIds.value.has(rowKey(row))
const setRowSubmitting = (row, submitting) => {
  const next = new Set(submittingIds.value)
  if (submitting) next.add(rowKey(row))
  else next.delete(rowKey(row))
  submittingIds.value = next
}
const resolveSubmitError = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback

// 处理提交成绩
const submitGrade = async (row) => {
  submitError.value = ''
  if (row.usualScore == null && row.examScore == null) {
    ElMessage.warning('平时成绩和考试成绩至少填写一项')
    return
  }

  const teacherId = resolveTeacherId(row)
  if (!teacherId) {
    ElMessage.warning('当前课程存在多个授课教师，请使用教师账号录入成绩')
    return
  }

  setRowSubmitting(row, true)
  try {
    const payload = {
      studentId: row.studentId,
      courseId: row.courseId,
      semesterId: row.semesterId,
      teacherId,
      usualScore: row.usualScore,
      examScore: row.examScore
    }

    if (row.gradeId) payload.gradeId = row.gradeId
    await request.post('/grade', payload, { skipErrorMessage: true })

    ElMessage.success('成绩提交成功')
    await loadData()
  } catch (error) {
    submitError.value = resolveSubmitError(error, '提交失败，已保留填写的成绩，请核对后重试。')
    ElMessage.error(submitError.value)
  } finally {
    setRowSubmitting(row, false)
  }
}

// 批量提交
const batchSubmit = async () => {
  submitError.value = ''
  const pendingData = tableData.value.filter(item =>
    (item.status === 'draft' || item.status === 'rejected') &&
    (item.usualScore != null || item.examScore != null)
  )

  if (pendingData.length === 0) {
    ElMessage.warning('没有可提交的成绩记录')
    return
  }

  const missingTeacher = pendingData.find(item => !resolveTeacherId(item))
  if (missingTeacher) {
    ElMessage.warning('存在未确定授课教师的课程，请使用教师账号逐条提交成绩')
    return
  }

  batchSubmitting.value = true
  try {
    const payload = pendingData.map(item => ({
      gradeId: item.gradeId,
      studentId: item.studentId,
      courseId: item.courseId,
      semesterId: item.semesterId,
      teacherId: resolveTeacherId(item),
      usualScore: item.usualScore,
      examScore: item.examScore
    }))
    await request.post('/grade/batch', payload, { skipErrorMessage: true })
    ElMessage.success('批量提交成功')
    await loadData()
  } catch (error) {
    submitError.value = resolveSubmitError(error, '批量提交失败，已保留填写的成绩，请核对后重试。')
    ElMessage.error(submitError.value)
  } finally {
    batchSubmitting.value = false
  }
}
</script>

<style scoped>
.page-container { width: 100%; }
.submit-alert { margin-bottom: 16px; }
</style>
