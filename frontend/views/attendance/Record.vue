<!-- 考勤记录页面组件，负责处理考勤模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><Timer /></el-icon>
        </div>
        <div>
          <div class="page-header-title">考勤记录</div>
          <div class="page-header-desc">按课程和日期登记学生考勤</div>
        </div>
      </div>
      <el-button v-if="rosterMode" type="success" :loading="batchSaving" @click="saveRosterAttendance">
        批量保存考勤
      </el-button>
      <el-button type="primary" @click="addRecord">
        <el-icon><Plus /></el-icon>
        添加记录
      </el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchForm.studentNo" placeholder="搜索学号" style="width: 170px" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select
          v-model="currentSemesterId"
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
        >
          <el-option
            v-for="item in courseOptions"
            :key="item.courseId"
            :label="item.label"
            :value="item.courseId"
          />
        </el-select>
        <el-date-picker v-model="searchForm.date" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 160px" />
        <el-select v-model="searchForm.status" placeholder="考勤状态" clearable style="width: 120px">
          <el-option label="出勤" value="present" />
          <el-option label="迟到" value="late" />
          <el-option label="早退" value="early" />
          <el-option label="缺勤" value="absent" />
          <el-option label="请假" value="leave" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <PageErrorState v-if="listError" :retrying="loading" title="考勤名单加载失败" @retry="loadData" />
      <el-table v-else ref="attendanceTableRef" :data="tableData" stripe highlight-current-row v-loading="loading" :row-class-name="attendanceRowClassName">
        <el-table-column prop="studentNo" label="学号" min-width="150">
          <template #default="{ row }">{{ displayStudentNo(row) }}</template>
        </el-table-column>
        <el-table-column prop="studentName" label="姓名" width="100">
          <template #default="{ row }">
            <span>{{ row.studentName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseId" label="课程ID" width="100" />
        <el-table-column prop="courseName" label="课程名称" width="140" />
        <el-table-column prop="date" label="日期" width="110" />
        <el-table-column prop="status" label="考勤状态" width="140">
          <template #default="{ row }">
            <el-select v-if="rosterMode" v-model="row.status" size="small" placeholder="选择状态">
              <el-option label="出勤" value="present" />
              <el-option label="迟到" value="late" />
              <el-option label="早退" value="early" />
              <el-option label="缺勤" value="absent" />
              <el-option label="请假" value="leave" />
            </el-select>
            <el-tag v-else-if="row.status" :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="row.attendanceId" size="small" type="primary" link @click="editRecord(row)">编辑</el-button>
            <el-button v-else size="small" type="primary" link @click="registerRecord(row)">登记</el-button>
            <el-button v-if="row.attendanceId" size="small" type="danger" link @click="deleteRecord(row.attendanceId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!listError && attendanceFailures.length" class="attendance-failures" aria-live="polite">
        <div class="attendance-failures-title">以下记录未保存，修改状态后可直接再次批量保存</div>
        <el-table :data="attendanceFailures" size="small">
          <el-table-column prop="studentNo" label="学号" min-width="150"><template #default="{ row }">{{ displayStudentNo(row) }}</template></el-table-column>
          <el-table-column prop="studentName" label="姓名" min-width="100"><template #default="{ row }">{{ row.studentName || '-' }}</template></el-table-column>
          <el-table-column prop="reason" label="失败原因" min-width="180" show-overflow-tooltip />
          <el-table-column prop="field" label="需修改字段" min-width="110"><template #default="{ row }">{{ attendanceFieldLabel(row.field) }}</template></el-table-column>
          <el-table-column prop="suggestion" label="修改建议" min-width="240" show-overflow-tooltip />
          <el-table-column label="操作" width="90"><template #default="{ row }"><el-button type="primary" link @click="locateFailedStudent(row.studentId)">定位</el-button></template></el-table-column>
        </el-table>
      </div>

      <el-pagination
        v-if="!listError && !rosterMode"
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :total="page.total"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
      />
    </el-card>

    <el-dialog :title="editMode ? '编辑考勤' : '登记考勤'" v-model="dialogVisible" width="520px">
      <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="90px">
        <el-form-item label="学生" prop="studentId">
          <el-select
            v-model="recordForm.studentId"
            filterable
            remote
            clearable
            reserve-keyword
            :remote-method="searchStudents"
            :loading="studentLoading"
            :disabled="teacherRosterRequired && !teacherRosterContextReady"
            :placeholder="studentSelectPlaceholder"
            :no-data-text="studentNoDataText"
            style="width: 100%"
            @change="recordFormRef?.validateField('studentId')"
          >
            <el-option v-for="student in studentOptions" :key="student.studentId" :label="studentOptionLabel(student)" :value="student.studentId">
              <div class="student-option">
                <span class="student-option-name">{{ student.name || '姓名未维护' }}</span>
                <span class="student-option-meta">{{ student.studentNo || student.studentId }}<template v-if="student.className"> · {{ student.className }}</template></span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedStudent" label="学生信息">
          <div class="selected-student" aria-live="polite">
            <strong>{{ selectedStudent.name || '姓名未维护' }}</strong>
            <span>学号：{{ selectedStudent.studentNo || selectedStudent.studentId }}</span>
            <span>班级：{{ selectedStudent.className || '未维护' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="课程ID" prop="courseId">
          <el-select
            v-model="recordForm.courseId"
            placeholder="请选择课程"
            filterable
            clearable
            style="width: 100%"
            :disabled="courseOptions.length === 0"
            @change="handleTeacherCourseChange"
          >
            <el-option
              v-for="item in courseOptions"
              :key="item.courseId"
              :label="item.label"
              :value="item.courseId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学期ID" prop="semesterId">
          <el-select
            v-model="recordForm.semesterId"
            placeholder="请选择学期"
            filterable
            clearable
            style="width: 100%"
            @change="handleTeacherCourseChange"
          >
            <el-option
              v-for="item in semesterOptions"
              :key="item.semesterId"
              :label="item.name"
              :value="item.semesterId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="recordForm.date" type="date" placeholder="请选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="考勤状态" prop="status">
          <el-select v-model="recordForm.status" style="width: 100%">
            <el-option label="出勤" value="present" />
            <el-option label="迟到" value="late" />
            <el-option label="早退" value="early" />
            <el-option label="缺勤" value="absent" />
            <el-option label="请假" value="leave" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRecord">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer, Plus, Search } from '@element-plus/icons-vue'
import { useStore } from 'vuex'
import request from '../../utils/request'

const store = useStore()
const loading = ref(false)
const listError = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const editMode = ref(false)
const recordFormRef = ref(null)
const recordForm = ref({ attendanceId: '', studentId: '', courseId: '', semesterId: '', date: '', status: '' })
const semesterOptions = ref([])
const courseOptions = ref([])
const studentOptions = ref([])
const studentLoading = ref(false)
const studentSearchFailed = ref(false)
const courseNameMap = ref({})
const classNameMap = ref({})
const currentSemesterId = ref('')
const attendanceTableRef = ref(null)
const attendanceFailures = ref([])
const batchSaving = ref(false)

const searchForm = reactive({ studentNo: '', courseId: '', date: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const rosterMode = computed(() => !!searchForm.courseId && !!searchForm.date)
const currentTeacherId = computed(() => store.state.user?.relatedId || '')
const teacherRosterRequired = computed(() => String(store.state.user?.roleId || '') === '4')
const teacherRosterContextReady = computed(() => !!recordForm.value.courseId && !!recordForm.value.semesterId)
const studentSelectPlaceholder = computed(() => teacherRosterRequired.value && !teacherRosterContextReady.value
  ? '请先选择课程和学期后加载花名册'
  : '输入姓名或学号搜索')
const studentNoDataText = computed(() => {
  if (teacherRosterRequired.value && !teacherRosterContextReady.value) return '请先选择课程和学期'
  return studentSearchFailed.value ? '学生列表加载失败，请重新输入后重试' : '未找到匹配学生，请核对姓名或学号'
})
const selectedStudent = computed(() => studentOptions.value.find(item => item.studentId === recordForm.value.studentId) || null)
const failedStudentIds = computed(() => new Set(attendanceFailures.value.map(item => item.studentId)))
const attendanceRowClassName = ({ row }) => failedStudentIds.value.has(row.studentId) ? 'attendance-failed-row' : ''

const locateFailedStudent = studentId => {
  const row = tableData.value.find(item => item.studentId === studentId)
  if (!row) { ElMessage.warning('当前花名册中未找到该学生，请重新查询课程和日期'); return }
  attendanceTableRef.value?.setCurrentRow(row)
  const rowIndex = tableData.value.indexOf(row)
  attendanceTableRef.value?.scrollTo?.({ top: Math.max(0, rowIndex * 48 - 48), behavior: 'smooth' })
}

const recordRules = {
  studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
  courseId: [{ required: true, message: '请输入课程ID', trigger: 'blur' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择考勤状态', trigger: 'change' }]
}

const statusLabel = value => ({ present: '出勤', late: '迟到', early: '早退', early_leave: '早退', absent: '缺勤', leave: '请假' }[value] || value)
const statusTagType = value => ({ present: 'success', late: 'warning', early: 'warning', early_leave: 'warning', absent: 'danger', leave: 'info' }[value] || 'info')
const attendanceFieldLabel = value => ({ studentId: '学生', courseId: '课程', semesterId: '学期', date: '日期', status: '考勤状态', teacherId: '授课权限', records: '提交记录' }[value] || '记录内容')
const displayStudentNo = row => row.studentNo || '无法生成：请在学生管理中补全入学年份、院系和专业后重试'
const studentOptionLabel = student => `${student.name || '姓名未维护'}（${student.studentNo || student.studentId}）`

let studentSearchSequence = 0
const normalizeStudent = student => ({
  ...student,
  className: student.className || classNameMap.value[student.classId] || ''
})

const normalizeRosterStudent = student => normalizeStudent({
  ...student,
  name: student.studentName || student.name || ''
})

const mergeStudents = groups => {
  const merged = new Map(studentOptions.value
    .filter(item => item.studentId === recordForm.value.studentId)
    .map(item => [item.studentId, item]))
  groups.flat().forEach(student => {
    if (student?.studentId) merged.set(student.studentId, normalizeStudent(student))
  })
  return [...merged.values()]
}

// 教师只能从本人课程花名册选择学生；其他角色可按姓名、学号或内部编号检索。
const searchStudents = async keyword => {
  const sequence = ++studentSearchSequence
  const value = String(keyword || '').trim()
  studentLoading.value = true
  studentSearchFailed.value = false
  try {
    if (teacherRosterRequired.value) {
      if (!teacherRosterContextReady.value) {
        if (sequence === studentSearchSequence) studentOptions.value = []
        return
      }
      const response = await request.get('/attendance/roster', {
        params: {
          courseId: recordForm.value.courseId,
          semesterId: recordForm.value.semesterId
        }
      })
      if (sequence === studentSearchSequence) {
        const students = (response.data || [])
          .map(normalizeRosterStudent)
          .filter(student => !value || [student.name, student.studentNo, student.studentId]
            .some(item => String(item || '').includes(value)))
        studentOptions.value = mergeStudents([students])
      }
      return
    }

    const common = { current: 1, size: 20, status: 'active' }
    const queries = value
      ? [{ name: value }, { studentNo: value }, { studentId: value }]
      : [{}]
    const responses = await Promise.all(queries.map(params => request.get('/student/page', {
      params: { ...common, ...params }
    }).then(response => ({ response, failed: false }))
      .catch(() => ({ response: { data: { records: [] } }, failed: true }))))
    if (sequence === studentSearchSequence) {
      studentSearchFailed.value = responses.every(item => item.failed)
      studentOptions.value = mergeStudents(responses.map(item => item.response.data?.records || []))
    }
  } finally {
    if (sequence === studentSearchSequence) studentLoading.value = false
  }
}

const ensureStudentOption = async studentId => {
  if (!studentId) return
  if (teacherRosterRequired.value) return
  try {
    const response = await request.get('/student/page', { params: { current: 1, size: 1, studentId } })
    studentOptions.value = mergeStudents([response.data?.records || []])
  } catch {
    studentOptions.value = mergeStudents([[{ studentId, name: '姓名加载失败' }]])
  }
}

const handleTeacherCourseChange = async () => {
  if (!teacherRosterRequired.value) return
  recordForm.value.studentId = ''
  studentOptions.value = []
  await searchStudents('')
}

// 页面挂载时初始化考勤数据
onMounted(async () => {
  await loadLookupData()
  await loadData()
})

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
  const seen = new Set()
  return (schedules || []).reduce((result, item) => {
    if (!item?.courseId || seen.has(item.courseId)) {
      return result
    }

    seen.add(item.courseId)
    result.push({
      courseId: item.courseId,
      label: `${courseNameMap.value[item.courseId] || item.courseId} (${item.courseId})`
    })
    return result
  }, [])
}

// 加载基础数据
const loadLookupData = async () => {
  try {
    const [semesterRes, courseRes, classRes] = await Promise.all([
      request.get('/semester/list').catch(() => ({ data: [] })),
      request.get('/course/list').catch(() => ({ data: [] })),
      request.get('/class/list').catch(() => ({ data: [] }))
    ])

    semesterOptions.value = sortSemesters(semesterRes.data || [])
    courseNameMap.value = Object.fromEntries((courseRes.data || []).map(item => [item.courseId, item.name]))
    classNameMap.value = Object.fromEntries((classRes.data || []).map(item => [item.classId, item.name]))
    currentSemesterId.value = pickDefaultSemesterId(semesterOptions.value)
    await Promise.all([loadCourseOptions(), searchStudents('')])
  } catch {
    semesterOptions.value = []
    courseOptions.value = []
    currentSemesterId.value = ''
  }
}

// 加载课程选项
const loadCourseOptions = async () => {
  courseOptions.value = []

  if (!currentSemesterId.value) {
    searchForm.courseId = ''
    return
  }

  try {
    const res = currentTeacherId.value
      ? await request.get('/schedule/query/by-teacher', {
        params: {
          teacherId: currentTeacherId.value,
          semesterId: currentSemesterId.value
        }
      })
      : await request.get('/schedule/list', { params: { semesterId: currentSemesterId.value } })

    courseOptions.value = buildCourseOptions(res.data || [])
    if (!courseOptions.value.some(item => item.courseId === searchForm.courseId)) {
      searchForm.courseId = courseOptions.value[0]?.courseId || ''
    }
  } catch {
    courseOptions.value = []
    searchForm.courseId = ''
  }
}

// 处理学期变化
const handleSemesterChange = async () => {
  await loadCourseOptions()
  if (dialogVisible.value && !editMode.value) {
    recordForm.value.semesterId = currentSemesterId.value
    if (!recordForm.value.courseId) {
      recordForm.value.courseId = searchForm.courseId
    }
  }
  await loadData()
}

// 重置查询条件
const resetSearch = () => {
  Object.assign(searchForm, { studentNo: '', courseId: '', date: '', status: '' })
  page.current = 1
  loadData()
}

// 加载数据
const loadData = async () => {
  loading.value = true
  listError.value = false
  try {
    if (rosterMode.value) {
      const res = await request.get('/attendance/roster', {
        params: {
          courseId: searchForm.courseId,
          semesterId: currentSemesterId.value || undefined,
          date: searchForm.date,
          teacherId: store.state.user?.relatedId || undefined
        },
        skipErrorMessage: true
      })
      tableData.value = res.data || []
      page.total = tableData.value.length
      return
    }

    const params = { page: page.current, limit: page.size }
    Object.keys(searchForm).forEach(key => {
      if (searchForm[key] !== '') {
        params[key] = searchForm[key]
      }
    })
    const res = await request.get('/attendance/list', { params, skipErrorMessage: true })
    tableData.value = res.data?.records || []
    page.total = res.data?.total || 0
  } catch {
    tableData.value = []
    page.total = 0
    listError.value = true
  } finally {
    loading.value = false
  }
}

// 添加记录
const addRecord = async () => {
  editMode.value = false
  recordForm.value = {
    attendanceId: '',
    studentId: '',
    courseId: searchForm.courseId || courseOptions.value[0]?.courseId || '',
    semesterId: currentSemesterId.value || '',
    date: searchForm.date || '',
    status: ''
  }
  await searchStudents('')
  dialogVisible.value = true
}

// 处理注册记录
const registerRecord = async (row) => {
  editMode.value = false
  recordForm.value = {
    attendanceId: '',
    studentId: row.studentId || '',
    courseId: row.courseId || '',
    semesterId: row.semesterId || currentSemesterId.value || '',
    date: row.date || searchForm.date || '',
    status: row.status || ''
  }
  studentOptions.value = mergeStudents([[{
    studentId: row.studentId,
    studentNo: row.studentNo,
    name: row.studentName,
    classId: row.classId,
    className: row.className
  }]])
  await ensureStudentOption(row.studentId)
  dialogVisible.value = true
}

// 编辑记录
const editRecord = async (row) => {
  editMode.value = true
  recordForm.value = { ...row }
  studentOptions.value = mergeStudents([[{
    studentId: row.studentId,
    studentNo: row.studentNo,
    name: row.studentName,
    classId: row.classId,
    className: row.className
  }]])
  await ensureStudentOption(row.studentId)
  dialogVisible.value = true
}

// 删除记录
const deleteRecord = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该考勤记录？', '提示', { type: 'warning' })
    await request.delete(`/attendance/${id}`)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 保存记录
const saveRecord = async () => {
  if (!recordFormRef.value) return
  try {
    await recordFormRef.value.validate()
  } catch {
    return
  }

  try {
    const payload = {
      ...recordForm.value,
      semesterId: recordForm.value.semesterId || currentSemesterId.value
    }

    if (editMode.value && recordForm.value.attendanceId) {
      await request.put('/attendance', payload)
    } else {
      await request.post('/attendance', payload)
    }
    dialogVisible.value = false
    ElMessage.success(editMode.value ? '更新成功' : '登记成功')
    await loadData()
  } catch {
    ElMessage.error('保存失败')
  }
}

const saveRosterAttendance = async () => {
  if (!rosterMode.value || !currentSemesterId.value) {
    ElMessage.warning('请先选择学期、课程和日期')
    return
  }
  const missingStatus = tableData.value.find(item => !item.status)
  if (missingStatus) {
    ElMessage.warning(`请完成学号 ${displayStudentNo(missingStatus)}（${missingStatus.studentName || '姓名未维护'}）的考勤状态`)
    return
  }

  const editedStatus = new Map(tableData.value.map(item => [item.studentId, item.status]))
  batchSaving.value = true
  try {
    const res = await request.post('/attendance/batch-save', {
      courseId: searchForm.courseId,
      semesterId: currentSemesterId.value,
      date: searchForm.date,
      records: tableData.value.map(item => ({
        studentId: item.studentId,
        status: item.status
      }))
    })
    const data = res.data || {}
    const failureCount = Number(data.failedCount || 0)
    attendanceFailures.value = Array.isArray(data.failureDetails) ? data.failureDetails : []
    await loadData()
    if (failureCount > 0) {
      tableData.value.forEach(item => {
        if (failedStudentIds.value.has(item.studentId) && editedStatus.has(item.studentId)) item.status = editedStatus.get(item.studentId)
      })
      ElMessage.warning(`已保存 ${data.successCount || 0} 条，${failureCount} 条失败；失败项的编辑状态已保留`)
      locateFailedStudent(attendanceFailures.value[0]?.studentId)
    } else {
      ElMessage.success(`已保存 ${data.successCount || tableData.value.length} 条考勤记录`)
      attendanceFailures.value = []
    }
  } catch (error) {
    ElMessage.error(error?.message || '批量保存失败，当前编辑内容已保留，请稍后重试')
  } finally {
    batchSaving.value = false
  }
}
</script>

<style scoped>
.page-container { width: 100%; }
.attendance-failures { margin-top: 18px; }
.attendance-failures-title { margin-bottom: 10px; color: var(--el-color-danger); font-weight: 600; }
:deep(.attendance-failed-row > td.el-table__cell) { background: var(--el-color-danger-light-9) !important; }
.student-option { display: flex; align-items: center; justify-content: space-between; gap: 16px; min-width: 0; }
.student-option-name { flex: 0 1 auto; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 600; }
.student-option-meta { flex: 1 1 auto; overflow: hidden; text-align: right; text-overflow: ellipsis; white-space: nowrap; color: var(--el-text-color-secondary); font-size: 12px; }
.selected-student { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 16px; width: 100%; min-width: 0; padding: 9px 12px; border-radius: 8px; background: var(--el-fill-color-light); color: var(--el-text-color-regular); overflow-wrap: anywhere; }
.selected-student strong { color: var(--el-text-color-primary); }
</style>
