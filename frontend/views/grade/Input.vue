<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #f97316, #fb923c);">
          <el-icon :size="22"><EditPen /></el-icon>
        </div>
        <div>
          <div class="page-header-title">成绩录入</div>
          <div class="page-header-desc">录入学生课程成绩并提交审核</div>
        </div>
      </div>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchForm.courseId" placeholder="课程ID" style="width: 150px" clearable />
        <el-input v-model="searchForm.semesterId" placeholder="学期ID" style="width: 150px" clearable />
        <el-button type="primary" @click="loadData">查询</el-button>
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
        <el-table-column label="平时成绩" width="130">
          <template #default="{ row }">
            <el-input-number
              v-if="row.status === 'pending' || row.status === 'rejected'"
              v-model="row.usualScore"
              :min="0"
              :max="100"
              :precision="1"
              size="small"
              controls-position="right"
            />
            <span v-else>{{ row.usualScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="考试成绩" width="130">
          <template #default="{ row }">
            <el-input-number
              v-if="row.status === 'pending' || row.status === 'rejected'"
              v-model="row.examScore"
              :min="0"
              :max="100"
              :precision="1"
              size="small"
              controls-position="right"
            />
            <span v-else>{{ row.examScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总评" width="80">
          <template #default="{ row }">
            <span>{{ calcTotal(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'pending' ? 'warning' : row.status === 'approved' ? 'success' : 'danger'" size="small">
              {{ row.status === 'pending' ? '待审核' : row.status === 'approved' ? '已通过' : '已驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending' || row.status === 'rejected'"
              size="small" type="primary" link @click="submitGrade(row)"
            >提交</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="action-bar">
        <el-button type="primary" @click="batchSubmit">批量提交</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { useStore } from 'vuex'
import request from '../../utils/request'

const store = useStore()
const loading = ref(false)
const tableData = ref([])
const searchForm = ref({ courseId: '', semesterId: '' })

onMounted(() => { loadData() })

const calcTotal = (row) => {
  if (row.usualScore == null && row.examScore == null) return '-'
  const usual = row.usualScore || 0
  const exam = row.examScore || 0
  return (usual * 0.3 + exam * 0.7).toFixed(1)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/grade/page', { params: { current: 1, size: 100, ...searchForm.value } })
    if (res?.data?.records) tableData.value = res.data.records
    else tableData.value = []
  } catch {
    tableData.value = []
  } finally { loading.value = false }
}

const submitGrade = async (row) => {
  if (row.usualScore == null && row.examScore == null) {
    ElMessage.warning('平时成绩和考试成绩至少填写一项')
    return
  }
  try {
    const payload = {
      studentId: row.studentId,
      courseId: row.courseId,
      semesterId: row.semesterId,
      teacherId: store.state.user?.relatedId,
      usualScore: row.usualScore,
      examScore: row.examScore
    }
    if (row.gradeId) {
      payload.gradeId = row.gradeId
      payload.status = 'pending'
      await request.put('/grade', payload)
    } else {
      await request.post('/grade', payload)
    }
    ElMessage.success('成绩提交成功')
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  }
}

const batchSubmit = async () => {
  const pendingData = tableData.value.filter(r => (r.status === 'pending' || r.status === 'rejected') && (r.usualScore != null || r.examScore != null))
  if (pendingData.length === 0) {
    ElMessage.warning('没有可提交的成绩记录')
    return
  }
  try {
    const payload = pendingData.map(r => ({
      gradeId: r.gradeId,
      studentId: r.studentId,
      courseId: r.courseId,
      semesterId: r.semesterId,
      teacherId: store.state.user?.relatedId,
      usualScore: r.usualScore,
      examScore: r.examScore,
      status: 'pending'
    }))
    await request.post('/grade/batch', payload)
    ElMessage.success('批量提交成功')
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '批量提交失败')
  }
}
</script>

<style scoped>
.page-container { width: 100%; }
</style>
