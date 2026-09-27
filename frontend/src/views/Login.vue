<script setup lang="ts">
import { ref } from 'vue'
import { ElForm, ElFormItem, ElInput, ElButton, ElMessage } from 'element-plus'
import { School as BuildingIcon } from '@element-plus/icons-vue'
import { authApi, type LoginResponse } from '../api'

const form = ref({
  username: '',
  password: ''
})

const loading = ref(false)

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  
  loading.value = true
  try {
    const response = await authApi.login(form.value.username, form.value.password)
    const result: LoginResponse = response.data.data
    localStorage.setItem('token', result.token)
    localStorage.setItem('user', JSON.stringify(result))
    ElMessage.success('登录成功')
    setTimeout(() => {
      window.location.href = '/'
    }, 500)
  } catch (error) {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-500 via-purple-500 to-pink-500 flex items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8">
      <div class="text-center mb-8">
        <div class="w-16 h-16 bg-gradient-to-r from-blue-500 to-purple-500 rounded-full flex items-center justify-center mx-auto mb-4">
          <BuildingIcon class="w-8 h-8 text-white" />
        </div>
        <h1 class="text-2xl font-bold text-gray-800">职业培训讲师课程匹配系统</h1>
        <p class="text-gray-500 mt-2">智能匹配，精准推荐</p>
      </div>
      
      <ElForm :model="form" label-width="0" class="space-y-4">
        <ElFormItem>
          <ElInput 
            v-model="form.username" 
            placeholder="用户名"
            prefix-icon="User"
            size="large"
            @keyup.enter="handleLogin"
          />
        </ElFormItem>
        
        <ElFormItem>
          <ElInput 
            v-model="form.password" 
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            @keyup.enter="handleLogin"
          />
        </ElFormItem>
        
        <ElFormItem>
          <ElButton 
            type="primary" 
            size="large" 
            class="w-full"
            @click="handleLogin"
            :loading="loading"
          >
            登录
          </ElButton>
        </ElFormItem>
      </ElForm>
      
      <div class="mt-6 text-center">
        <p class="text-sm text-gray-400">
          默认账号：admin / admin123
        </p>
        <p class="text-xs text-gray-400 mt-1">
          督导：supervisor1/supervisor2；负责人：course_owner；讲师：instructor1/instructor2（密码均为 admin123）
        </p>
      </div>
    </div>
  </div>
</template>
