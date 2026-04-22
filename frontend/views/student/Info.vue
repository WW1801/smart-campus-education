<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #6366f1, #818cf8);">
          <el-icon :size="22"><User /></el-icon>
        </div>
        <div>
          <div class="page-header-title">学生信息</div>
          <div class="page-header-desc">管理学生基本信息与学籍状态</div>
        </div>
      </div>
      <el-button type="primary" @click="openDialog(null)">
        <el-icon><Plus /></el-icon>
        新增学生
      </el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchForm.studentId" placeholder="搜索学号" style="width: 150px" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-input v-model="searchForm.name" placeholder="搜索姓名" style="width: 130px" clearable />
        <el-select v-model="searchForm.departmentId" placeholder="筛选院系" clearable style="width: 160px">
          <el-option v-for="d in deptList" :key="d.departmentId" :label="d.name" :value="d.departmentId" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="学籍状态" clearable style="width: 130px">
          <el-option label="在读" value="active" />
          <el-option label="休学" value="suspended" />
          <el-option label="毕业" value="graduated" />
          <el-option label="退学" value="dropped" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="80" />
        <el-table-column prop="gender" label="性别" width="60">
          <template #default="{ row }">{{ row.gender === 'male' ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column prop="departmentId" label="院系" width="120" />
        <el-table-column prop="majorId" label="专业" width="140" />
        <el-table-column prop="classId" label="班级" width="100" />
        <el-table-column prop="enrollmentDate" label="入学日期" width="110" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" link @click="changeStatus(row)" :disabled="row.status === 'graduated' || row.status === 'dropped'">学籍变更</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentId">
              <el-input v-model="form.studentId" :disabled="!!form._existing" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio label="male">男</el-radio>
                <el-radio label="female">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthdate">
              <el-date-picker v-model="form.birthdate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="院系" prop="departmentId">
              <el-select v-model="form.departmentId" style="width: 100%" @change="onDeptChange">
                <el-option v-for="d in deptList" :key="d.departmentId" :label="d.name" :value="d.departmentId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业" prop="majorId">
              <el-select v-model="form.majorId" style="width: 100%">
                <el-option v-for="m in filteredMajors" :key="m.majorId" :label="m.name" :value="m.majorId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="班级" prop="classId">
              <el-select v-model="form.classId" style="width: 100%">
                <el-option v-for="c in filteredClasses" :key="c.classId" :label="c.name" :value="c.classId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入学日期" prop="enrollmentDate">
              <el-date-picker v-model="form.enrollmentDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusDialogVisible" title="学籍状态变更" width="420px">
      <el-form label-width="80px">
        <el-form-item label="当前状态">
          <el-tag :type="statusTagType(currentStudent?.status)">{{ statusLabel(currentStudent?.status) }}</el-tag>
        </el-form-item>
        <el-form-item label="变更至">
          <el-select v-model="targetStatus" style="width: 100%">
            <el-option v-for="s in allowedTargets" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因">
          <el-input v-model="statusReason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStatusChange">确认变更</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Plus, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const deptList = ref([])
const majorList = ref([])
const classList = ref([])
const dialogVisible = ref(false)
const statusDialogVisible = ref(false)
const formRef = ref(null)
const currentStudent = ref(null)
const targetStatus = ref('')
const statusReason = ref('')

const searchForm = reactive({ studentId: '', name: '', departmentId: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const form = reactive({
  studentId: '', name: '', gender: 'male', birthdate: '', phone: '', email: '',
  departmentId: '', majorId: '', classId: '', enrollmentDate: '', _existing: false
})

const rules = {
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  birthdate: [{ required: true, message: '请选择出生日期', trigger: 'change' }],
  departmentId: [{ required: true, message: '请选择院系', trigger: 'change' }],
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  enrollmentDate: [{ required: true, message: '请选择入学日期', trigger: 'change' }]
}

const dialogTitle = ref('新增学生')

const statusLabel = (s) => ({ active: '在读', suspended: '休学', graduated: '毕业', dropped: '退学' }[s] || s)
const statusTagType = (s) => ({ active: 'success', suspended: 'warning', graduated: '', dropped: 'danger' }[s] || 'info')

const allowedTargets = computed(() => {
  const s = currentStudent.value?.status
  const map = {
    active: [{ value: 'suspended', label: '休学' }, { value: 'graduated', label: '毕业' }, { value: 'dropped', label: '退学' }],
    suspended: [{ value: 'active', label: '复学' }, { value: 'dropped', label: '退学' }]
  }
  return map[s] || []
})

const filteredMajors = computed(() => {
  if (!form.departmentId) return majorList.value
  return majorList.value.filter(m => m.departmentId === form.departmentId)
})

const filteredClasses = computed(() => {
  if (!form.majorId) return classList.value
  return classList.value.filter(c => c.majorId === form.majorId)
})

const onDeptChange = () => {
  form.majorId = ''
  form.classId = ''
}

const resetSearch = () => {
  Object.assign(searchForm, { studentId: '', name: '', departmentId: '', status: '' })
  page.current = 1
  loadData()
}

const loadBasicData = async () => {
  const [deptRes, majorRes, classRes] = await Promise.all([
    request.get('/department/list').catch(() => ({ data: [] })),
    request.get('/major/list').catch(() => ({ data: [] })),
    request.get('/class/list').catch(() => ({ data: [] }))
  ])
  deptList.value = deptRes.data || []
  majorList.value = majorRes.data || []
  classList.value = classRes.data || []
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/student/page', {
      params: { current: page.current, size: page.size, ...searchForm }
    })
    tableData.value = res.data.records
    page.total = res.data.total
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  dialogTitle.value = row ? '编辑学生' : '新增学生'
  Object.assign(form, row ? { ...row, _existing: true } : {
    studentId: '', name: '', gender: 'male', birthdate: '', phone: '', email: '',
    departmentId: '', majorId: '', classId: '', enrollmentDate: '', _existing: false
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch { return }
  const payload = { ...form }
  delete payload._existing
  try {
    if (form._existing) {
      await request.put('/student', payload)
    } else {
      await request.post('/student', payload)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } catch { ElMessage.error('操作失败') }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该学生？', '提示', { type: 'warning' })
  await request.delete(`/student/${row.studentId}`)
  ElMessage.success('删除成功')
  loadData()
}

const changeStatus = (row) => {
  currentStudent.value = row
  targetStatus.value = ''
  statusReason.value = ''
  statusDialogVisible.value = true
}

const submitStatusChange = async () => {
  if (!targetStatus.value) {
    ElMessage.warning('请选择目标状态')
    return
  }
  await request.put(`/student/${currentStudent.value.studentId}/status`, {
    status: targetStatus.value,
    reason: statusReason.value
  })
  ElMessage.success('学籍状态变更成功')
  statusDialogVisible.value = false
  loadData()
}

onMounted(() => {
  loadBasicData()
  loadData()
})
</script>

<style scoped>
.page-container {
  width: 100%;
}
</style>
