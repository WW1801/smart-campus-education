<!-- 学生注册页面组件，负责处理学生模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><Stamp /></el-icon>
        </div>
        <div>
          <div class="page-header-title">学生注册</div>
          <div class="page-header-desc">管理学生注册审核流程</div>
        </div>
      </div>
      <el-button type="primary" @click="addRegister">
        <el-icon><Plus /></el-icon>
        添加注册
      </el-button>
    </div>

    <PageErrorState v-if="listError" :retrying="loading" title="注册列表加载失败" @retry="getRegisterList" />
    <el-card v-else>
      <div class="search-bar">
        <el-input v-model="searchForm.studentNo" placeholder="搜索学号" style="width: 150px" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-input v-model="searchForm.name" placeholder="搜索姓名" style="width: 130px" clearable />
        <el-select v-model="searchForm.status" placeholder="审核状态" clearable style="width: 130px">
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="registerList" stripe v-loading="loading">
        <el-table-column prop="registerId" label="注册ID" width="120" />
        <el-table-column prop="studentNo" label="学号" width="170" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="departmentName" label="院系" width="150" />
        <el-table-column prop="majorName" label="专业" width="150" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="registerDate" label="注册日期" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'pending' ? 'warning' : scope.row.status === 'approved' ? 'success' : 'danger'">
              {{ scope.row.status === 'pending' ? '待审核' : scope.row.status === 'approved' ? '已通过' : '已拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button type="primary" size="small" link @click="viewRegister(scope.row)">查看</el-button>
            <el-button v-if="scope.row.status === 'pending'" type="success" size="small" link @click="approveRegister(scope.row.registerId)">通过</el-button>
            <el-button v-if="scope.row.status === 'pending'" type="danger" size="small" link @click="rejectRegister(scope.row.registerId)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
      />
    </el-card>

    <el-dialog title="添加注册" v-model="dialogVisible" width="600px">
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="100px">
        <el-form-item label="学号">
          <el-input disabled placeholder="保存后自动生成" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="registerForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="院系" prop="departmentId">
          <el-select v-model="registerForm.departmentId" placeholder="请选择院系" style="width: 100%">
            <el-option v-for="d in deptList" :key="d.departmentId" :label="d.name" :value="d.departmentId" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业" prop="majorId">
          <el-select v-model="registerForm.majorId" placeholder="请选择专业" style="width: 100%">
            <el-option v-for="m in majorList" :key="m.majorId" :label="m.name" :value="m.majorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="classId">
          <el-select v-model="registerForm.classId" placeholder="请选择班级" style="width: 100%">
            <el-option v-for="c in classList" :key="c.classId" :label="c.name" :value="c.classId" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册日期" prop="registerDate">
          <el-date-picker v-model="registerForm.registerDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择注册日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="registerForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRegister">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="注册详情" v-model="viewDialogVisible" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="注册ID">{{ viewForm.registerId }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ viewForm.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ viewForm.name }}</el-descriptions-item>
        <el-descriptions-item label="院系">{{ viewForm.departmentName }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ viewForm.majorName }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ viewForm.className }}</el-descriptions-item>
        <el-descriptions-item label="注册日期">{{ viewForm.registerDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ viewForm.status === 'pending' ? '待审核' : viewForm.status === 'approved' ? '已通过' : '已拒绝' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ viewForm.remark }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Stamp, Plus, Search } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const listError = ref(false)
const registerList = ref([])
const deptList = ref([])
const majorList = ref([])
const classList = ref([])
const searchForm = ref({ studentNo: '', name: '', status: '' })
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const registerFormRef = ref(null)
const registerForm = ref({
  registerId: '', name: '', departmentId: '', majorId: '', classId: '', registerDate: '', remark: ''
})

const viewForm = ref({
  registerId: '', studentNo: '', name: '', departmentName: '', majorName: '', className: '', registerDate: '', status: '', remark: ''
})

const registerRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择院系', trigger: 'change' }],
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  registerDate: [{ required: true, message: '请选择注册日期', trigger: 'change' }]
}

// 加载basic数据
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

// 页面挂载时初始化学生数据
onMounted(() => {
  getRegisterList()
  loadBasicData()
})

// 获取注册列表
const getRegisterList = async () => {
  loading.value = true
  listError.value = false
  try {
    const res = await request.get('/student/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        studentNo: searchForm.value.studentNo || undefined,
        name: searchForm.value.name || undefined,
        status: searchForm.value.status || undefined
      },
      skipErrorMessage: true
    })
    registerList.value = (res.data.records || []).map(s => ({
      registerId: s.studentId,
      studentNo: s.studentNo,
      name: s.name,
      departmentName: s.departmentId,
      majorName: s.majorId,
      className: s.classId,
      registerDate: s.enrollmentDate,
      status: s.status === 'active' ? 'approved' : 'pending',
      remark: ''
    }))
    total.value = res.data.total || 0
  } catch {
    registerList.value = []
    total.value = 0
    listError.value = true
  } finally {
    loading.value = false
  }
}

// 按条件查询学生
const search = () => { currentPage.value = 1; getRegisterList() }
// 重置查询条件
const resetSearch = () => { searchForm.value = { studentNo: '', name: '', status: '' }; currentPage.value = 1; getRegisterList() }

// 添加注册
const addRegister = () => {
  registerForm.value = { registerId: '', name: '', departmentId: '', majorId: '', classId: '', registerDate: '', remark: '' }
  dialogVisible.value = true
}

// 处理视图注册
const viewRegister = (row) => { viewForm.value = { ...row }; viewDialogVisible.value = true }

// 处理通过注册
const approveRegister = async (registerId) => {
  await ElMessageBox.confirm('确认通过该注册申请？', '提示', { type: 'success' })
  try {
    await request.put(`/student/${registerId}/status`, { status: 'active', reason: '注册审核通过' })
    ElMessage.success('审核通过成功')
    getRegisterList()
  } catch { ElMessage.error('操作失败') }
}

// 处理驳回注册
const rejectRegister = async (registerId) => {
  await ElMessageBox.confirm('确认拒绝该注册申请？', '提示', { type: 'warning' })
  try {
    await request.put(`/student/${registerId}/status`, { status: 'dropped', reason: '注册审核拒绝' })
    ElMessage.success('已拒绝')
    getRegisterList()
  } catch { ElMessage.error('操作失败') }
}

// 保存注册
const saveRegister = async () => {
  if (!registerFormRef.value) return
  try {
    await registerFormRef.value.validate()
  } catch { return }
  try {
    const res = await request.post('/student', {
      name: registerForm.value.name,
      gender: 'male',
      birthdate: '2000-01-01',
      departmentId: registerForm.value.departmentId,
      majorId: registerForm.value.majorId,
      classId: registerForm.value.classId,
      enrollmentDate: registerForm.value.registerDate,
      status: 'active'
    })
    dialogVisible.value = false
    ElMessage.success(res.message || '添加成功')
    getRegisterList()
  } catch { ElMessage.error('保存失败') }
}

// 处理每页条数变化
const handleSizeChange = (size) => { pageSize.value = size; getRegisterList() }
// 处理页码变化
const handleCurrentChange = (current) => { currentPage.value = current; getRegisterList() }
</script>

<style scoped>
.page-container { width: 100%; }
</style>
