<!-- 系统管理角色管理页面组件，负责处理系统管理模块的页面展示与交互。 -->
<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">
          <el-icon :size="22"><Setting /></el-icon>
        </div>
        <div>
          <div class="page-header-title">角色管理</div>
          <div class="page-header-desc">管理系统角色与权限配置</div>
        </div>
      </div>
      <el-button type="primary" @click="addRole">
        <el-icon><Plus /></el-icon>
        添加角色
      </el-button>
    </div>

    <PageErrorState v-if="listError" :retrying="loading" title="角色列表加载失败" @retry="getRoleList" />
    <el-card v-else>
      <el-table :data="roleList" stripe v-loading="loading">
        <el-table-column prop="roleId" label="角色ID" width="100" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="description" label="角色描述" />
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <el-button size="small" type="primary" link @click="editRole(scope.row)">编辑</el-button>
            <el-button size="small" type="success" link @click="setPermission(scope.row)">权限设置</el-button>
            <el-button size="small" type="danger" link @click="deleteRole(scope.row.roleId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="editMode ? '编辑角色' : '添加角色'" v-model="dialogVisible" width="520px">
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="roleForm.description" type="textarea" placeholder="请输入角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="权限设置" v-model="permissionDialogVisible" width="600px">
      <el-tree
        ref="permissionTreeRef"
        :data="permissionTree"
        :props="treeProps"
        show-checkbox
        node-key="permissionId"
        :default-expanded-keys="expandedKeys"
        :default-checked-keys="checkedKeys"
      />
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermission">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting, Plus } from '@element-plus/icons-vue'

const roleList = ref([])
const loading = ref(false)
const listError = ref(false)
const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const editMode = ref(false)
const roleFormRef = ref(null)
const permissionTreeRef = ref(null)
const roleForm = ref({ roleId: '', name: '', description: '' })

const roleRules = { name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }] }

const permissionTree = ref([])
const expandedKeys = ref([])
const checkedKeys = ref([])
const currentRole = ref(null)
const treeProps = { label: 'name', children: 'children' }

// 构建权限tree
const buildPermissionTree = (permissions) => {
  const map = {}
  const roots = []
  permissions.forEach(p => { map[p.permissionId] = { ...p, children: [] } })
  permissions.forEach(p => {
    if (p.parentId && map[p.parentId]) { map[p.parentId].children.push(map[p.permissionId]) }
    else { roots.push(map[p.permissionId]) }
  })
  return roots
}

// 加载权限tree
const loadPermissionTree = async () => {
  try {
    const res = await request.get('/system/permission/list')
    const permissions = res.data || []
    permissionTree.value = buildPermissionTree(permissions)
    expandedKeys.value = permissions.map(p => p.permissionId)
  } catch (error) { console.error('获取权限列表失败:', error) }
}

// 页面挂载时初始化角色数据
onMounted(() => { getRoleList(); loadPermissionTree() })

// 获取角色列表
const getRoleList = async () => {
  loading.value = true
  listError.value = false
  try {
    const res = await request.get('/system/role/list', { skipErrorMessage: true })
    roleList.value = res.data
  } catch {
    roleList.value = []
    listError.value = true
  } finally {
    loading.value = false
  }
}

// 添加角色
const addRole = () => { editMode.value = false; roleForm.value = { roleId: '', name: '', description: '' }; dialogVisible.value = true }
// 编辑角色
const editRole = (row) => { editMode.value = true; roleForm.value = { ...row }; dialogVisible.value = true }

// 删除角色
const deleteRole = async (roleId) => {
  try {
    await ElMessageBox.confirm('确认删除该角色？删除后无法继续分配此角色。', '删除角色', { type: 'warning' })
    await request.delete(`/system/role/${roleId}`, { skipErrorMessage: true })
    ElMessage.success('删除成功')
    getRoleList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '删除失败')
  }
}

// 保存角色
const saveRole = async () => {
  try {
    await roleFormRef.value?.validate()
    if (editMode.value) { await request.put('/system/role', roleForm.value) }
    else { await request.post('/system/role', roleForm.value) }
    ElMessage.success(editMode.value ? '更新成功' : '添加成功'); dialogVisible.value = false; getRoleList()
  } catch (error) { ElMessage.error('保存失败') }
}

// 设置权限
const setPermission = async (role) => {
  currentRole.value = role
  try { const res = await request.get(`/system/role/${role.roleId}/permissions`); checkedKeys.value = res.data || [] }
  catch (error) { checkedKeys.value = [] }
  permissionDialogVisible.value = true
  await nextTick()
  permissionTreeRef.value?.setCheckedKeys(checkedKeys.value)
}

// 保存权限
const savePermission = async () => {
  try {
    const permissionIds = permissionTreeRef.value
      ? [...permissionTreeRef.value.getCheckedKeys(), ...permissionTreeRef.value.getHalfCheckedKeys()]
      : checkedKeys.value
    await request.put(`/system/role/${currentRole.value.roleId}/permissions`, permissionIds)
    ElMessage.success('权限设置成功'); permissionDialogVisible.value = false
  } catch (error) { ElMessage.error('权限设置失败') }
}
</script>

<style scoped>
.page-container {
  width: 100%;
}
</style>
