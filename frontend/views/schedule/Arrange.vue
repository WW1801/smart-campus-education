<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #10b981, #34d399);">
          <el-icon :size="22"><Calendar /></el-icon>
        </div>
        <div>
          <div class="page-header-title">课程安排</div>
          <div class="page-header-desc">管理排课信息与冲突检测</div>
        </div>
      </div>
      <el-button type="primary" @click="openDialog(null)">
        <el-icon><Plus /></el-icon>
        新增排课
      </el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="searchForm.semesterId" placeholder="选择学期" clearable style="width: 200px">
          <el-option v-for="s in semesterList" :key="s.semesterId" :label="s.name" :value="s.semesterId" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="courseId" label="课程ID" width="100" />
        <el-table-column prop="teacherId" label="教师ID" width="100" />
        <el-table-column prop="classId" label="班级ID" width="100" />
        <el-table-column prop="mode" label="模式" width="110">
          <template #default="{ row }">
            <el-tag :type="row.mode === 'open_selection' ? 'success' : ''">
              {{ row.mode === 'open_selection' ? '开放选课' : '按班级排课' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="130">
          <template #default="{ row }">周{{ row.dayOfWeek }} 第{{ row.startPeriod }}-{{ row.endPeriod }}节</template>
        </el-table-column>
        <el-table-column prop="classroomId" label="教室" width="80" />
        <el-table-column label="选课容量" width="120">
          <template #default="{ row }">
            <span v-if="row.mode === 'open_selection'">{{ row.currentStudents }}/{{ row.maxStudents }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" link @click="checkConflict(row)">冲突检测</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
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
                <el-option v-for="s in semesterList" :key="s.semesterId" :label="s.name" :value="s.semesterId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程" prop="courseId">
              <el-select v-model="form.courseId" placeholder="请选择课程" filterable style="width: 100%">
                <el-option v-for="c in courseList" :key="c.courseId" :label="c.name" :value="c.courseId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教师" prop="teacherId">
              <el-select v-model="form.teacherId" placeholder="请选择教师" filterable style="width: 100%">
                <el-option v-for="t in teacherList" :key="t.teacherId" :label="t.name" :value="t.teacherId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="班级" prop="classId" v-if="form.mode === 'class_based'">
              <el-select v-model="form.classId" placeholder="请选择班级" style="width: 100%">
                <el-option v-for="c in classList" :key="c.classId" :label="c.name" :value="c.classId" />
              </el-select>
            </el-form-item>
            <el-form-item label="选课容量" v-if="form.mode === 'open_selection'">
              <el-input-number v-model="form.maxStudents" :min="1" :max="500" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教室" prop="classroomId">
              <el-select v-model="form.classroomId" placeholder="请选择教室" style="width: 100%">
                <el-option v-for="c in classroomList" :key="c.classroomId" :label="c.name" :value="c.classroomId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="星期" prop="dayOfWeek">
              <el-select v-model="form.dayOfWeek" style="width: 100%">
                <el-option v-for="d in 7" :key="d" :label="'周'+d" :value="d" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开始节次" prop="startPeriod">
              <el-input-number v-model="form.startPeriod" :min="1" :max="12" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束节次" prop="endPeriod">
              <el-input-number v-model="form.endPeriod" :min="1" :max="12" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <ConflictTip :conflicts="conflictList" v-if="conflictList.length > 0" />

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="warning" @click="doCheckConflict">检测冲突</el-button>
        <el-button type="primary" @click="handleSubmit" :disabled="hasP0Conflict">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="conflictDialogVisible" title="冲突检测结果" width="600px">
      <ConflictTip :conflicts="conflictResult" />
      <el-empty v-if="conflictResult.length === 0" description="未检测到冲突" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Plus } from '@element-plus/icons-vue'
import ConflictTip from '../../components/ConflictTip.vue'

const loading = ref(false)
const tableData = ref([])
const semesterList = ref([])
const courseList = ref([])
const teacherList = ref([])
const classroomList = ref([])
const classList = ref([])
const dialogVisible = ref(false)
const conflictDialogVisible = ref(false)
const conflictList = ref([])
const conflictResult = ref([])
const formRef = ref(null)

const searchForm = reactive({ semesterId: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const form = reactive({
  scheduleId: '', mode: 'class_based', maxStudents: 100, currentStudents: 0,
  courseId: '', teacherId: '', classId: '', semesterId: '',
  classroomId: '', dayOfWeek: 1, startPeriod: 1, endPeriod: 2
})

const rules = {
  mode: [{ required: true, message: '请选择排课模式', trigger: 'change' }],
  courseId: [{ required: true, message: '请输入课程ID', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请输入教师ID', trigger: 'blur' }],
  semesterId: [{ required: true, message: '请输入学期ID', trigger: 'blur' }],
  classroomId: [{ required: true, message: '请输入教室ID', trigger: 'blur' }],
  dayOfWeek: [{ required: true, message: '请选择星期', trigger: 'change' }],
  startPeriod: [{ required: true, message: '请输入开始节次', trigger: 'blur' }],
  endPeriod: [{ required: true, message: '请输入结束节次', trigger: 'blur' }]
}

const dialogTitle = ref('新增排课')
const hasP0Conflict = computed(() => conflictList.value.some(c => c.priority === 'P0'))

const resetSearch = () => { searchForm.semesterId = ''; page.current = 1; loadData() }

const loadSemesters = async () => {
  try { const res = await request.get('/semester/list'); semesterList.value = res.data || [] } catch {}
}

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

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/schedule/page', { params: { current: page.current, size: page.size, ...searchForm } })
    tableData.value = res.data.records
    page.total = res.data.total
  } finally { loading.value = false }
}

const openDialog = (row) => {
  dialogTitle.value = row ? '编辑排课' : '新增排课'
  conflictList.value = []
  Object.assign(form, row || { scheduleId: '', mode: 'class_based', maxStudents: 100, currentStudents: 0, courseId: '', teacherId: '', classId: '', semesterId: '', classroomId: '', dayOfWeek: 1, startPeriod: 1, endPeriod: 2 })
  dialogVisible.value = true
}

const doCheckConflict = async () => {
  try {
    const res = await request.post('/schedule/check-conflict', form)
    conflictList.value = res.data || []
    if (conflictList.value.length === 0) ElMessage.success('未检测到冲突')
    else ElMessage.warning(`检测到${conflictList.value.length}个冲突`)
  } catch {}
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch { return }
  try {
    if (form.scheduleId) await request.put('/schedule', form)
    else await request.post('/schedule', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch {}
}

const checkConflict = async (row) => {
  try { const res = await request.post('/schedule/check-conflict', row); conflictResult.value = res.data || []; conflictDialogVisible.value = true } catch {}
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该排课记录？', '提示', { type: 'warning' })
  await request.delete(`/schedule/${row.scheduleId}`)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => { loadSemesters(); loadSelectData(); loadData() })
</script>

<style scoped>
.page-container { width: 100%; }
</style>
