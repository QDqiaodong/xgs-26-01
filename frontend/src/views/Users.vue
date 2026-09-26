<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElSwitch, ElMessage, ElMessageBox } from 'element-plus'
import { Plus as PlusIcon, Edit as EditIcon, Delete as DeleteIcon, User as UserIcon } from '@element-plus/icons-vue'
import { authApi, type User } from '../api'

const users = ref<User[]>([])
const dialogVisible = ref(false)
const editingUser = ref<User | null>(null)
const form = ref({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: ''
})

const fetchUsers = async () => {
  try {
    const response = await authApi.getAllUsers()
    users.value = response.data
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  }
}

const handleAdd = () => {
  editingUser.value = null
  form.value = { username: '', password: '', realName: '', phone: '', email: '' }
  dialogVisible.value = true
}

const handleEdit = (user: User) => {
  editingUser.value = user
  form.value = {
    username: user.username,
    password: '',
    realName: user.realName,
    phone: user.phone || '',
    email: user.email || ''
  }
  dialogVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await authApi.deleteUser(id)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error) {
    ElMessage.info('已取消删除')
  }
}

const handleSubmit = async () => {
  if (!form.value.username || (!form.value.password && !editingUser.value)) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  
  try {
    if (editingUser.value) {
      await authApi.updateUser(editingUser.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await authApi.createUser(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchUsers()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

onMounted(fetchUsers)
</script>

<template>
  <div class="space-y-6">
    <div class="bg-white rounded-xl shadow-sm p-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
            <UserIcon class="w-5 h-5 text-blue-600" />
          </div>
          <div>
            <h3 class="font-semibold text-gray-800">用户管理</h3>
            <p class="text-sm text-gray-500">管理系统用户和权限</p>
          </div>
        </div>
        <ElButton type="primary" @click="handleAdd">
          <PlusIcon class="w-4 h-4 mr-1" />
          添加用户
        </ElButton>
      </div>
    </div>
    
    <div class="bg-white rounded-xl shadow-sm p-6">
      <ElTable :data="users" border stripe row-key="id">
        <ElTableColumn prop="id" label="ID" width="60" />
        <ElTableColumn prop="username" label="用户名" />
        <ElTableColumn prop="realName" label="真实姓名" />
        <ElTableColumn prop="phone" label="手机号" />
        <ElTableColumn prop="email" label="邮箱" />
        <ElTableColumn prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <ElSwitch :model-value="row.enabled" disabled />
          </template>
        </ElTableColumn>
        <ElTableColumn prop="lastLoginTime" label="最后登录" width="160" />
        <ElTableColumn prop="createdAt" label="创建时间" width="160" />
        <ElTableColumn label="操作" width="150">
          <template #default="{ row }">
            <ElButton type="primary" size="small" @click="handleEdit(row as User)">
              <EditIcon class="w-4 h-4" />
            </ElButton>
            <ElButton type="danger" size="small" @click="handleDelete(row.id)">
              <DeleteIcon class="w-4 h-4" />
            </ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </div>
    
    <ElDialog v-model="dialogVisible" :title="editingUser ? '编辑用户' : '添加用户'">
      <ElForm :model="form" label-width="100px">
        <ElFormItem label="用户名" required>
          <ElInput v-model="form.username" placeholder="请输入用户名" />
        </ElFormItem>
        <ElFormItem label="密码" :required="!editingUser">
          <ElInput v-model="form.password" type="password" :placeholder="editingUser ? '留空则不修改密码' : '请输入密码'" />
        </ElFormItem>
        <ElFormItem label="真实姓名" required>
          <ElInput v-model="form.realName" placeholder="请输入真实姓名" />
        </ElFormItem>
        <ElFormItem label="手机号">
          <ElInput v-model="form.phone" placeholder="请输入手机号" />
        </ElFormItem>
        <ElFormItem label="邮箱">
          <ElInput v-model="form.email" placeholder="请输入邮箱" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>