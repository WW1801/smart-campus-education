<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #ef4444, #f87171);">
          <el-icon :size="22"><Timer /></el-icon>
        </div>
        <div>
          <div class="page-header-title">考勤记录</div>
          <div class="page-header-desc">管理学生考勤记录与状态</div>
        </div>
      </div>
      <el-button type="primary" @click="addRecord">
        <el-icon><Plus /></el-icon>
        添加记录
      </el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchForm.studentId" placeholder="搜索学号" style="width: 150px" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-input v-model="searchForm.courseId" placeholder="课程ID" style="width: 130px" clearable />
        <el-date-picker v-model="searchForm.date" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 160px" />
        <el-select v-model="searchForm.status" placeholder="考勤状态" clearable style="width: 120px">
          <el-option label="出勤" value="present" />
          <el-option label="迟到" value="late" />
          <el-option label="早退" value="early_leave" />
          <el-option label="缺勤" value="absent" />
          <el-option label="请假" value="leave" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="courseId" label="课程ID" width="100" />
        <el-table-column prop="courseName" label="课程名称" width="140" />
        <el-table-column prop="date" label="日期" width="110" />
        <el-table-column prop="status" label="考勤状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="editRecord(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="deleteRecord(row.attendanceId)">删除</el-button>
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

    <el-dialog :title="editMode ? '编辑考勤' : '添加考勤'" v-model="dialogVisible" width="520px">
      <el-form ref="recordForm" :model="recordForm" :rules="recordRules" label-width="90px">
        <el-form-item label="学号" prop="studentId">
          <el-input v-model="recordForm.studentId" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="课程ID" prop="courseId">
          <el-input v-model="recordForm.courseId" placeholder="请输入课程ID" />
        </el-form-item>
        <el-form-item label="学期ID" prop="semesterId">
          <el-input v-model="recordForm.semesterId" placeholder="请输入学期ID" />
        </el-form-item>
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="recordForm.date" type="date" placeholder="请选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="考勤状态" prop="status">
          <el-select v-model="recordForm.status" style="width: 100%">
            <el-option label="出勤" value="present" />
            <el-option label="迟到" value="late" />
            <el-option label="早退" value="early_leave" />
            <el-option label="缺勤" value="absent" />
            <el-option label="请假" value="leave" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRecord">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer, Plus, Search } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const editMode = ref(false)
const recordForm = ref({ attendanceId: '', studentId: '', courseId: '', semesterId: '', date: '', status: '' })

const searchForm = reactive({ studentId: '', courseId: '', date: '', status: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const recordRules = {
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  courseId: [{ required: true, message: '请输入课程ID', trigger: 'blur' }],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择考勤状态', trigger: 'change' }]
}

const statusLabel = (s) => ({ present: '出勤', late: '迟到', early_leave: '早退', absent: '缺勤', leave: '请假' }[s] || s)
const statusTagType = (s) => ({ present: 'success', late: 'warning', early_leave: 'warning', absent: 'danger', leave: 'info' }[s] || 'info')

const resetSearch = () => {
  Object.assign(searchForm, { studentId: '', courseId: '', date: '', status: '' })
  page.current = 1
  loadData()
}

onMounted(() => { loadData() })

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.current, limit: page.size }
    Object.keys(searchForm).forEach(key => { if (searchForm[key] !== '') params[key] = searchForm[key] })
    const res = await request.get('/attendance/list', { params }).catch(() => null)
    if (res?.data) { tableData.value = res.data.records; page.total = res.data.total }
    else { tableData.value = []; page.total = 0 }
  } finally { loading.value = false }
}

const addRecord = () => { editMode.value = false; recordForm.value = { attendanceId: '', studentId: '', courseId: '', semesterId: '', date: '', status: '' }; dialogVisible.value = true }
const editRecord = (row) => { editMode.value = true; recordForm.value = { ...row }; dialogVisible.value = true }

const deleteRecord = async (id) => {
  try { await ElMessageBox.confirm('确认删除该考勤记录？', '提示', { type: 'warning' }); await request.delete(`/attendance/${id}`); ElMessage.success('删除成功'); loadData() }
  catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

const saveRecord = async () => {
  if (!recordForm.value) return
  try { await recordForm.value.validate() } catch { return }
  try {
    if (editMode.value) await request.put('/attendance', recordForm.value)
    else await request.post('/attendance', recordForm.value)
    dialogVisible.value = false
    ElMessage.success(editMode.value ? '更新成功' : '添加成功')
    loadData()
  } catch { ElMessage.error('保存失败') }
}
</script>

<style scoped>
.page-container { width: 100%; }
</style>
