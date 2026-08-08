<!-- 排课排课页面组件，负责处理排课模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><Calendar /></el-icon>
        </div>
        <div>
          <div class="page-header-title">排课安排</div>
          <div class="page-header-desc">管理排课信息并检测冲突</div>
        </div>
      </div>
      <div>
        <el-button type="success" @click="openAutoArrangeDialog">自动排课</el-button>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon>
          新增排课
        </el-button>
      </div>
    </div>

    <PageErrorState v-if="listError" :retrying="loading" title="排课列表加载失败" @retry="loadData" />
    <el-card v-else>
      <div class="search-bar">
        <el-select v-model="searchForm.semesterId" placeholder="选择学期" clearable style="width: 220px">
          <el-option
            v-for="semester in semesterList"
            :key="semester.semesterId"
            :label="semester.name"
            :value="semester.semesterId"
          />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="semesterName" label="学期" min-width="160" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程" min-width="140" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="教师" min-width="120" show-overflow-tooltip />
        <el-table-column prop="className" label="班级" min-width="120" show-overflow-tooltip />
        <el-table-column prop="classroomName" label="教室" min-width="140" show-overflow-tooltip />
        <el-table-column label="排课模式" width="120">
          <template #default="{ row }">
            <el-tag :type="row.mode === 'open_selection' ? 'success' : ''">
              {{ row.mode === 'open_selection' ? '开放选课' : '按班级排课' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">
            {{ formatWeekday(row.dayOfWeek) }} 第{{ row.startPeriod }}-{{ row.endPeriod }}节
          </template>
        </el-table-column>
        <el-table-column label="容量" width="100">
          <template #default="{ row }">
            <span v-if="row.mode === 'open_selection'">{{ row.currentStudents || 0 }}/{{ row.maxStudents || 0 }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" link @click="checkConflict(row)">检测冲突</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :total="page.total"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="排课模式" prop="mode">
              <el-select v-model="form.mode" style="width: 100%">
                <el-option label="按班级排课" value="class_based" />
                <el-option label="开放选课" value="open_selection" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学期" prop="semesterId">
              <el-select v-model="form.semesterId" placeholder="请选择学期" style="width: 100%">
                <el-option
                  v-for="semester in semesterList"
                  :key="semester.semesterId"
                  :label="semester.name"
                  :value="semester.semesterId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程" prop="courseId">
              <el-select v-model="form.courseId" placeholder="请选择课程" filterable style="width: 100%">
                <el-option v-for="course in courseList" :key="course.courseId" :label="course.name" :value="course.courseId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教师" prop="teacherId">
              <el-select v-model="form.teacherId" placeholder="请选择教师" filterable style="width: 100%">
                <el-option v-for="teacher in teacherList" :key="teacher.teacherId" :label="teacher.name" :value="teacher.teacherId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item v-if="form.mode === 'class_based'" label="班级" prop="classId">
              <el-select v-model="form.classId" placeholder="请选择班级" filterable style="width: 100%">
                <el-option v-for="item in classList" :key="item.classId" :label="item.name" :value="item.classId" />
              </el-select>
            </el-form-item>
            <el-form-item v-else label="容量" prop="maxStudents">
              <el-input-number v-model="form.maxStudents" :min="1" :max="500" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教室" prop="classroomId">
              <el-select v-model="form.classroomId" placeholder="请选择教室" filterable style="width: 100%">
                <el-option
                  v-for="item in classroomList"
                  :key="item.classroomId"
                  :label="formatClassroomLabel(item)"
                  :value="item.classroomId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="星期" prop="dayOfWeek">
              <el-select v-model="form.dayOfWeek" style="width: 100%">
                <el-option v-for="day in weekdayOptions" :key="day.value" :label="day.label" :value="day.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开始节次" prop="startPeriod">
              <el-select v-model="form.startPeriod" placeholder="请选择开始节次" style="width: 100%">
                <el-option
                  v-for="period in periodOptions"
                  :key="period.value"
                  :label="period.label"
                  :value="period.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束节次" prop="endPeriod">
              <el-select v-model="form.endPeriod" placeholder="请选择结束节次" style="width: 100%">
                <el-option
                  v-for="period in endPeriodOptions"
                  :key="period.value"
                  :label="period.label"
                  :value="period.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <ConflictTip v-if="conflictList.length > 0" :conflicts="conflictList" />

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="warning" @click="doCheckConflict">检测冲突</el-button>
        <el-button type="primary" :disabled="hasP0Conflict" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="autoArrangeDialogVisible" title="自动排课" width="min(1040px, 94vw)" :close-on-click-modal="false">
      <el-alert title="可一次提交多个教学任务和多个候选时间段；部分成功后仅重试未完成任务。" type="info" :closable="false" show-icon />
      <el-form :model="autoArrangeForm" label-width="90px" class="auto-arrange-form">
        <el-form-item label="学期" required>
          <el-select v-model="autoArrangeForm.semesterId" style="width: 100%">
            <el-option v-for="semester in semesterList" :key="semester.semesterId" :label="semester.name" :value="semester.semesterId" />
          </el-select>
        </el-form-item>
        <div class="auto-arrange-section-header">
          <strong>排课任务</strong>
          <el-button type="primary" plain size="small" @click="addAutoArrangeTask">新增任务</el-button>
        </div>
        <div v-for="(task, index) in autoArrangeForm.tasks" :key="task.key" class="auto-arrange-row" :class="{ 'is-completed': task.completed }">
          <div class="auto-arrange-row-title">
            <span>任务 {{ index + 1 }}<el-tag v-if="task.completed" type="success" size="small">已排课</el-tag></span>
            <el-button type="danger" link :disabled="autoArrangeForm.tasks.length === 1" @click="removeAutoArrangeTask(index)">删除任务</el-button>
          </div>
          <el-row :gutter="12">
            <el-col :xs="24" :md="8"><el-form-item label="课程" required>
              <el-select v-model="task.courseId" filterable :disabled="task.completed" style="width: 100%">
                <el-option v-for="course in courseList" :key="course.courseId" :label="course.name" :value="course.courseId" />
              </el-select>
            </el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="教师" required>
              <el-select v-model="task.teacherId" filterable :disabled="task.completed" style="width: 100%">
                <el-option v-for="teacher in teacherList" :key="teacher.teacherId" :label="teacher.name" :value="teacher.teacherId" />
              </el-select>
            </el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="模式" required>
              <el-select v-model="task.mode" :disabled="task.completed" style="width: 100%">
                <el-option label="按班级排课" value="class_based" /><el-option label="开放选课" value="open_selection" />
              </el-select>
            </el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="班级" :required="task.mode === 'class_based'">
              <el-select v-model="task.classId" filterable clearable :disabled="task.completed || task.mode !== 'class_based'" style="width: 100%">
                <el-option v-for="item in classList" :key="item.classId" :label="item.name" :value="item.classId" />
              </el-select>
            </el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="容量" :required="task.mode === 'open_selection'">
              <el-input-number v-model="task.maxStudents" :min="1" :max="10000" :disabled="task.completed || task.mode !== 'open_selection'" style="width: 100%" />
            </el-form-item></el-col>
          </el-row>
        </div>
        <el-form-item label="候选教室" required>
          <el-select v-model="autoArrangeForm.classroomIds" multiple filterable style="width: 100%">
            <el-option v-for="item in classroomList" :key="item.classroomId" :label="formatClassroomLabel(item)" :value="item.classroomId" />
          </el-select>
        </el-form-item>
        <div class="auto-arrange-section-header">
          <strong>候选时间段</strong>
          <el-button type="primary" plain size="small" @click="addAutoArrangeTimeSlot">新增时间段</el-button>
        </div>
        <div v-for="(slot, index) in autoArrangeForm.timeSlots" :key="slot.key" class="auto-arrange-time-row">
          <span class="time-row-index">{{ index + 1 }}</span>
          <el-select v-model="slot.dayOfWeek" aria-label="星期"><el-option v-for="day in weekdayOptions" :key="day.value" :label="day.label" :value="day.value" /></el-select>
          <el-input-number v-model="slot.startPeriod" :min="1" :max="12" aria-label="开始节次" />
          <span>至</span>
          <el-input-number v-model="slot.endPeriod" :min="1" :max="12" aria-label="结束节次" />
          <el-button type="danger" link :disabled="autoArrangeForm.timeSlots.length === 1" @click="removeAutoArrangeTimeSlot(index)">删除</el-button>
        </div>
      </el-form>
      <div v-if="autoArrangeFailures.length" class="auto-arrange-result">
        <div class="auto-arrange-result-title">排课未完成：请根据以下冲突调整后直接重试</div>
        <ConflictTip :conflicts="autoArrangeFailureConflicts" />
      </div>
      <template #footer>
        <el-button @click="autoArrangeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="autoArranging" @click="submitAutoArrange">开始排课</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="conflictDialogVisible" title="冲突检测结果" width="600px">
      <ConflictTip v-if="conflictResult.length > 0" :conflicts="conflictResult" />
      <el-empty v-else description="未检测到冲突" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Plus } from '@element-plus/icons-vue'
import request from '../../utils/request'
import ConflictTip from '../../components/ConflictTip.vue'

const loading = ref(false)
const listError = ref(false)
const dialogVisible = ref(false)
const autoArrangeDialogVisible = ref(false)
const autoArranging = ref(false)
const autoArrangeFailures = ref([])
const conflictDialogVisible = ref(false)
const formRef = ref(null)
const tableData = ref([])
const semesterList = ref([])
const courseList = ref([])
const teacherList = ref([])
const classroomList = ref([])
const classList = ref([])
const conflictList = ref([])
const conflictResult = ref([])
const dialogTitle = ref('新增排课')

const searchForm = reactive({
  semesterId: ''
})

const page = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 创建空表单
const createEmptyForm = () => ({
  scheduleId: '',
  mode: 'class_based',
  maxStudents: 100,
  currentStudents: 0,
  courseId: '',
  teacherId: '',
  classId: '',
  semesterId: '',
  classroomId: '',
  dayOfWeek: 1,
  startPeriod: 1,
  endPeriod: 2
})

const form = reactive(createEmptyForm())
const autoArrangeForm = reactive({
  semesterId: '',
  classroomIds: [],
  tasks: [],
  timeSlots: []
})

let autoArrangeRowKey = 0
const createAutoArrangeTask = () => ({ key: ++autoArrangeRowKey, courseId: '', teacherId: '', classId: '', mode: 'class_based', maxStudents: 50, completed: false })
const createAutoArrangeTimeSlot = () => ({ key: ++autoArrangeRowKey, dayOfWeek: 1, startPeriod: 1, endPeriod: 2 })
const addAutoArrangeTask = () => autoArrangeForm.tasks.push(createAutoArrangeTask())
const removeAutoArrangeTask = index => autoArrangeForm.tasks.splice(index, 1)
const addAutoArrangeTimeSlot = () => autoArrangeForm.timeSlots.push(createAutoArrangeTimeSlot())
const removeAutoArrangeTimeSlot = index => autoArrangeForm.timeSlots.splice(index, 1)

const weekdayOptions = [
  { label: '周1', value: 1 },
  { label: '周2', value: 2 },
  { label: '周3', value: 3 },
  { label: '周4', value: 4 },
  { label: '周5', value: 5 },
  { label: '周6', value: 6 },
  { label: '周7', value: 7 }
]

const periodOptions = Array.from({ length: 12 }, (_, index) => ({
  value: index + 1,
  label: `第${index + 1}节`
}))

const endPeriodOptions = computed(() => periodOptions.filter(item => item.value >= form.startPeriod))
const hasP0Conflict = computed(() => conflictList.value.some(item => item.priority === 'P0'))

// 校验班级编号
const validateClassId = (_, value, callback) => {
  if (form.mode === 'class_based' && !value) {
    callback(new Error('请选择班级'))
    return
  }
  callback()
}

// 校验结束节次
const validateEndPeriod = (_, value, callback) => {
  if (!value) {
    callback(new Error('请选择结束节次'))
    return
  }
  if (value < form.startPeriod) {
    callback(new Error('结束节次不能早于开始节次'))
    return
  }
  callback()
}

const rules = {
  mode: [{ required: true, message: '请选择排课模式', trigger: 'change' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  teacherId: [{ required: true, message: '请选择教师', trigger: 'change' }],
  classId: [{ validator: validateClassId, trigger: 'change' }],
  classroomId: [{ required: true, message: '请选择教室', trigger: 'change' }],
  dayOfWeek: [{ required: true, message: '请选择星期', trigger: 'change' }],
  startPeriod: [{ required: true, message: '请选择开始节次', trigger: 'change' }],
  endPeriod: [{ validator: validateEndPeriod, trigger: 'change' }]
}

const semesterMap = computed(() => Object.fromEntries(semesterList.value.map(item => [item.semesterId, item.name])))
const courseMap = computed(() => Object.fromEntries(courseList.value.map(item => [item.courseId, item.name])))
const teacherMap = computed(() => Object.fromEntries(teacherList.value.map(item => [item.teacherId, item.name])))
const classMap = computed(() => Object.fromEntries(classList.value.map(item => [item.classId, item.name])))
const classroomMap = computed(() => Object.fromEntries(classroomList.value.map(item => [item.classroomId, formatClassroomLabel(item)])))

// 监听关键数据变化
watch(
  () => form.startPeriod,
  value => {
    if (form.endPeriod < value) {
      form.endPeriod = value
    }
  }
)

// 监听关键数据变化
watch(
  () => form.mode,
  mode => {
    if (mode !== 'class_based') {
      form.classId = ''
    }
  }
)

// 格式化星期显示
const formatWeekday = value => `周${value || ''}`

// 格式化教室显示名称
const formatClassroomLabel = classroom => {
  if (!classroom) {
    return ''
  }
  const extra = [classroom.building, classroom.type].filter(Boolean).join(' / ')
  return extra ? `${classroom.name} (${extra})` : classroom.name
}

// 构建排课请求数据
const buildPayload = source => ({
  scheduleId: source.scheduleId || '',
  mode: source.mode || 'class_based',
  maxStudents: source.mode === 'open_selection' ? Number(source.maxStudents || 0) : Number(source.maxStudents || 100),
  currentStudents: Number(source.currentStudents || 0),
  courseId: source.courseId || '',
  teacherId: source.teacherId || '',
  classId: source.mode === 'class_based' ? (source.classId || '') : '',
  semesterId: source.semesterId || '',
  classroomId: source.classroomId || '',
  dayOfWeek: Number(source.dayOfWeek || 1),
  startPeriod: Number(source.startPeriod || 1),
  endPeriod: Number(source.endPeriod || source.startPeriod || 1)
})

// 补全排课列表展示字段
const enrichScheduleRows = rows =>
  (rows || []).map(item => ({
    ...item,
    semesterName: semesterMap.value[item.semesterId] || item.semesterId,
    courseName: courseMap.value[item.courseId] || item.courseId,
    teacherName: teacherMap.value[item.teacherId] || item.teacherId,
    className: classMap.value[item.classId] || (item.mode === 'class_based' ? item.classId : '-'),
    classroomName: classroomMap.value[item.classroomId] || item.classroomId
  }))

// 重置查询条件
const resetSearch = () => {
  searchForm.semesterId = ''
  page.current = 1
  loadData()
}

// 加载学期列表
const loadSemesters = async () => {
  const res = await request.get('/semester/list')
  semesterList.value = res.data || []
}

// 加载选择数据
const loadSelectData = async () => {
  const [courseRes, teacherRes, classroomRes, classRes] = await Promise.all([
    request.get('/course/list').catch(() => ({ data: [] })),
    request.get('/teacher/list').catch(() => ({ data: [] })),
    request.get('/classroom/list').catch(() => ({ data: [] })),
    request.get('/class/list').catch(() => ({ data: [] }))
  ])

  courseList.value = courseRes.data || []
  teacherList.value = teacherRes.data || []
  classroomList.value = classroomRes.data || []
  classList.value = classRes.data || []
}

// 加载数据
const loadData = async () => {
  loading.value = true
  listError.value = false
  try {
    const res = await request.get('/schedule/page', {
      params: {
        current: page.current,
        size: page.size,
        semesterId: searchForm.semesterId || undefined
      },
      skipErrorMessage: true
    })
    tableData.value = enrichScheduleRows(res.data?.records || [])
    page.total = res.data?.total || 0
  } catch {
    tableData.value = []
    page.total = 0
    listError.value = true
  } finally {
    loading.value = false
  }
}

// 重置表单数据
const resetForm = row => {
  const payload = buildPayload(row || createEmptyForm())
  Object.assign(form, payload)
  if (!row && !form.semesterId) {
    form.semesterId = semesterList.value[0]?.semesterId || ''
  }
}

// 打开编辑对话框
const openDialog = row => {
  dialogTitle.value = row ? '编辑排课' : '新增排课'
  conflictList.value = []
  resetForm(row)
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openAutoArrangeDialog = () => {
  Object.assign(autoArrangeForm, {
    semesterId: searchForm.semesterId || semesterList.value[0]?.semesterId || '',
    classroomIds: [],
    tasks: [createAutoArrangeTask()],
    timeSlots: [createAutoArrangeTimeSlot()]
  })
  autoArrangeFailures.value = []
  autoArrangeDialogVisible.value = true
}

const autoArrangeFailureConflicts = computed(() => autoArrangeFailures.value.map(failure => ({
  ...failure,
  message: failure.reason || '未找到可用排课方案'
})))

const submitAutoArrange = async () => {
  if (!autoArrangeForm.semesterId) { ElMessage.warning('请选择学期'); return }
  if (autoArrangeForm.classroomIds.length === 0) { ElMessage.warning('请至少选择一个候选教室'); return }
  const pendingTasks = autoArrangeForm.tasks.map((task, index) => ({ task, index })).filter(item => !item.task.completed)
  if (pendingTasks.length === 0) { ElMessage.warning('当前任务均已排课，请新增任务后再提交'); return }
  const invalidTask = pendingTasks.find(({ task }) => !task.courseId || !task.teacherId
    || (task.mode === 'class_based' && !task.classId)
    || (task.mode === 'open_selection' && (!task.maxStudents || task.maxStudents < 1)))
  if (invalidTask) {
    ElMessage.warning(`请完整填写任务 ${invalidTask.index + 1} 的课程、教师、模式及班级或容量`)
    return
  }
  const invalidSlotIndex = autoArrangeForm.timeSlots.findIndex(slot => !slot.dayOfWeek || !slot.startPeriod || !slot.endPeriod || slot.startPeriod > slot.endPeriod)
  if (invalidSlotIndex >= 0) {
    ElMessage.warning(`候选时间段 ${invalidSlotIndex + 1} 的开始节次不能大于结束节次`)
    return
  }
  autoArranging.value = true
  try {
    const res = await request.post('/schedule-basic/auto-arrange', {
      semesterId: autoArrangeForm.semesterId,
      classroomIds: autoArrangeForm.classroomIds,
      timeSlots: autoArrangeForm.timeSlots.map(({ dayOfWeek, startPeriod, endPeriod }) => ({ dayOfWeek, startPeriod, endPeriod })),
      tasks: pendingTasks.map(({ task }) => ({
        courseId: task.courseId, teacherId: task.teacherId, classId: task.mode === 'class_based' ? task.classId : null,
        mode: task.mode, maxStudents: task.mode === 'open_selection' ? task.maxStudents : null
      }))
    })
    const data = res.data || {}
    const failed = Number(data.failedCount || data.failureCount || 0)
    const responseFailures = Array.isArray(data.failureDetails) ? data.failureDetails : []
    const failedIndexes = new Set(responseFailures.map(item => Number(item.index)))
    pendingTasks.forEach((item, responseIndex) => { if (!failedIndexes.has(responseIndex)) item.task.completed = true })
    autoArrangeFailures.value = responseFailures.map(item => ({ ...item, taskIndex: pendingTasks[Number(item.index)]?.index }))
    if (failed > 0) {
      ElMessage.warning(`自动排课部分完成：成功 ${data.arrangedCount || 0} 条，失败 ${failed} 条；请根据窗口中的建议调整后重试`)
      await loadData()
      return
    }
    ElMessage.success(`自动排课完成：成功 ${data.arrangedCount || 0} 条`)
    autoArrangeDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error?.message || '自动排课失败，已保留当前填写内容，请检查后重试')
  } finally {
    autoArranging.value = false
  }
}

// 执行检查冲突
// 执行冲突检测
const doCheckConflict = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  const res = await request.post('/schedule/check-conflict', buildPayload(form))
  conflictList.value = res.data || []
  if (conflictList.value.length === 0) {
    ElMessage.success('未检测到冲突')
    return
  }
  ElMessage.warning(`检测到 ${conflictList.value.length} 个冲突`)
}

// 处理提交
// 提交排课数据
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  const payload = buildPayload(form)
  if (payload.scheduleId) {
    await request.put('/schedule', payload)
  } else {
    await request.post('/schedule', payload)
  }

  ElMessage.success('保存成功')
  dialogVisible.value = false
  await loadData()
}

// 检查单条排课冲突
const checkConflict = async row => {
  const res = await request.post('/schedule/check-conflict', buildPayload(row))
  conflictResult.value = res.data || []
  conflictDialogVisible.value = true
}

// 删除排课记录
const handleDelete = async row => {
  await ElMessageBox.confirm('确认删除这条排课记录吗？', '提示', { type: 'warning' })
  await request.delete(`/schedule/${row.scheduleId}`)
  ElMessage.success('删除成功')
  await loadData()
}

// 页面挂载时初始化课表数据
onMounted(async () => {
  await Promise.all([loadSemesters(), loadSelectData()])
  await loadData()
})

</script>

<style scoped>
.page-container {
  width: 100%;
}

.auto-arrange-result {
  margin-top: 16px;
}

.auto-arrange-result-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-color-danger);
}

.auto-arrange-form { margin-top: 18px; }
.auto-arrange-section-header, .auto-arrange-row-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.auto-arrange-section-header { margin: 18px 0 10px; }
.auto-arrange-row {
  margin-bottom: 12px;
  padding: 12px 14px 0;
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
}
.auto-arrange-row.is-completed { background: var(--el-color-success-light-9); }
.auto-arrange-row-title { margin-bottom: 8px; font-weight: 600; }
.auto-arrange-row-title span { display: flex; align-items: center; gap: 8px; }
.auto-arrange-time-row {
  display: grid;
  grid-template-columns: 28px minmax(120px, 1fr) minmax(130px, 1fr) auto minmax(130px, 1fr) auto;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.time-row-index { text-align: center; color: var(--el-text-color-secondary); }
@media (max-width: 720px) {
  .auto-arrange-time-row { grid-template-columns: 24px 1fr 1fr; }
  .auto-arrange-time-row > span:nth-of-type(2) { display: none; }
}
</style>
