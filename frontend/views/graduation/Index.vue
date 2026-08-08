<!-- 毕业审核主页面组件，负责处理毕业审核模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-header-title">毕业管理</div>
        <div class="page-header-desc">毕业资格审核、学位授予与补修课程查看</div>
      </div>
      <div class="action-group">
        <el-button type="primary" @click="loadData">刷新</el-button>
        <el-button type="success" @click="batchAudit">批量审核</el-button>
      </div>
    </div>

    <PageErrorState v-if="listError" :retrying="loading" title="毕业审核数据加载失败" @retry="loadData" />
    <div v-if="!listError" class="stats-grid">
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

    <el-card v-if="!listError">
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
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="auditOne(row)">审核</el-button>
            <el-button link type="warning" @click="viewRemedialCourses(row)">补修课程</el-button>
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

    <el-dialog v-model="remedialDialogVisible" title="补修课程" width="860px">
      <div v-loading="remedialLoading">
        <div class="remedial-summary">
          <div class="remedial-title">
            {{ remedialInfo.studentName || '-' }}（{{ remedialInfo.studentId || '-' }}）
          </div>
          <div class="remedial-metrics">
            <span>学分进度：{{ remedialInfo.totalCredits || 0 }}/{{ remedialInfo.requiredCredits || 0 }}</span>
            <span>还差学分：{{ remedialInfo.remainingCredits || 0 }}</span>
            <span>未完成必修：{{ remedialInfo.compulsoryMissingCount || 0 }}</span>
          </div>
        </div>

        <el-alert
          :title="remedialInfo.message || '毕业学分以已通过成绩为准'"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
        />

        <el-empty v-if="remedialInfo.hasTeachingPlan === false" description="当前专业未配置教学计划" />
        <el-empty v-else-if="!remedialInfo.missingCourses || remedialInfo.missingCourses.length === 0" description="当前没有待补修课程" />
        <el-table v-else :data="remedialInfo.missingCourses" stripe>
          <el-table-column prop="courseId" label="课程ID" width="110" />
          <el-table-column prop="courseName" label="课程名称" min-width="180" />
          <el-table-column prop="credits" label="学分" width="80" />
          <el-table-column prop="semesterType" label="建议学期" width="100" />
          <el-table-column label="课程性质" width="110">
            <template #default="{ row }">
              <el-tag :type="courseNatureTagType(row.courseNature)" size="small">{{ courseNatureText(row.courseNature) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="当前进度" width="110">
            <template #default="{ row }">
              <el-tag :type="progressStatusTagType(row.progressStatus)" size="small">{{ progressStatusText(row.progressStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalScore" label="最近成绩" width="100">
            <template #default="{ row }">
              <span>{{ row.totalScore ?? '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const listError = ref(false)
const tableData = ref([])
const majorOptions = ref([])
const classOptions = ref([])
const stats = ref({})
const remedialDialogVisible = ref(false)
const remedialLoading = ref(false)
const remedialInfo = ref({ missingCourses: [] })
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

// 页面挂载时初始化毕业数据
onMounted(() => {
  loadOptions()
  loadData()
})

// 加载选项
const loadOptions = async () => {
  const [majorRes, classRes] = await Promise.all([
    request.get('/major/list'),
    request.get('/class/list')
  ])
  majorOptions.value = majorRes.data || []
  classOptions.value = classRes.data || []
}

// 加载数据
const loadData = async () => {
  loading.value = true
  listError.value = false
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
        },
        skipErrorMessage: true
      }),
      request.get('/graduation/statistics', {
        params: {
          majorId: searchForm.majorId || undefined,
          classId: searchForm.classId || undefined
        },
        skipErrorMessage: true
      })
    ])
    tableData.value = pageRes.data?.records || []
    pagination.total = pageRes.data?.total || 0
    stats.value = statsRes.data || {}
  } catch {
    tableData.value = []
    pagination.total = 0
    stats.value = {}
    listError.value = true
  } finally {
    loading.value = false
  }
}

// 处理查询
const handleSearch = () => {
  pagination.current = 1
  loadData()
}

// 处理重置
const handleReset = () => {
  searchForm.studentId = ''
  searchForm.majorId = ''
  searchForm.classId = ''
  searchForm.status = ''
  pagination.current = 1
  loadData()
}

// 处理分页变化
const handlePageChange = (page) => {
  pagination.current = page
  loadData()
}

// 审核单条记录
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

// 批量审核
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

// 授予学位
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

// 处理视图补修课程
const viewRemedialCourses = async (row) => {
  remedialDialogVisible.value = true
  remedialLoading.value = true
  remedialInfo.value = {
    studentId: row.studentId,
    studentName: row.studentName,
    missingCourses: []
  }
  try {
    const res = await request.get(`/graduation/remedial/${row.studentId}`)
    remedialInfo.value = res.data || { missingCourses: [] }
  } catch (error) {
    remedialInfo.value = {
      studentId: row.studentId,
      studentName: row.studentName,
      missingCourses: [],
      message: error.message || '补修课程加载失败'
    }
  } finally {
    remedialLoading.value = false
  }
}

// 获取状态文本
const statusText = (status) => {
  const map = {
    unaudited: '未审核',
    approved: '通过',
    rejected: '未通过'
  }
  return map[status] || status
}

// 获取状态标签类型
const statusTagType = (status) => {
  const map = {
    unaudited: 'info',
    approved: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

// 获取课程性质文本
const courseNatureText = (nature) => {
  const map = {
    compulsory: '必修',
    elective_major: '专业选修',
    elective_public: '公共选修'
  }
  return map[nature] || nature
}

// 获取课程性质标签类型
const courseNatureTagType = (nature) => {
  const map = {
    compulsory: 'danger',
    elective_major: 'warning',
    elective_public: 'success'
  }
  return map[nature] || 'info'
}

// 获取进度状态文本
const progressStatusText = (status) => {
  const map = {
    not_taken: '未修',
    pending_review: '待审核',
    rejected: '已驳回',
    failed: '未通过',
    passed: '已通过'
  }
  return map[status] || status
}

// 获取进度状态标签类型
const progressStatusTagType = (status) => {
  const map = {
    not_taken: 'info',
    pending_review: 'warning',
    rejected: 'danger',
    failed: 'danger',
    passed: 'success'
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
  color: var(--ink);
}

.page-header-desc {
  margin-top: 4px;
  color: var(--text-secondary);
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
  color: var(--text-secondary);
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: var(--ink);
}

.stat-value.success {
  color: var(--sage);
}

.stat-value.danger {
  color: var(--vermilion);
}

.stat-value.primary {
  color: var(--blue);
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

.remedial-summary {
  margin-bottom: 16px;
}

.remedial-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}

.remedial-metrics {
  display: flex;
  gap: 20px;
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 13px;
  flex-wrap: wrap;
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
