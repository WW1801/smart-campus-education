<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-header-title">毕业管理</div>
        <div class="page-header-desc">毕业资格审核、学位授予与统计分析</div>
      </div>
      <div class="action-group">
        <el-button type="primary" @click="loadData">刷新</el-button>
        <el-button type="success" @click="batchAudit">批量审核</el-button>
      </div>
    </div>

    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="stat-label">学生总数</div>
        <div class="stat-value">{{ stats.totalStudents || 0 }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">审核通过</div>
        <div class="stat-value success">{{ stats.approvedCount || 0 }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">未通过</div>
        <div class="stat-value danger">{{ stats.rejectedCount || 0 }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">已授予学位</div>
        <div class="stat-value primary">{{ stats.degreeGrantedCount || 0 }}</div>
      </el-card>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchForm.studentId" placeholder="学号" clearable style="width: 180px" />
        <el-select v-model="searchForm.majorId" placeholder="专业" clearable style="width: 180px">
          <el-option v-for="item in majorOptions" :key="item.majorId" :label="item.name" :value="item.majorId" />
        </el-select>
        <el-select v-model="searchForm.classId" placeholder="班级" clearable style="width: 180px">
          <el-option v-for="item in filteredClassOptions" :key="item.classId" :label="item.name" :value="item.classId" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="审核状态" clearable style="width: 160px">
          <el-option label="未审核" value="unaudited" />
          <el-option label="审核通过" value="approved" />
          <el-option label="审核未通过" value="rejected" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="studentId" label="学号" width="100" />
        <el-table-column prop="studentName" label="姓名" width="110" />
        <el-table-column prop="majorId" label="专业" width="120" />
        <el-table-column prop="classId" label="班级" width="100" />
        <el-table-column label="学分进度" min-width="140">
          <template #default="{ row }">
            {{ row.totalCredits || 0 }}/{{ row.requiredCredits || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="gpa" label="绩点" width="90" />
        <el-table-column label="必修课" width="90">
          <template #default="{ row }">
            <el-tag :type="row.compulsoryPass ? 'success' : 'warning'" size="small">
              {{ row.compulsoryPass ? '通过' : '未通过' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学位状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.degreeGranted ? 'success' : 'info'" size="small">
              {{ row.degreeGranted ? '已授予' : '未授予' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditOpinion" label="审核意见" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="auditOne(row)">审核</el-button>
            <el-button
              link
              type="success"
              :disabled="row.status !== 'approved' || row.degreeGranted"
              @click="grantDegree(row)"
            >授予学位</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="pagination.current"
          :page-size="pagination.size"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const tableData = ref([])
const majorOptions = ref([])
const classOptions = ref([])
const stats = ref({})
const searchForm = reactive({
  studentId: '',
  majorId: '',
  classId: '',
  status: ''
})
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const filteredClassOptions = computed(() => {
  if (!searchForm.majorId) return classOptions.value
  return classOptions.value.filter(item => item.majorId === searchForm.majorId)
})

onMounted(() => {
  loadOptions()
  loadData()
})

const loadOptions = async () => {
  const [majorRes, classRes] = await Promise.all([
    request.get('/major/list'),
    request.get('/class/list')
  ])
  majorOptions.value = majorRes.data || []
  classOptions.value = classRes.data || []
}

const loadData = async () => {
  loading.value = true
  try {
    const [pageRes, statsRes] = await Promise.all([
      request.get('/graduation/page', {
        params: {
          current: pagination.current,
          size: pagination.size,
          studentId: searchForm.studentId || undefined,
          majorId: searchForm.majorId || undefined,
          classId: searchForm.classId || undefined,
          status: searchForm.status || undefined
        }
      }),
      request.get('/graduation/statistics', {
        params: {
          majorId: searchForm.majorId || undefined,
          classId: searchForm.classId || undefined
        }
      })
    ])
    tableData.value = pageRes.data?.records || []
    pagination.total = pageRes.data?.total || 0
    stats.value = statsRes.data || {}
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  searchForm.studentId = ''
  searchForm.majorId = ''
  searchForm.classId = ''
  searchForm.status = ''
  pagination.current = 1
  loadData()
}

const handlePageChange = (page) => {
  pagination.current = page
  loadData()
}

const auditOne = async (row) => {
  try {
    await ElMessageBox.confirm(`确认审核学生 ${row.studentName} (${row.studentId})？`, '毕业审核', { type: 'warning' })
  } catch {
    return
  }
  await request.post(`/graduation/audit/${row.studentId}`)
  ElMessage.success('毕业审核完成')
  loadData()
}

const batchAudit = async () => {
  try {
    await ElMessageBox.confirm('将对当前筛选范围内学生执行批量毕业审核，是否继续？', '批量审核', { type: 'warning' })
  } catch {
    return
  }
  const res = await request.post('/graduation/batch-audit', {
    majorId: searchForm.majorId || undefined,
    classId: searchForm.classId || undefined
  })
  ElMessage.success(`批量审核完成，共处理 ${res.data?.length || 0} 人`)
  loadData()
}

const grantDegree = async (row) => {
  try {
    await ElMessageBox.confirm(`确认向 ${row.studentName} 授予学位？`, '学位授予', { type: 'warning' })
  } catch {
    return
  }
  await request.put(`/graduation/degree/${row.studentId}`)
  ElMessage.success('学位授予成功')
  loadData()
}

const statusText = (status) => {
  const map = {
    unaudited: '未审核',
    approved: '通过',
    rejected: '未通过'
  }
  return map[status] || status
}

const statusTagType = (status) => {
  const map = {
    unaudited: 'info',
    approved: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
}
</script>

<style scoped>
.page-container {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header-title {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
}

.page-header-desc {
  margin-top: 4px;
  color: #6b7280;
}

.action-group {
  display: flex;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 14px;
}

.stat-label {
  color: #6b7280;
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.stat-value.success {
  color: #16a34a;
}

.stat-value.danger {
  color: #dc2626;
}

.stat-value.primary {
  color: #2563eb;
}

.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 960px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
