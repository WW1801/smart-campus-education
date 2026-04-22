<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #8b5cf6, #a78bfa);">
          <el-icon :size="22"><UserFilled /></el-icon>
        </div>
        <div>
          <div class="page-header-title">教师信息</div>
          <div class="page-header-desc">管理教师基本信息与职称</div>
        </div>
      </div>
      <el-button type="primary" @click="addTeacher">
        <el-icon><Plus /></el-icon>
        添加教师
      </el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchForm.teacherId" placeholder="搜索工号" style="width: 150px" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-input v-model="searchForm.name" placeholder="搜索姓名" style="width: 130px" clearable />
        <el-select v-model="searchForm.departmentId" placeholder="筛选院系" clearable style="width: 160px">
          <el-option v-for="d in deptList" :key="d.departmentId" :label="d.name" :value="d.departmentId" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="teacherList" stripe v-loading="loading">
        <el-table-column prop="teacherId" label="工号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="scope">{{ scope.row.gender === 'male' ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="departmentId" label="院系" width="130" />
        <el-table-column prop="title" label="职称" width="100" />
        <el-table-column prop="specialty" label="专业方向" width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'active' ? 'success' : scope.row.status === 'leave' ? 'warning' : 'danger'">
              {{ scope.row.status === 'active' ? '在职' : scope.row.status === 'leave' ? '休假' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button type="primary" size="small" link @click="editTeacher(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="deleteTeacher(scope.row.teacherId)">删除</el-button>
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

    <el-dialog :title="editMode ? '编辑教师' : '添加教师'" v-model="dialogVisible" width="600px">
      <el-form ref="teacherForm" :model="teacherForm" :rules="teacherRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工号" prop="teacherId">
              <el-input v-model="teacherForm.teacherId" placeholder="请输入工号" :disabled="editMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="teacherForm.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="teacherForm.gender" placeholder="请选择性别" style="width: 100%">
                <el-option label="男" value="male" />
                <el-option label="女" value="female" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthdate">
              <el-date-picker v-model="teacherForm.birthdate" type="date" placeholder="请选择出生日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="电话" prop="phone">
              <el-input v-model="teacherForm.phone" placeholder="请输入电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="teacherForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="院系" prop="departmentId">
              <el-select v-model="teacherForm.departmentId" placeholder="请选择院系" style="width: 100%">
                <el-option v-for="d in deptList" :key="d.departmentId" :label="d.name" :value="d.departmentId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称" prop="title">
              <el-input v-model="teacherForm.title" placeholder="请输入职称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="专业方向" prop="specialty">
              <el-input v-model="teacherForm.specialty" placeholder="请输入专业方向" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="teacherForm.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="在职" value="active" />
                <el-option label="休假" value="leave" />
                <el-option label="离职" value="resigned" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTeacher">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, Plus, Search } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const teacherList = ref([])
const deptList = ref([])
const searchForm = ref({ teacherId: '', name: '', departmentId: '' })
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)
const dialogVisible = ref(false)
const editMode = ref(false)
const teacherForm = ref({
  teacherId: '', name: '', gender: '', birthdate: '', phone: '', email: '', departmentId: '', title: '', specialty: '', status: ''
})

const teacherRules = {
  teacherId: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  birthdate: [{ required: true, message: '请选择出生日期', trigger: 'change' }],
  departmentId: [{ required: true, message: '请选择院系', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const loadDepts = async () => {
  try {
    const res = await request.get('/department/list')
    deptList.value = res.data || []
  } catch {}
}

onMounted(() => { getTeacherList(); loadDepts() })

const getTeacherList = async () => {
  loading.value = true
  try {
    const res = await request.get('/teacher/page', {
      params: { current: currentPage.value, size: pageSize.value, ...searchForm.value }
    })
    teacherList.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    ElMessage.error('获取教师列表失败')
  } finally {
    loading.value = false
  }
}

const search = () => { currentPage.value = 1; getTeacherList() }
const resetSearch = () => {
  searchForm.value = { teacherId: '', name: '', departmentId: '' }
  currentPage.value = 1
  getTeacherList()
}

const addTeacher = () => {
  editMode.value = false
  teacherForm.value = { teacherId: '', name: '', gender: '', birthdate: '', phone: '', email: '', departmentId: '', title: '', specialty: '', status: '' }
  dialogVisible.value = true
}

const editTeacher = (row) => { editMode.value = true; teacherForm.value = { ...row }; dialogVisible.value = true }

const deleteTeacher = async (teacherId) => {
  try { await ElMessageBox.confirm('确认删除该教师？', '提示', { type: 'warning' }); await request.delete(`/teacher/${teacherId}`); ElMessage.success('删除成功'); getTeacherList() }
  catch (e) { if (e !== 'cancel') ElMessage.error('删除教师失败') }
}

const saveTeacher = async () => {
  if (!teacherForm.value) return
  try { await teacherForm.value.validate() } catch { return }
  try {
    if (editMode.value) { await request.put('/teacher', teacherForm.value) }
    else { await request.post('/teacher', teacherForm.value) }
    dialogVisible.value = false
    ElMessage.success(editMode.value ? '更新成功' : '添加成功')
    getTeacherList()
  } catch (error) { ElMessage.error('保存教师失败') }
}

const handleSizeChange = (size) => { pageSize.value = size; getTeacherList() }
const handleCurrentChange = (current) => { currentPage.value = current; getTeacherList() }
</script>

<style scoped>
.page-container { width: 100%; }
</style>
