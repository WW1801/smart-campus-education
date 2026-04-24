<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #f59e0b, #fbbf24);">
          <el-icon :size="22"><Document /></el-icon>
        </div>
        <div>
          <div class="page-header-title">课程信息</div>
          <div class="page-header-desc">管理课程基本信息与学分</div>
        </div>
      </div>
      <el-button type="primary" @click="addCourse">
        <el-icon><Plus /></el-icon>
        添加课程
      </el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchForm.code" placeholder="搜索课程代码" style="width: 150px" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-input v-model="searchForm.name" placeholder="搜索课程名称" style="width: 150px" clearable />
        <el-select v-model="searchForm.departmentId" placeholder="筛选院系" clearable style="width: 160px">
          <el-option v-for="d in deptList" :key="d.departmentId" :label="d.name" :value="d.departmentId" />
        </el-select>
        <el-select v-model="searchForm.type" placeholder="课程类型" clearable style="width: 130px">
          <el-option label="必修课" value="compulsory" />
          <el-option label="选修课" value="elective" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="courseList" stripe v-loading="loading">
        <el-table-column prop="courseId" label="课程ID" width="100" />
        <el-table-column prop="code" label="课程代码" width="120" />
        <el-table-column prop="name" label="课程名称" width="160" />
        <el-table-column prop="credits" label="学分" width="70" />
        <el-table-column prop="hours" label="课时" width="70" />
        <el-table-column prop="type" label="课程类型" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.type === 'compulsory' ? 'success' : 'info'">
              {{ scope.row.type === 'compulsory' ? '必修课' : '选修课' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="departmentName" label="院系" width="130" />
        <el-table-column prop="description" label="课程描述" show-overflow-tooltip />
        <el-table-column label="操作" width="140">
          <template #default="scope">
            <el-button size="small" type="primary" link @click="editCourse(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="deleteCourse(scope.row.courseId)">删除</el-button>
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

    <el-dialog :title="editMode ? '编辑课程' : '添加课程'" v-model="dialogVisible" width="560px">
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="100px">
        <el-form-item label="课程代码" prop="code">
          <el-input v-model="courseForm.code" placeholder="请输入课程代码" />
        </el-form-item>
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="courseForm.name" placeholder="请输入课程名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学分" prop="credits">
              <el-input-number v-model.number="courseForm.credits" :min="1" :max="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课时" prop="hours">
              <el-input-number v-model.number="courseForm.hours" :min="1" :max="200" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程类型" prop="type">
              <el-select v-model="courseForm.type" style="width: 100%">
                <el-option label="必修课" value="compulsory" />
                <el-option label="选修课" value="elective" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="院系" prop="departmentId">
              <el-select v-model="courseForm.departmentId" style="width: 100%">
                <el-option v-for="d in deptList" :key="d.departmentId" :label="d.name" :value="d.departmentId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="课程描述" prop="description">
          <el-input v-model="courseForm.description" type="textarea" placeholder="请输入课程描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCourse">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Plus, Search } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const courseList = ref([])
const deptList = ref([])
const searchForm = ref({ code: '', name: '', departmentId: '', type: '' })
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)
const dialogVisible = ref(false)
const editMode = ref(false)
const courseFormRef = ref(null)
const courseForm = ref({
  courseId: '', code: '', name: '', credits: null, hours: null, type: '', departmentId: '', description: ''
})

const courseRules = {
  code: [{ required: true, message: '请输入课程代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  credits: [{ required: true, message: '请输入学分', trigger: 'blur' }],
  hours: [{ required: true, message: '请输入课时', trigger: 'blur' }],
  type: [{ required: true, message: '请选择课程类型', trigger: 'change' }],
  departmentId: [{ required: true, message: '请选择院系', trigger: 'change' }]
}

onMounted(() => { getCourseList(); loadDepts() })

const loadDepts = async () => {
  try { const res = await request.get('/department/list'); deptList.value = res.data || [] }
  catch {}
}

const getCourseList = async () => {
  loading.value = true
  try {
    const res = await request.get('/course/page', {
      params: { current: currentPage.value, size: pageSize.value, ...searchForm.value }
    })
    courseList.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const search = () => { currentPage.value = 1; getCourseList() }
const resetSearch = () => { searchForm.value = { code: '', name: '', departmentId: '', type: '' }; currentPage.value = 1; getCourseList() }

const addCourse = () => { editMode.value = false; courseForm.value = { courseId: '', code: '', name: '', credits: null, hours: null, type: '', departmentId: '', description: '' }; dialogVisible.value = true }
const editCourse = (row) => { editMode.value = true; courseForm.value = { ...row }; dialogVisible.value = true }

const deleteCourse = async (courseId) => {
  try { await ElMessageBox.confirm('确认删除该课程？', '提示', { type: 'warning' }); await request.delete(`/course/${courseId}`); ElMessage.success('删除成功'); getCourseList() }
  catch (error) { if (error !== 'cancel') ElMessage.error('删除课程失败') }
}

const saveCourse = async () => {
  if (!courseFormRef.value) return
  try { await courseFormRef.value.validate() } catch { return }
  try {
    if (editMode.value) { await request.put('/course', courseForm.value) }
    else { await request.post('/course', courseForm.value) }
    dialogVisible.value = false
    ElMessage.success(editMode.value ? '更新成功' : '添加成功')
    getCourseList()
  } catch (error) { ElMessage.error('保存课程失败') }
}

const handleSizeChange = (size) => { pageSize.value = size; getCourseList() }
const handleCurrentChange = (current) => { currentPage.value = current; getCourseList() }
</script>

<style scoped>
.page-container { width: 100%; }
</style>
