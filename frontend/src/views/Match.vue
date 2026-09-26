<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElButton, ElSelect, ElOption, ElForm, ElFormItem, ElTable, ElTableColumn, ElMessage, ElProgress } from 'element-plus'
import { MagicStick as MatchIcon } from '@element-plus/icons-vue'
import { courseApi, weightApi, matchApi, type Course, type WeightConfig, type MatchResultResponse } from '../api'

const courses = ref<Course[]>([])
const weights = ref<WeightConfig[]>([])
const industries = ref<string[]>([])

const form = ref({
  courseId: null as number | null,
  weightConfigId: null as number | null,
  industry: ''
})

const matchResult = ref<MatchResultResponse | null>(null)
const batchResults = ref<MatchResultResponse[]>([])
const loading = ref(false)
const mode = ref<'single' | 'batch'>('single')

const fetchData = async () => {
  try {
    const [courseRes, weightRes] = await Promise.all([
      courseApi.getAllList(),
      weightApi.getAll()
    ])
    courses.value = courseRes.data
    weights.value = weightRes.data
    
    const industrySet = new Set(courses.value.map(c => c.industry))
    industries.value = Array.from(industrySet)
    
    if (weights.value.length > 0) {
      form.value.weightConfigId = weights.value[0].id
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
  }
}

const handleSingleMatch = async () => {
  if (!form.value.courseId || !form.value.weightConfigId) {
    ElMessage.warning('请选择课程和权重配置')
    return
  }
  
  loading.value = true
  try {
    const response = await matchApi.match(form.value.courseId, form.value.weightConfigId)
    matchResult.value = response.data
    batchResults.value = []
    ElMessage.success('匹配完成')
  } catch (error) {
    ElMessage.error('匹配失败')
  } finally {
    loading.value = false
  }
}

const handleBatchMatch = async () => {
  if (!form.value.industry || !form.value.weightConfigId) {
    ElMessage.warning('请选择行业和权重配置')
    return
  }
  
  loading.value = true
  try {
    const response = await matchApi.batchMatch(form.value.industry, form.value.weightConfigId)
    batchResults.value = response.data
    matchResult.value = null
    ElMessage.success(`批量匹配完成，共匹配 ${batchResults.value.length} 门课程`)
  } catch (error) {
    ElMessage.error('批量匹配失败')
  } finally {
    loading.value = false
  }
}

const getScoreColor = (score: number) => {
  if (score >= 80) return 'text-green-600'
  if (score >= 60) return 'text-yellow-600'
  return 'text-red-600'
}

const getScoreBgColor = (score: number) => {
  if (score >= 80) return 'bg-green-500'
  if (score >= 60) return 'bg-yellow-500'
  return 'bg-red-500'
}

onMounted(fetchData)
</script>

<template>
  <div class="space-y-6">
    <div class="bg-white rounded-xl shadow-sm p-6">
      <div class="flex items-center gap-4 mb-6">
        <div>
          <h3 class="font-semibold text-gray-800">智能匹配</h3>
          <p class="text-sm text-gray-500 mt-1">基于五维度权重算法自动计算讲师适配度</p>
        </div>
      </div>
      
      <div class="flex items-center gap-4 mb-4">
        <ElButton 
          :type="mode === 'single' ? 'primary' : 'default'"
          @click="mode = 'single'"
        >
          单课程匹配
        </ElButton>
        <ElButton 
          :type="mode === 'batch' ? 'primary' : 'default'"
          @click="mode = 'batch'"
        >
          行业批量匹配
        </ElButton>
      </div>
      
      <ElForm :model="form" label-width="100px" inline>
        <template v-if="mode === 'single'">
          <ElFormItem label="选择课程" required>
            <ElSelect v-model="form.courseId" placeholder="请选择课程" style="width: 250px">
              <ElOption v-for="course in courses" :key="course.id" :value="course.id" :label="course.name + ' (' + course.industry + ')'" />
            </ElSelect>
          </ElFormItem>
        </template>
        <template v-else>
          <ElFormItem label="选择行业" required>
            <ElSelect v-model="form.industry" placeholder="请选择行业" style="width: 200px">
              <ElOption v-for="industry in industries" :key="industry" :value="industry" :label="industry" />
            </ElSelect>
          </ElFormItem>
        </template>
        <ElFormItem label="权重配置" required>
          <ElSelect v-model="form.weightConfigId" placeholder="请选择权重配置" style="width: 200px">
            <ElOption v-for="weight in weights" :key="weight.id" :value="weight.id" :label="weight.name" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem>
          <ElButton 
            type="primary" 
            @click="mode === 'single' ? handleSingleMatch() : handleBatchMatch()"
            :loading="loading"
          >
            <MatchIcon class="w-4 h-4 mr-1" />
            {{ mode === 'single' ? '开始匹配' : '批量匹配' }}
          </ElButton>
        </ElFormItem>
      </ElForm>
    </div>
    
    <div v-if="matchResult" class="bg-white rounded-xl shadow-sm p-6">
      <div class="flex items-center justify-between mb-4">
        <h4 class="font-semibold text-gray-800">匹配结果 - {{ matchResult.courseName }}</h4>
        <div class="flex items-center gap-2">
          <span :class="matchResult.hasGap ? 'text-red-500' : 'text-green-500'" class="text-sm font-medium">
            {{ matchResult.hasGap ? '⚠️ 师资缺口' : '✓ 匹配正常' }}
          </span>
        </div>
      </div>
      
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div class="bg-gray-50 rounded-lg p-4">
          <p class="text-sm text-gray-500">最佳匹配分数</p>
          <p :class="getScoreColor(matchResult.bestScore)" class="text-2xl font-bold">
            {{ matchResult.bestScore }}
          </p>
        </div>
        <div class="bg-gray-50 rounded-lg p-4">
          <p class="text-sm text-gray-500">匹配讲师数量</p>
          <p class="text-2xl font-bold text-blue-600">{{ matchResult.matchCount }}</p>
        </div>
        <div class="bg-gray-50 rounded-lg p-4">
          <p class="text-sm text-gray-500">所属行业</p>
          <p class="text-2xl font-bold text-gray-800">{{ matchResult.courseIndustry }}</p>
        </div>
      </div>
      
      <ElTable :data="matchResult.instructors" border stripe>
        <ElTableColumn prop="rank" label="排名" width="60" />
        <ElTableColumn prop="instructorName" label="讲师姓名" />
        <ElTableColumn label="适配分数" width="120">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <ElProgress :percentage="row.totalScore" :color="getScoreBgColor(row.totalScore)" :width="80" :show-text="false" />
              <span :class="getScoreColor(row.totalScore)" class="font-medium">{{ row.totalScore }}</span>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="授课年限" width="100">
          <template #default="{ row }">{{ row.teachingYearsScore.toFixed(0) }}</template>
        </ElTableColumn>
        <ElTableColumn label="专业资质" width="100">
          <template #default="{ row }">{{ row.qualificationScore.toFixed(0) }}</template>
        </ElTableColumn>
        <ElTableColumn label="学员口碑" width="100">
          <template #default="{ row }">{{ row.ratingScore.toFixed(0) }}</template>
        </ElTableColumn>
        <ElTableColumn label="行业擅长" width="100">
          <template #default="{ row }">{{ row.expertiseScore.toFixed(0) }}</template>
        </ElTableColumn>
        <ElTableColumn label="授课风格" width="100">
          <template #default="{ row }">{{ row.styleScore.toFixed(0) }}</template>
        </ElTableColumn>
      </ElTable>
    </div>
    
    <div v-if="batchResults.length > 0" class="space-y-6">
      <div v-for="result in batchResults" :key="result.recordId" class="bg-white rounded-xl shadow-sm p-6">
        <div class="flex items-center justify-between mb-4">
          <h4 class="font-semibold text-gray-800">{{ result.courseName }}</h4>
          <span :class="result.hasGap ? 'text-red-500' : 'text-green-500'" class="text-sm">
            {{ result.hasGap ? '⚠️ 师资缺口' : '✓ 匹配正常' }}
          </span>
        </div>
        
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
          <div class="bg-gray-50 rounded-lg p-3">
            <p class="text-xs text-gray-500">最佳分数</p>
            <p :class="getScoreColor(result.bestScore)" class="text-lg font-bold">{{ result.bestScore }}</p>
          </div>
          <div class="bg-gray-50 rounded-lg p-3">
            <p class="text-xs text-gray-500">匹配数量</p>
            <p class="text-lg font-bold text-blue-600">{{ result.matchCount }}</p>
          </div>
          <div class="bg-gray-50 rounded-lg p-3">
            <p class="text-xs text-gray-500">行业</p>
            <p class="text-lg font-bold text-gray-800">{{ result.courseIndustry }}</p>
          </div>
          <div class="bg-gray-50 rounded-lg p-3">
            <p class="text-xs text-gray-500">排名第一</p>
            <p class="text-lg font-bold text-purple-600">{{ result.instructors[0]?.instructorName || '-' }}</p>
          </div>
        </div>
        
        <ElTable :data="result.instructors.slice(0, 5)" border stripe size="small">
          <ElTableColumn prop="rank" label="排名" width="50" />
          <ElTableColumn prop="instructorName" label="讲师" />
          <ElTableColumn label="分数" width="100">
            <template #default="{ row }">
              <span :class="getScoreColor(row.totalScore)">{{ row.totalScore }}</span>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </div>
  </div>
</template>