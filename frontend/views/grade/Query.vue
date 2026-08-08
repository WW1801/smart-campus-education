<!-- 成绩查询页面组件，负责处理成绩模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><Search /></el-icon>
        </div>
        <div>
          <div class="page-header-title">成绩查询</div>
          <div class="page-header-desc">查询学生课程成绩与审核</div>
        </div>
      </div>
    </div>

    <PageErrorState v-if="listError" :retrying="loading" title="成绩列表加载失败" @retry="loadData" />
    <el-card v-else>
      <div class="search-bar">
        <el-input v-model="searchForm.studentId" placeholder="学号" style="width: 150px" clearable />
        <el-input v-model="searchForm.courseId" placeholder="课程ID" style="width: 150px" clearable />
        <el-select v-model="searchForm.status" placeholder="审核状态" clearable style="width: 130px">
          <el-option label="待审核" value="submitted" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
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
        <el-table-column prop="usualScore" label="平时成绩" width="100" />
        <el-table-column prop="examScore" label="考试成绩" width="100" />
        <el-table-column prop="totalScore" label="总评" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'submitted' ? 'warning' : row.status === 'approved' ? 'success' : 'danger'" size="small">
              {{ row.status === 'submitted' ? '待审核' : row.status === 'approved' ? '已通过' : '已驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button v-if="row.status === 'submitted'" size="small" type="success" link @click="approveGrade(row)">通过</el-button>
            <el-button v-if="row.status === 'submitted'" size="small" type="danger" link @click="rejectGrade(row)">驳回</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const listError = ref(false)
const tableData = ref([])
const searchForm = reactive({ studentId: '', courseId: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

// 页面挂载时初始化成绩数据
onMounted(() => { loadData() })

// 重置查询条件
const resetSearch = () => {
  Object.assign(searchForm, { studentId: '', courseId: '', status: '' })
  page.current = 1
  loadData()
}

// 加载数据
const loadData = async () => {
  loading.value = true
  listError.value = false
  try {
    const res = await request.get('/grade/page', { params: { current: page.current, size: page.size, ...searchForm }, skipErrorMessage: true })
    tableData.value = res.data?.records || []
    page.total = res.data?.total || 0
  } catch {
    tableData.value = []
    page.total = 0
    listError.value = true
  } finally { loading.value = false }
}

// 处理通过成绩
const approveGrade = async (row) => {
  try {
    await request.put(`/grade/${row.gradeId}/approve`)
    ElMessage.success('审核通过')
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '审核失败')
  }
}

// 处理驳回成绩
const rejectGrade = async (row) => {
  try {
    await request.put(`/grade/${row.gradeId}/reject`, { reason: '成绩不合规' })
    ElMessage.warning('已驳回')
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '驳回失败')
  }
}
</script>

<style scoped>
.page-container { width: 100%; }
</style>
