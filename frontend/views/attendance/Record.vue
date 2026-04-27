<!-- 考勤记录页面组件，负责处理考勤模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #ef4444, #f87171);">
          <el-icon :size="22"><Timer /></el-icon>
        </div>
        <div>
          <div class="page-header-title">考勤记录</div>
          <div class="page-header-desc">按课程和日期登记学生考勤</div>
        </div>
      </div>
      <el-button type="primary" @click="addRecord">
        <el-icon><Plus /></el-icon>
        添加记录
      </el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchForm.studentId" placeholder="搜索学号" style="width: 150px" clearable>
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

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="100">
          <template #default="{ row }">
            <span>{{ row.studentName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseId" label="课程ID" width="100" />
        <el-table-column prop="courseName" label="课程名称" width="140" />
        <el-table-column prop="date" label="日期" width="110" />
        <el-table-column prop="status" label="考勤状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status" :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
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

      <el-pagination
        v-if="!rosterMode"
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
        <el-form-item label="学号" prop="studentId">
          <el-input v-model="recordForm.studentId" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="课程ID" prop="courseId">
          <el-select
            v-model="recordForm.courseId"
            placeholder="请选择课程"
            filterable
            clearable
            style="width: 100%"
            :disabled="courseOptions.length === 0"
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
const tableData = ref([])
const dialogVisible = ref(false)
const editMode = ref(false)
const recordFormRef = ref(null)
const recordForm = ref({ attendanceId: '', studentId: '', courseId: '', semesterId: '', date: '', status: '' })
const semesterOptions = ref([])
const courseOptions = ref([])
const courseNameMap = ref({})
const currentSemesterId = ref('')

const searchForm = reactive({ studentId: '', courseId: '', date: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const rosterMode = computed(() => !!searchForm.courseId && !!searchForm.date)
const currentTeacherId = computed(() => store.state.user?.relatedId || '')

const recordRules = {
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  courseId: [{ required: true, message: '请输入课程ID', trigger: 'blur' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择考勤状态', trigger: 'change' }]
}

const statusLabel = value => ({ present: '出勤', late: '迟到', early: '早退', early_leave: '早退', absent: '缺勤', leave: '请假' }[value] || value)
const statusTagType = value => ({ present: 'success', late: 'warning', early: 'warning', early_leave: 'warning', absent: 'danger', leave: 'info' }[value] || 'info')

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
    const [semesterRes, courseRes] = await Promise.all([
      request.get('/semester/list').catch(() => ({ data: [] })),
      request.get('/course/list').catch(() => ({ data: [] }))
    ])

    semesterOptions.value = sortSemesters(semesterRes.data || [])
    courseNameMap.value = Object.fromEntries((courseRes.data || []).map(item => [item.courseId, item.name]))
    currentSemesterId.value = pickDefaultSemesterId(semesterOptions.value)
    await loadCourseOptions()
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
  Object.assign(searchForm, { studentId: '', courseId: '', date: '', status: '' })
  page.current = 1
  loadData()
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    if (rosterMode.value) {
      const res = await request.get('/attendance/roster', {
        params: {
          courseId: searchForm.courseId,
          semesterId: currentSemesterId.value || undefined,
          date: searchForm.date,
          teacherId: store.state.user?.relatedId || undefined
        }
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
    const res = await request.get('/attendance/list', { params }).catch(() => null)
    if (res?.data) {
      tableData.value = res.data.records || []
      page.total = res.data.total || 0
    } else {
      tableData.value = []
      page.total = 0
    }
  } finally {
    loading.value = false
  }
}

// 添加记录
const addRecord = () => {
  editMode.value = false
  recordForm.value = {
    attendanceId: '',
    studentId: '',
    courseId: searchForm.courseId || courseOptions.value[0]?.courseId || '',
    semesterId: currentSemesterId.value || '',
    date: searchForm.date || '',
    status: ''
  }
  dialogVisible.value = true
}

// 处理注册记录
const registerRecord = (row) => {
  editMode.value = false
  recordForm.value = {
    attendanceId: '',
    studentId: row.studentId || '',
    courseId: row.courseId || '',
    semesterId: row.semesterId || currentSemesterId.value || '',
    date: row.date || searchForm.date || '',
    status: row.status || ''
  }
  dialogVisible.value = true
}

// 编辑记录
const editRecord = (row) => {
  editMode.value = true
  recordForm.value = { ...row }
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
</script>

<style scoped>
.page-container { width: 100%; }
</style>
