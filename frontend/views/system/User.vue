<!-- 系统管理用户管理页面组件，负责处理系统管理模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><User /></el-icon>
        </div>
        <div>
          <div class="page-header-title">用户管理</div>
          <div class="page-header-desc">管理系统用户账号与角色分配</div>
        </div>
      </div>
      <el-button type="primary" @click="openDialog(null)">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
    </div>

    <el-card>
      <div class="search-bar">
        <el-input v-model="searchForm.username" placeholder="搜索用户名" style="width: 180px" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="searchForm.roleId" placeholder="筛选角色" clearable style="width: 150px">
          <el-option v-for="r in roleList" :key="r.roleId" :label="r.name" :value="r.roleId" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="userId" label="用户ID" width="120" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="roleId" label="角色" width="120">
          <template #default="{ row }">
            <el-tag>{{ getRoleName(row.roleId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="relatedId" label="关联ID" width="120" />
        <el-table-column prop="lastLogin" label="最后登录" width="170" />
        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" link @click="resetPassword(row)">重置密码</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.userId" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.userId">
          <el-input v-model="form.password" type="password" placeholder="默认123456" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" style="width: 100%" placeholder="请选择角色">
            <el-option v-for="r in roleList" :key="r.roleId" :label="r.name" :value="r.roleId" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联ID">
          <el-input v-model="form.relatedId" placeholder="学生学号或教师工号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Plus, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const roleList = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

const searchForm = reactive({ username: '', roleId: '' })
const page = reactive({ current: 1, size: 10, total: 0 })

const form = reactive({
  userId: '', username: '', password: '', name: '', roleId: '', relatedId: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const dialogTitle = ref('新增用户')

// 获取角色name
const getRoleName = (roleId) => {
  const role = roleList.value.find(r => r.roleId === roleId)
  return role ? role.name : roleId
}

// 重置查询条件
const resetSearch = () => {
  searchForm.username = ''
  searchForm.roleId = ''
  page.current = 1
  loadData()
}

// 加载角色
const loadRoles = async () => {
  const res = await request.get('/system/role/list')
  roleList.value = res.data
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/system/user/page', {
      params: { current: page.current, size: page.size, ...searchForm }
    })
    tableData.value = res.data.records
    page.total = res.data.total
  } finally {
    loading.value = false
  }
}

// 处理opendialog
const openDialog = (row) => {
  dialogTitle.value = row ? '编辑用户' : '新增用户'
  Object.assign(form, row || { userId: '', username: '', password: '', name: '', roleId: '', relatedId: '' })
  dialogVisible.value = true
}

// 处理提交
const handleSubmit = async () => {
  await formRef.value.validate()
  if (form.userId) {
    await request.put('/system/user', form)
  } else {
    await request.post('/system/user', form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

// 处理删除
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
  await request.delete(`/system/user/${row.userId}`)
  ElMessage.success('删除成功')
  loadData()
}

// 重置密码
const resetPassword = async (row) => {
  await ElMessageBox.confirm('确认重置密码为默认密码？', '提示', { type: 'warning' })
  await request.put(`/system/user/${row.userId}/reset-password`)
  ElMessage.success('密码已重置')
}

// 页面挂载时初始化用户数据
onMounted(() => {
  loadRoles()
  loadData()
})
</script>

<style scoped>
.page-container {
  width: 100%;
}
</style>
