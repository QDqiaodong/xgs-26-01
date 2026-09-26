<template>
  <div class="container mx-auto p-4">
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-4">
        <button @click="$router.back()" class="btn btn-outline-secondary">
          <ArrowLeft /> 返回
        </button>
        <h1 class="text-2xl font-bold">匹配结果详情</h1>
        <el-button
          v-if="canCreatePlan"
          type="success"
          :loading="savingPlan"
          @click="saveAsPlan"
        >
          保存为推演方案（评审发布）
        </el-button>
      </div>
    </div>

    <div v-if="matchRecord" class="space-y-6">
      <el-card shadow="hover">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">匹配基本信息</span>
            <el-tag :type="matchRecord.hasGap ? 'danger' : 'success'" size="large">
              {{ matchRecord.hasGap ? '⚠ 存在师资缺口' : '✓ 匹配充足' }}
            </el-tag>
          </div>
        </template>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div class="text-center p-4 bg-gray-50 rounded-lg">
            <div class="text-3xl font-bold text-primary">{{ matchRecord.courseName }}</div>
            <div class="text-sm text-gray-500">课程名称</div>
          </div>
          <div class="text-center p-4 bg-gray-50 rounded-lg">
            <div class="text-3xl font-bold" :class="matchRecord.bestScore >= 60 ? 'text-green-600' : 'text-red-600'">
              {{ matchRecord.bestScore }}%
            </div>
            <div class="text-sm text-gray-500">最佳匹配度</div>
          </div>
          <div class="text-center p-4 bg-gray-50 rounded-lg">
            <div class="text-3xl font-bold text-blue-600">{{ matchRecord.matchCount }}</div>
            <div class="text-sm text-gray-500">匹配讲师数</div>
          </div>
          <div class="text-center p-4 bg-gray-50 rounded-lg">
            <div class="text-3xl font-bold text-purple-600">{{ matchRecord.courseIndustry }}</div>
            <div class="text-sm text-gray-500">所属行业</div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover">
        <template #header>
          <span class="font-semibold">权重配置</span>
        </template>
        <div class="grid grid-cols-1 md:grid-cols-5 gap-4">
          <div class="text-center">
            <div class="text-xl font-semibold text-primary">{{ matchRecord.teachingYearsWeight * 100 }}%</div>
            <div class="text-sm text-gray-500">授课年限</div>
          </div>
          <div class="text-center">
            <div class="text-xl font-semibold text-success">{{ matchRecord.qualificationWeight * 100 }}%</div>
            <div class="text-sm text-gray-500">专业资质</div>
          </div>
          <div class="text-center">
            <div class="text-xl font-semibold text-warning">{{ matchRecord.ratingWeight * 100 }}%</div>
            <div class="text-sm text-gray-500">学员口碑</div>
          </div>
          <div class="text-center">
            <div class="text-xl font-semibold text-danger">{{ matchRecord.expertiseWeight * 100 }}%</div>
            <div class="text-sm text-gray-500">行业擅长</div>
          </div>
          <div class="text-center">
            <div class="text-xl font-semibold text-info">{{ matchRecord.styleWeight * 100 }}%</div>
            <div class="text-sm text-gray-500">授课风格</div>
          </div>
        </div>
      </el-card>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2">
          <el-card shadow="hover">
            <template #header>
              <span class="font-semibold">匹配排名详情</span>
            </template>
            <el-table :data="matchDetails" border>
              <el-table-column prop="rank" label="排名" width="60">
                <template #default="{ row }">
                  <span v-if="row.rank === 1" class="text-yellow-500 font-bold">🥇</span>
                  <span v-else-if="row.rank === 2" class="text-gray-400 font-bold">🥈</span>
                  <span v-else-if="row.rank === 3" class="text-amber-600 font-bold">🥉</span>
                  <span v-else>{{ row.rank }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="instructorName" label="讲师姓名" />
              <el-table-column prop="totalScore" label="综合得分">
                <template #default="{ row }">
                  <span :class="row.totalScore >= 60 ? 'text-green-600' : 'text-red-600'" class="font-semibold">
                    {{ row.totalScore }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="teachingYearsScore" label="年限得分" />
              <el-table-column prop="qualificationScore" label="资质得分" />
              <el-table-column prop="ratingScore" label="口碑得分" />
              <el-table-column prop="expertiseScore" label="行业得分" />
              <el-table-column prop="styleScore" label="风格得分" />
              <el-table-column label="操作">
                <template #default="{ row }">
                  <button @click="viewInstructor(row.instructorId)" class="btn btn-sm btn-primary">
                    <User /> 查看讲师
                  </button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>

        <div class="lg:col-span-1">
          <el-card shadow="hover">
            <template #header>
              <span class="font-semibold">Top3 雷达图对比</span>
            </template>
            <div ref="radarChartRef" class="w-full h-80"></div>
          </el-card>

          <el-card shadow="hover" class="mt-4">
            <template #header>
              <span class="font-semibold">匹配统计</span>
            </template>
            <div class="space-y-3">
              <div class="flex justify-between">
                <span class="text-gray-500">匹配时间</span>
                <span>{{ formatDate(matchRecord.createdAt) }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-500">匹配类型</span>
                <span>{{ matchRecord.batchMatch ? '批量匹配' : '单课程匹配' }}</span>
              </div>
              <div v-if="matchRecord.batchMatch" class="flex justify-between">
                <span class="text-gray-500">批量ID</span>
                <span class="text-xs">{{ matchRecord.batchId }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-500">操作人</span>
                <span>{{ matchRecord.operator }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-500">算法版本</span>
                <span>{{ matchRecord.algorithmVersion }}</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ArrowLeft, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { matchApi, planApi, type MatchRecord, type MatchRecordDetail } from '../api'

const route = useRoute()
const router = useRouter()
const recordId = Number(route.params.id)
const matchRecord = ref<MatchRecord | null>(null)
const matchDetails = ref<MatchRecordDetail[]>([])
const radarChartRef = ref<HTMLElement | null>(null)
let radarChart: echarts.ECharts | null = null

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const loadMatchRecord = async () => {
  try {
    const response = await matchApi.getById(recordId)
    matchRecord.value = response.data
  } catch (error) {
    ElMessage.error('加载匹配记录失败')
  }
}

const loadMatchDetails = async () => {
  try {
    const response = await matchApi.getDetails(recordId)
    matchDetails.value = response.data
    initRadarChart()
  } catch (error) {
    ElMessage.error('加载匹配详情失败')
  }
}

const initRadarChart = () => {
  if (!radarChartRef.value || matchDetails.value.length === 0) return
  
  nextTick(() => {
    if (radarChart) {
      radarChart.dispose()
    }
    radarChart = echarts.init(radarChartRef.value)
    
    const top3 = matchDetails.value.slice(0, 3)
    const colors = ['#409EFF', '#67C23A', '#E6A23C']
    
    const option: echarts.EChartsOption = {
      radar: {
        indicator: [
          { name: '授课年限', max: 100 },
          { name: '专业资质', max: 100 },
          { name: '学员口碑', max: 100 },
          { name: '行业擅长', max: 100 },
          { name: '授课风格', max: 100 }
        ],
        shape: 'polygon',
        splitNumber: 5
      },
      series: [{
        type: 'radar',
        data: top3.map((detail, index) => ({
          value: [
            detail.teachingYearsScore,
            detail.qualificationScore,
            detail.ratingScore,
            detail.expertiseScore,
            detail.styleScore
          ],
          name: `${detail.instructorName} (${detail.totalScore})`,
          areaStyle: { opacity: 0.2 },
          lineStyle: { width: 2, color: colors[index] },
          itemStyle: { color: colors[index] }
        }))
      }],
      legend: {
        data: top3.map(d => `${d.instructorName} (${d.totalScore})`)
      }
    }
    
    radarChart.setOption(option)
    
    window.addEventListener('resize', () => {
      radarChart?.resize()
    })
  })
}

const viewInstructor = (instructorId: number) => {
  router.push(`/instructors/${instructorId}`)
}

const savingPlan = ref(false)
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
const canCreatePlan = computed(() => (currentUser.roles || []).includes('COURSE_ADMIN'))

const saveAsPlan = async () => {
  try {
    const { value: name } = await ElMessageBox.prompt(
      '方案将固化本次推演的课程要求、权重配置、候选讲师关键资料、五项评分与排序，之后不会按最新资料重算。',
      '保存为推演方案草稿',
      {
        confirmButtonText: '保存草稿',
        cancelButtonText: '取消',
        inputPlaceholder: '方案名称（可选，留空自动生成版本名）',
        inputValue: matchRecord.value?.courseName ? matchRecord.value.courseName + ' 推演方案' : ''
      }
    )
    savingPlan.value = true
    const res = await planApi.createFromRecord(recordId, name || undefined)
    ElMessage.success(`草稿 v${res.data.versionNo} 已创建`)
    router.push(`/plans/${res.data.id}`)
  } catch (e: any) {
    if (e !== 'cancel' && e?.response) {
      ElMessage.error(e.response?.data?.message || '保存方案失败')
    }
  } finally {
    savingPlan.value = false
  }
}

onMounted(() => {
  loadMatchRecord()
  loadMatchDetails()
})
</script>