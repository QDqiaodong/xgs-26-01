<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElMessage, ElMessageBox } from 'element-plus'
import { Clock as HistoryIcon, Delete as DeleteIcon, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { matchApi, type MatchRecord, type MatchRecordDetail } from '../api'

const records = ref<MatchRecord[]>([])
const dialogVisible = ref(false)
const details = ref<MatchRecordDetail[]>([])
const currentRecord = ref<MatchRecord | null>(null)

const fetchRecords = async () => {
  try {
    const response = await matchApi.getAll(0, 100)
    const data = response.data
    records.value = data.content || []
  } catch (error) {
    ElMessage.error('获取推演记录失败')
  }
}

const viewDetails = async (record: MatchRecord) => {
  currentRecord.value = record
  try {
    const response = await matchApi.getDetails(record.id)
    details.value = response.data
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const deleteRecord = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该推演记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await matchApi.delete(id)
    ElMessage.success('删除成功')
    fetchRecords()
  } catch (error) {
    ElMessage.info('已取消删除')
  }
}

const getScoreColor = (score: number) => {
  if (score >= 80) return 'text-green-600'
  if (score >= 60) return 'text-yellow-600'
  return 'text-red-600'
}

onMounted(fetchRecords)
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h3 class="font-semibold text-gray-800">推演历史记录</h3>
        <p class="text-sm text-gray-500 mt-1">留存全部历史匹配推演记录，支持全周期查询</p>
      </div>
      <ElButton @click="fetchRecords()">
        <RefreshIcon class="w-4 h-4 mr-1" />
        刷新
      </ElButton>
    </div>
    
    <div v-if="records.length === 0" class="text-center py-12">
      <div class="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center mx-auto mb-4">
        <HistoryIcon class="w-8 h-8 text-gray-400" />
      </div>
      <p class="text-gray-500">暂无推演记录</p>
      <p class="text-sm text-gray-400 mt-1">前往智能匹配页面进行讲师匹配</p>
    </div>
    
    <ElTable v-else :data="records" border stripe row-key="id">
      <ElTableColumn prop="id" label="记录ID" width="80" />
      <ElTableColumn prop="courseName" label="课程名称" />
      <ElTableColumn prop="courseIndustry" label="行业" />
      <ElTableColumn label="最佳分数" width="100">
        <template #default="{ row }">
          <span :class="getScoreColor(row.bestScore)" class="font-medium">{{ row.bestScore }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="matchCount" label="匹配数" width="80" />
      <ElTableColumn label="缺口状态" width="100">
        <template #default="{ row }">
          <span :class="row.hasGap ? 'text-red-500' : 'text-green-500'" class="text-sm">
            {{ row.hasGap ? '缺口' : '正常' }}
          </span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="批量匹配" width="80">
        <template #default="{ row }">
          <span v-if="row.batchMatch" class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-blue-100 text-blue-600">
            批量
          </span>
          <span v-else class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-600">
            单课
          </span>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="createdAt" label="推演时间" width="180" />
      <ElTableColumn label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <ElButton type="primary" size="small" @click="viewDetails(row as MatchRecord)">
            详情
          </ElButton>
          <ElButton type="danger" size="small" @click="deleteRecord(row.id)">
            <DeleteIcon class="w-4 h-4" />
          </ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
    
    <ElDialog 
      title="推演详情" 
      v-model="dialogVisible"
      width="800px"
    >
      <div v-if="currentRecord" class="mb-4">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div class="bg-gray-50 rounded-lg p-3">
            <p class="text-xs text-gray-500">课程名称</p>
            <p class="font-medium">{{ currentRecord.courseName }}</p>
          </div>
          <div class="bg-gray-50 rounded-lg p-3">
            <p class="text-xs text-gray-500">所属行业</p>
            <p class="font-medium">{{ currentRecord.courseIndustry }}</p>
          </div>
          <div class="bg-gray-50 rounded-lg p-3">
            <p class="text-xs text-gray-500">最佳分数</p>
            <p :class="getScoreColor(currentRecord.bestScore)" class="font-medium">{{ currentRecord.bestScore }}</p>
          </div>
          <div class="bg-gray-50 rounded-lg p-3">
            <p class="text-xs text-gray-500">缺口状态</p>
            <p :class="currentRecord.hasGap ? 'text-red-500' : 'text-green-500'" class="font-medium">
              {{ currentRecord.hasGap ? '有缺口' : '无缺口' }}
            </p>
          </div>
        </div>
        
        <div class="mt-4 p-4 bg-gray-50 rounded-lg">
          <p class="text-sm font-medium text-gray-700 mb-2">权重配置</p>
          <div class="grid grid-cols-5 gap-4 text-sm">
            <div>
              <p class="text-gray-500">授课年限</p>
              <p class="font-medium">{{ (currentRecord.teachingYearsWeight * 100).toFixed(0) }}%</p>
            </div>
            <div>
              <p class="text-gray-500">专业资质</p>
              <p class="font-medium">{{ (currentRecord.qualificationWeight * 100).toFixed(0) }}%</p>
            </div>
            <div>
              <p class="text-gray-500">学员口碑</p>
              <p class="font-medium">{{ (currentRecord.ratingWeight * 100).toFixed(0) }}%</p>
            </div>
            <div>
              <p class="text-gray-500">行业擅长</p>
              <p class="font-medium">{{ (currentRecord.expertiseWeight * 100).toFixed(0) }}%</p>
            </div>
            <div>
              <p class="text-gray-500">授课风格</p>
              <p class="font-medium">{{ (currentRecord.styleWeight * 100).toFixed(0) }}%</p>
            </div>
          </div>
        </div>
      </div>
      
      <ElTable :data="details" border stripe size="small">
        <ElTableColumn prop="rank" label="排名" width="60" />
        <ElTableColumn prop="instructorName" label="讲师姓名" />
        <ElTableColumn label="总分数" width="80">
          <template #default="{ row }">
            <span :class="getScoreColor(row.totalScore)">{{ row.totalScore }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="授课年限" width="80">
          <template #default="{ row }">{{ row.teachingYearsScore.toFixed(0) }}</template>
        </ElTableColumn>
        <ElTableColumn label="专业资质" width="80">
          <template #default="{ row }">{{ row.qualificationScore.toFixed(0) }}</template>
        </ElTableColumn>
        <ElTableColumn label="学员口碑" width="80">
          <template #default="{ row }">{{ row.ratingScore.toFixed(0) }}</template>
        </ElTableColumn>
        <ElTableColumn label="行业擅长" width="80">
          <template #default="{ row }">{{ row.expertiseScore.toFixed(0) }}</template>
        </ElTableColumn>
        <ElTableColumn label="授课风格" width="80">
          <template #default="{ row }">{{ row.styleScore.toFixed(0) }}</template>
        </ElTableColumn>
      </ElTable>
      
      <template #footer>
        <ElButton @click="dialogVisible = false">关闭</ElButton>
      </template>
    </ElDialog>
  </div>
</template>