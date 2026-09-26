<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElMessage } from 'element-plus'
import { Warning as WarningIcon, Refresh as RefreshIcon, ArrowRight as ArrowRightIcon } from '@element-plus/icons-vue'
import { matchApi, type GapWarningResponse } from '../api'
import { useRouter } from 'vue-router'

const warnings = ref<GapWarningResponse[]>([])
const router = useRouter()

const fetchWarnings = async () => {
  try {
    const response = await matchApi.getGaps()
    warnings.value = response.data
  } catch (error) {
    ElMessage.error('获取缺口预警失败')
  }
}

const getScoreColor = (score: number) => {
  if (score >= 60) return 'text-yellow-600'
  if (score >= 40) return 'text-orange-600'
  return 'text-red-600'
}

const goToMatch = () => {
  router.push('/match')
}

onMounted(fetchWarnings)
</script>

<template>
  <div class="space-y-6">
    <div class="bg-gradient-to-r from-red-50 to-orange-50 rounded-xl p-6 border border-red-100">
      <div class="flex items-center gap-4">
        <div class="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center">
          <WarningIcon class="w-8 h-8 text-red-600" />
        </div>
        <div>
          <h3 class="text-xl font-bold text-gray-800">师资缺口风险预警</h3>
          <p class="text-gray-600 mt-1">系统自动识别无适配讲师的课程，提醒您及时补充师资</p>
          <div class="flex items-center gap-4 mt-3">
            <span class="text-sm text-gray-500">
              当前共有 <span class="font-bold text-red-600">{{ warnings.length }}</span> 门课程存在师资缺口
            </span>
            <ElButton type="primary" @click="goToMatch()">
              <ArrowRightIcon class="w-4 h-4 mr-1" />
              立即匹配
            </ElButton>
          </div>
        </div>
      </div>
    </div>
    
    <div class="bg-white rounded-xl shadow-sm p-6">
      <div class="flex items-center justify-between mb-6">
        <h3 class="font-semibold text-gray-800">缺口预警列表</h3>
        <ElButton @click="fetchWarnings()">
          <RefreshIcon class="w-4 h-4 mr-1" />
          刷新
        </ElButton>
      </div>
      
      <div v-if="warnings.length === 0" class="text-center py-12">
        <div class="w-16 h-16 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
          </svg>
        </div>
        <p class="text-gray-500">暂无师资缺口预警</p>
        <p class="text-sm text-gray-400 mt-1">所有课程均有适配讲师</p>
      </div>
      
      <ElTable v-else :data="warnings" border stripe>
        <ElTableColumn prop="courseId" label="课程ID" width="80" />
        <ElTableColumn prop="courseName" label="课程名称" />
        <ElTableColumn prop="industry" label="所属行业" />
        <ElTableColumn prop="difficulty" label="难度" width="80" />
        <ElTableColumn label="最佳匹配分数" width="120">
          <template #default="{ row }">
            <span :class="getScoreColor(row.bestScore)" class="font-medium">
              {{ row.bestScore }}
            </span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="matchCount" label="匹配讲师数" width="100" />
        <ElTableColumn prop="lastMatchTime" label="上次匹配时间" width="180" />
        <ElTableColumn label="风险等级" width="100">
          <template #default="{ row }">
            <span 
              v-if="row.bestScore < 40" 
              class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-red-100 text-red-600"
            >
              高风险
            </span>
            <span 
              v-else-if="row.bestScore < 60" 
              class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-orange-100 text-orange-600"
            >
              中风险
            </span>
            <span 
              v-else 
              class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-yellow-100 text-yellow-600"
            >
              低风险
            </span>
          </template>
        </ElTableColumn>
      </ElTable>
    </div>
  </div>
</template>