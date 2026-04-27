<!-- 成绩录入页面组件，负责处理成绩模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #f97316, #fb923c);">
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
          placeholder="选择学期"
          filterable
          clearable
          style="width: 220px"
          @change="handleSemesterChange"
        >
          <el-option
            v-for="item in semesterOptions"
            :key="item.semesterId"
            :label="item.name"
            :value="item.semesterId"
          />
        </el-select>
        <el-select
          v-model="searchForm.courseId"
          placeholder="选择课程"
          filterable
          clearable
          style="width: 220px"
          :disabled="courseOptions.length === 0"
          @change="loadData"
        >
          <el-option
            v-for="item in courseOptions"
            :key="item.courseId"
            :label="item.label"
            :value="item.courseId"
          />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading">
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
              v-if="row.status === 'pending' || row.status === 'rejected'"
              v-model="row.usualScore"
              :min="0"
              :max="100"
              :precision="1"
              size="small"
              controls-position="right"
            />
            <span v-else>{{ row.usualScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="考试成绩" width="130">
          <template #default="{ row }">
            <el-input-number
              v-if="row.status === 'pending' || row.status === 'rejected'"
              v-model="row.examScore"
              :min="0"
              :max="100"
              :precision="1"
              size="small"
              controls-position="right"
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
            <el-tag :type="row.status === 'pending' ? 'warning' : row.status === 'approved' ? 'success' : 'danger'" size="small">
              {{ row.status === 'pending' ? '待提交' : row.status === 'approved' ? '已通过' : '已驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending' || row.status === 'rejected'"
              size="small"
              type="primary"
              link
              @click="submitGrade(row)"
            >提交</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="action-bar">
        <el-button type="primary" @click="batchSubmit">批量提交</el-button>
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
const tableData = ref([])
const searchForm = ref({ courseId: '', semesterId: '' })
const semesterOptions = ref([])
const courseOptions = ref([])
const courseNameMap = ref({})

const currentTeacherId = computed(() => store.state.user?.relatedId || '')

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

// 排序学期列表
const sortSemesters = (items) =>
  [...items].sort((left, right) => {
    if (left.status === 'current') return -1
    if (right.status === 'current') return 1
    return String(right.startDate || '').localeCompare(String(left.startDate || ''))
  })

// 选取默认学期编号
const pickDefaultSemesterId = (items) => items.find(item => item.status === 'current')?.semesterId || items[0]?.semesterId || ''

// 构建课程选项
const buildCourseOptions = (schedules) => {
  const optionMap = new Map()

  schedules.forEach(item => {
    if (!item?.courseId) {
      return
    }

    const existing = optionMap.get(item.courseId)
    if (!existing) {
      optionMap.set(item.courseId, {
        courseId: item.courseId,
        teacherId: item.teacherId || '',
        ambiguousTeacher: false
      })
      return
    }

    if (existing.teacherId && item.teacherId && existing.teacherId !== item.teacherId) {
      existing.teacherId = ''
      existing.ambiguousTeacher = true
    }
  })

  return Array.from(optionMap.values()).map(item => ({
    ...item,
    label: `${courseNameMap.value[item.courseId] || item.courseId} (${item.courseId})${item.ambiguousTeacher ? ' · 多教师' : ''}`
  }))
}

// 加载过滤选项
const loadFilterOptions = async () => {
  try {
    const [semesterRes, courseRes] = await Promise.all([
      request.get('/semester/list').catch(() => ({ data: [] })),
      request.get('/course/list').catch(() => ({ data: [] }))
    ])

    semesterOptions.value = sortSemesters(semesterRes.data || [])
    courseNameMap.value = Object.fromEntries((courseRes.data || []).map(item => [item.courseId, item.name]))
    searchForm.value.semesterId = pickDefaultSemesterId(semesterOptions.value)
    await loadCourseOptions()
    await loadData()
  } catch {
    semesterOptions.value = []
    courseOptions.value = []
    tableData.value = []
  }
}

// 加载课程选项
const loadCourseOptions = async () => {
  const semesterId = searchForm.value.semesterId
  courseOptions.value = []
  searchForm.value.courseId = ''

  if (!semesterId) {
    return
  }

  try {
    const res = currentTeacherId.value
      ? await request.get('/schedule/query/by-teacher', {
        params: {
          teacherId: currentTeacherId.value,
          semesterId
        }
      })
      : await request.get('/schedule/list', { params: { semesterId } })

    courseOptions.value = buildCourseOptions(res.data || [])
    if (courseOptions.value.length > 0) {
      searchForm.value.courseId = courseOptions.value[0].courseId
    }
  } catch {
    courseOptions.value = []
  }
}

// 处理学期变化
const handleSemesterChange = async () => {
  await loadCourseOptions()
  await loadData()
}

// 加载数据
const loadData = async () => {
  if (!searchForm.value.courseId || !searchForm.value.semesterId) {
    tableData.value = []
    return
  }

  loading.value = true
  try {
    const res = await request.get('/grade/roster', {
      params: {
        ...searchForm.value,
        teacherId: store.state.user?.relatedId || undefined
      }
    })
    tableData.value = res.data || []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

// 解析教师编号
const resolveTeacherId = (row) => {
  if (row.teacherId) {
    return row.teacherId
  }

  const matchedCourse = courseOptions.value.find(item => item.courseId === row.courseId)
  if (matchedCourse?.teacherId) {
    return matchedCourse.teacherId
  }

  return currentTeacherId.value
}

// 处理提交成绩
const submitGrade = async (row) => {
  if (row.usualScore == null && row.examScore == null) {
    ElMessage.warning('平时成绩和考试成绩至少填写一项')
    return
  }

  const teacherId = resolveTeacherId(row)
  if (!teacherId) {
    ElMessage.warning('当前课程存在多个授课教师，请使用教师账号录入成绩')
    return
  }

  try {
    const payload = {
      studentId: row.studentId,
      courseId: row.courseId,
      semesterId: row.semesterId,
      teacherId,
      usualScore: row.usualScore,
      examScore: row.examScore
    }

    if (row.gradeId) {
      payload.gradeId = row.gradeId
      payload.status = 'pending'
      await request.put('/grade', payload)
    } else {
      await request.post('/grade', payload)
    }

    ElMessage.success('成绩提交成功')
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  }
}

// 批量提交
const batchSubmit = async () => {
  const pendingData = tableData.value.filter(item =>
    (item.status === 'pending' || item.status === 'rejected') &&
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

  try {
    const payload = pendingData.map(item => ({
      gradeId: item.gradeId,
      studentId: item.studentId,
      courseId: item.courseId,
      semesterId: item.semesterId,
      teacherId: resolveTeacherId(item),
      usualScore: item.usualScore,
      examScore: item.examScore,
      status: 'pending'
    }))
    await request.post('/grade/batch', payload)
    ElMessage.success('批量提交成功')
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '批量提交失败')
  }
}
</script>

<style scoped>
.page-container { width: 100%; }
</style>
