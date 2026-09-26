<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElSelect, ElOption, ElInput, ElButton, ElDatePicker, ElPagination } from 'element-plus'
import { Search as SearchIcon, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { api } from '../api'

interface OperationLog {
  id: number
  operationType: string
  targetType: string
  targetId: number | null
  operator: string
  detail: string
  ipAddress: string
  createdAt: string
}

const logs = ref<OperationLog[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const filters = ref({
  operationType: '',
  targetType: '',
  operator: '',
  keyword: '',
  startDate: '',
  endDate: ''
})

const operationTypes = ['CREATE', 'UPDATE', 'DELETE', 'MATCH', 'BATCH_MATCH', 'EXPORT']
const targetTypes = ['COURSE', 'INSTRUCTOR', 'WEIGHT_CONFIG', 'MATCH_RECORD', 'INDUSTRY']

const fetchLogs = async () => {
  try {
    const response = await api.get('/logs', { 
      params: {
        page: page.value - 1,
        size: size.value,
        ...filters.value
      }
    })
    const data = response.data
    logs.value = data.content || []
    total.value = data.totalElements || logs.value.length
  } catch (error) {
    console.error('Failed to fetch logs:', error)
  }
}

const handleSearch = () => {
  page.value = 1
  fetchLogs()
}

const handleReset = () => {
  filters.value = {
    operationType: '',
    targetType: '',
    operator: '',
    keyword: '',
    startDate: '',
    endDate: ''
  }
  page.value = 1
  fetchLogs()
}

const getOperationTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    CREATE: '创建',
    UPDATE: '更新',
    DELETE: '删除',
    MATCH: '匹配',
    BATCH_MATCH: '批量匹配',
    EXPORT: '导出'
  }
  return map[type] || type
}

const getTargetTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    COURSE: '课程',
    INSTRUCTOR: '讲师',
    WEIGHT_CONFIG: '权重配置',
    MATCH_RECORD: '匹配记录',
    INDUSTRY: '行业'
  }
  return map[type] || type
}

onMounted(fetchLogs)
</script>

<template>
  <div class="space-y-6">
    <div class="bg-white rounded-xl shadow-sm p-6">
      <div class="flex items-center justify-between mb-6">
        <h3 class="font-semibold text-gray-800">操作日志</h3>
        <ElButton @click="fetchLogs">
          <RefreshIcon class="w-4 h-4" />
          刷新
        </ElButton>
      </div>
      
      <div class="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-4">
        <ElSelect v-model="filters.operationType" placeholder="操作类型" clearable style="width: 100%">
          <ElOption v-for="type in operationTypes" :key="type" :value="type" :label="getOperationTypeLabel(type)" />
        </ElSelect>
        
        <ElSelect v-model="filters.targetType" placeholder="目标类型" clearable style="width: 100%">
          <ElOption v-for="type in targetTypes" :key="type" :value="type" :label="getTargetTypeLabel(type)" />
        </ElSelect>
        
        <ElInput v-model="filters.operator" placeholder="操作人" clearable />
        
        <ElInput v-model="filters.keyword" placeholder="关键词" clearable prefix-icon="Search" />
        
        <ElDatePicker v-model="filters.startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" />
        
        <ElDatePicker v-model="filters.endDate" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" />
      </div>
      
      <div class="flex items-center gap-2">
        <ElButton type="primary" @click="handleSearch">
          <SearchIcon class="w-4 h-4" />
          搜索
        </ElButton>
        <ElButton @click="handleReset">重置</ElButton>
      </div>
    </div>
    
    <div class="bg-white rounded-xl shadow-sm p-6">
      <ElTable :data="logs" border stripe>
        <ElTableColumn prop="id" label="ID" width="80" />
        <ElTableColumn prop="operationType" label="操作类型" width="100">
          <template #default="{ row }">{{ getOperationTypeLabel(row.operationType) }}</template>
        </ElTableColumn>
        <ElTableColumn prop="targetType" label="目标类型" width="100">
          <template #default="{ row }">{{ getTargetTypeLabel(row.targetType) }}</template>
        </ElTableColumn>
        <ElTableColumn prop="targetId" label="目标ID" width="80" />
        <ElTableColumn prop="operator" label="操作人" width="100" />
        <ElTableColumn prop="detail" label="操作详情" show-overflow-tooltip />
        <ElTableColumn prop="ipAddress" label="IP地址" width="120" />
        <ElTableColumn prop="createdAt" label="操作时间" width="160" />
      </ElTable>
      
      <div class="mt-4 flex items-center justify-end">
        <ElPagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchLogs"
          @current-change="fetchLogs"
        />
      </div>
    </div>
  </div>
</template>