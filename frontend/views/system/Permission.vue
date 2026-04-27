<!-- 系统管理权限管理页面组件，负责处理系统管理模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon" style="background: linear-gradient(135deg, #ff9f1c, #ffb74d);">
          <el-icon :size="22"><Lock /></el-icon>
        </div>
        <div>
          <div class="page-header-title">权限管理</div>
          <div class="page-header-desc">管理系统权限资源与分配</div>
        </div>
      </div>
      <el-button type="primary" @click="addPermission">
        <el-icon><Plus /></el-icon>
        添加权限
      </el-button>
    </div>

    <el-card>
      <el-table :data="permissionList" stripe>
        <el-table-column prop="permissionId" label="权限ID" width="100" />
        <el-table-column prop="name" label="权限名称" width="150" />
        <el-table-column prop="code" label="权限代码" width="150" />
        <el-table-column prop="description" label="权限描述" />
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" type="primary" link @click="editPermission(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="deletePermission(scope.row.permissionId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="editMode ? '编辑权限' : '添加权限'" v-model="dialogVisible" width="520px">
      <el-form ref="permissionFormRef" :model="permissionForm" :rules="permissionRules" label-width="100px">
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="permissionForm.name" placeholder="请输入权限名称" />
        </el-form-item>
        <el-form-item label="权限代码" prop="code">
          <el-input v-model="permissionForm.code" placeholder="请输入权限代码" />
        </el-form-item>
        <el-form-item label="权限描述" prop="description">
          <el-input v-model="permissionForm.description" type="textarea" placeholder="请输入权限描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermission">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Lock, Plus } from '@element-plus/icons-vue'

const permissionList = ref([])
const dialogVisible = ref(false)
const editMode = ref(false)
const permissionFormRef = ref(null)
const permissionForm = ref({ permissionId: '', name: '', code: '', description: '' })

const permissionRules = {
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入权限代码', trigger: 'blur' }]
}

// 页面挂载时初始化权限数据
onMounted(() => { getPermissionList() })

// 获取权限列表
const getPermissionList = async () => {
  try { const res = await request.get('/system/permission/list'); permissionList.value = res.data }
  catch (error) { ElMessage.error('获取权限列表失败') }
}

// 添加权限
const addPermission = () => { editMode.value = false; permissionForm.value = { permissionId: '', name: '', code: '', description: '' }; dialogVisible.value = true }
// 编辑权限
const editPermission = (row) => { editMode.value = true; permissionForm.value = { ...row }; dialogVisible.value = true }

// 删除权限
const deletePermission = async (permissionId) => {
  try { await request.delete(`/system/permission/${permissionId}`); ElMessage.success('删除成功'); getPermissionList() }
  catch (error) { ElMessage.error('删除失败') }
}

// 保存权限
const savePermission = async () => {
  try {
    await permissionFormRef.value?.validate()
    if (editMode.value) { await request.put('/system/permission', permissionForm.value) }
    else { await request.post('/system/permission', permissionForm.value) }
    ElMessage.success(editMode.value ? '更新成功' : '添加成功'); dialogVisible.value = false; getPermissionList()
  } catch (error) { ElMessage.error('保存失败') }
}
</script>

<style scoped>
.page-container {
  width: 100%;
}
</style>
