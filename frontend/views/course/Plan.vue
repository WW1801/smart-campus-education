<!-- 课程计划页面组件，负责处理课程模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #ec4899, #f472b6);">
          <el-icon :size="22"><Notebook /></el-icon>
        </div>
        <div>
          <div class="page-header-title">教学计划</div>
          <div class="page-header-desc">管理各专业教学计划与课程安排</div>
        </div>
      </div>
      <el-button type="primary" @click="addPlan">
        <el-icon><Plus /></el-icon>
        添加教学计划
      </el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-select v-model="searchForm.majorId" placeholder="筛选专业" clearable style="width: 180px">
          <el-option v-for="m in majorList" :key="m.majorId" :label="m.name" :value="m.majorId" />
        </el-select>
        <el-input v-model="searchForm.grade" placeholder="搜索年级" style="width: 130px" clearable />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="planList" stripe v-loading="loading">
        <el-table-column prop="planId" label="计划ID" width="110" />
        <el-table-column prop="majorId" label="专业" width="150" />
        <el-table-column prop="courseId" label="课程" width="150" />
        <el-table-column prop="semesterType" label="建议学期" width="100" />
        <el-table-column prop="courseNature" label="课程性质" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.courseNature === 'compulsory' ? 'success' : 'info'" size="small">
              {{ scope.row.courseNature === 'compulsory' ? '必修' : scope.row.courseNature === 'elective_major' ? '专业选修' : '公共选修' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isPrerequisite" label="先修课程" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isPrerequisite === 1 ? 'warning' : 'info'" size="small">
              {{ scope.row.isPrerequisite === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="prerequisiteIds" label="先修课程ID" show-overflow-tooltip />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" type="primary" link @click="editPlan(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="deletePlan(scope.row.planId)">删除</el-button>
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

    <el-dialog :title="editMode ? '编辑教学计划' : '添加教学计划'" v-model="dialogVisible" width="560px">
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="100px">
        <el-form-item label="专业" prop="majorId">
          <el-select v-model="planForm.majorId" style="width: 100%">
            <el-option v-for="m in majorList" :key="m.majorId" :label="m.name" :value="m.majorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="planForm.courseId" style="width: 100%" filterable>
            <el-option v-for="c in courseOptions" :key="c.courseId" :label="c.name" :value="c.courseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="建议学期" prop="semesterType">
          <el-input-number v-model="planForm.semesterType" :min="1" :max="8" style="width: 100%" />
        </el-form-item>
        <el-form-item label="课程性质" prop="courseNature">
          <el-select v-model="planForm.courseNature" style="width: 100%">
            <el-option label="必修" value="compulsory" />
            <el-option label="专业选修" value="elective_major" />
            <el-option label="公共选修" value="elective_public" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否先修" prop="isPrerequisite">
          <el-select v-model="planForm.isPrerequisite" style="width: 100%">
            <el-option label="否" :value="0" />
            <el-option label="是" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="先修课程ID" prop="prerequisiteIds">
          <el-input v-model="planForm.prerequisiteIds" placeholder="多个用逗号分隔，如：CO001,CO002" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePlan">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Notebook, Plus } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const planList = ref([])
const majorList = ref([])
const courseOptions = ref([])
const searchForm = ref({ majorId: '', grade: '' })
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const editMode = ref(false)
const planFormRef = ref(null)
const planForm = ref({ planId: '', majorId: '', courseId: '', semesterType: 1, courseNature: 'compulsory', isPrerequisite: 0, prerequisiteIds: '' })

const planRules = {
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  semesterType: [{ required: true, message: '请输入建议学期', trigger: 'blur' }],
  courseNature: [{ required: true, message: '请选择课程性质', trigger: 'change' }]
}

// 页面挂载时初始化课程数据
onMounted(() => {
  getPlanList()
  loadMajors()
  loadCourses()
})

// 加载专业列表
const loadMajors = async () => {
  try { const res = await request.get('/major/list'); majorList.value = res.data || [] }
  catch {}
}

// 加载课程
const loadCourses = async () => {
  try { const res = await request.get('/course/list'); courseOptions.value = res.data || [] }
  catch {}
}

// 获取计划列表
const getPlanList = async () => {
  loading.value = true
  try {
    const res = await request.get('/teaching-plan/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        majorId: searchForm.value.majorId || undefined
      }
    })
    planList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    planList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 按条件查询课程
const search = () => { currentPage.value = 1; getPlanList() }
// 重置查询条件
const resetSearch = () => { searchForm.value = { majorId: '', grade: '' }; currentPage.value = 1; getPlanList() }

// 添加计划
const addPlan = () => {
  editMode.value = false
  planForm.value = { planId: '', majorId: '', courseId: '', semesterType: 1, courseNature: 'compulsory', isPrerequisite: 0, prerequisiteIds: '' }
  dialogVisible.value = true
}

// 编辑计划
const editPlan = (row) => {
  editMode.value = true
  planForm.value = { ...row }
  dialogVisible.value = true
}

// 删除计划
const deletePlan = async (planId) => {
  await ElMessageBox.confirm('确认删除该教学计划？', '提示', { type: 'warning' })
  try {
    await request.delete(`/teaching-plan/${planId}`)
    ElMessage.success('删除成功')
    getPlanList()
  } catch { ElMessage.error('删除失败') }
}

// 保存计划
const savePlan = async () => {
  if (!planFormRef.value) return
  try {
    await planFormRef.value.validate()
  } catch { return }
  try {
    if (editMode.value) {
      await request.put('/teaching-plan', planForm.value)
    } else {
      await request.post('/teaching-plan', planForm.value)
    }
    dialogVisible.value = false
    ElMessage.success(editMode.value ? '更新成功' : '添加成功')
    getPlanList()
  } catch { ElMessage.error('保存失败') }
}

// 处理每页条数变化
const handleSizeChange = (size) => { pageSize.value = size; getPlanList() }
// 处理页码变化
const handleCurrentChange = (current) => { currentPage.value = current; getPlanList() }
</script>

<style scoped>
.page-container { width: 100%; }
</style>
