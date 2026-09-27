<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-3">
      <h1 class="text-2xl font-bold">讲师带教观察与改进周期</h1>
      <div class="flex items-center gap-3 flex-wrap">
        <el-select v-model="filterCourse" placeholder="全部课程" clearable class="w-52" @change="load">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-if="!isInstructor" v-model="filterInstructor" placeholder="全部讲师" clearable
          class="w-44" @change="load">
          <el-option v-for="i in instructors" :key="i.id" :label="i.name" :value="i.id" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="全部状态" clearable class="w-44" @change="load">
          <el-option v-for="(label, key) in statusLabels" :key="key" :label="label" :value="key" />
        </el-select>
        <el-button @click="load">刷新</el-button>
        <el-button v-if="isSupervisor" type="primary" @click="showCreate = true">建立观察</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon class="!mb-2">
      普通查看者仅显示周期状态、改进进度与最终结论；讲师仅可见与本人改进相关的脱敏内容；
      学员身份、投诉原文、内部判断仅督导/课程负责人可见。
    </el-alert>

    <el-card shadow="never">
      <el-table :data="rows" border v-loading="loading"
        empty-text="暂无带教观察记录">
        <el-table-column label="观察" min-width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">{{ row.title }}</el-button>
            <div class="text-xs text-gray-400">{{ row.courseName }} · {{ row.sessionNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="instructorName" label="被观察讲师" width="120" />
        <el-table-column v-if="!isInstructor" prop="observerName" label="观察督导" width="120" />
        <el-table-column label="授课时间" width="170">
          <template #default="{ row }">{{ formatDate(row.observedAt) }}</template>
        </el-table-column>
        <el-table-column label="周期状态" width="170">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.statusLabel }}</el-tag>
            <el-tooltip v-for="t in row.myPendingReminderTypes" :key="t"
              :content="reminderLabels[t] || t" placement="top">
              <el-tag size="small" type="danger" effect="plain" class="ml-1 mt-0.5">待我处理</el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="改进目标进度" width="140">
          <template #default="{ row }">
            <span v-if="row.requiredGoalCount > 0">
              {{ row.achievedRequiredGoalCount }}/{{ row.requiredGoalCount }}
              <el-icon v-if="row.allRequiredGoalsAchieved" color="#67C23A"><CircleCheckFilled /></el-icon>
            </span>
            <span v-else class="text-gray-300">-</span>
          </template>
        </el-table-column>
        <el-table-column label="最终结论" min-width="220">
          <template #default="{ row }">
            <template v-if="row.currentConclusionSummary">
              <el-tag size="small" type="success" effect="dark">v{{ row.conclusionVersionNo }}</el-tag>
              <span class="ml-1 text-sm">{{ row.currentConclusionSummary }}</span>
            </template>
            <span v-else class="text-gray-300 text-sm">周期进行中</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openDetail(row.id)">进入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <ObservationCreateDialog v-if="showCreate" v-model="showCreate" @created="onCreated" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import {
  observationApi,
  courseApi,
  instructorApi,
  type ObservationListItem,
  type Course,
  type Instructor
} from '../api'
import ObservationCreateDialog from '../components/ObservationCreateDialog.vue'

const router = useRouter()
const rows = ref<ObservationListItem[]>([])
const courses = ref<Course[]>([])
const instructors = ref<Instructor[]>([])
const loading = ref(false)
const showCreate = ref(false)

const filterCourse = ref<number | undefined>(undefined)
const filterInstructor = ref<number | undefined>(undefined)
const filterStatus = ref<string>('')

const user = JSON.parse(localStorage.getItem('user') || '{}')
const roles: string[] = user.roles || []
const isSupervisor = roles.includes('SUPERVISOR')
const isInstructor = roles.includes('INSTRUCTOR') && !(isSupervisor)

export const statusLabels: Record<string, string> = {
  PENDING_ACK: '待讲师确认',
  INSTRUCTOR_REJECTED: '讲师拒绝确认',
  DISPUTED: '异议复核中',
  RESPONSE_OVERDUE: '回应逾期',
  NO_IMPROVEMENT_NEEDED: '待负责人结案(无需改进)',
  IMPROVING: '改进中',
  EVIDENCE_PENDING_REVIEW: '证据待复查',
  REVIEW_OVERDUE: '督导复查逾期',
  PENDING_OWNER_CLOSE: '待负责人结案',
  CLOSED: '已结案',
  EXTENDED: '周期已延长',
  ESCALATED: '强化辅导',
  REOPEN_REQUESTED: '重开申请中',
  RE_REVIEW: '重开待复查'
}

const reminderLabels: Record<string, string> = {
  ACK_DUE: '待确认观察',
  RESPONSE_OVERDUE: '回应逾期',
  INSTRUCTOR_REJECTED: '讲师拒绝确认',
  DISPUTE_REVIEW: '异议待复核',
  GOAL_PROGRESS: '待回报进展',
  EVIDENCE_REVIEW: '证据待复查',
  INFO_REQUESTED: '需补充材料',
  EVIDENCE_REJECTED: '证明被退回',
  REVIEW_OVERDUE: '复查逾期',
  OWNER_CLOSE: '待结案',
  REOPEN_APPROVAL: '重开待审批',
  RE_REVIEW: '重开待复查'
}

const statusTagType = (s: string): 'info' | 'warning' | 'primary' | 'danger' | 'success' => {
  if (s === 'CLOSED') return 'success'
  if (['RESPONSE_OVERDUE', 'REVIEW_OVERDUE', 'INSTRUCTOR_REJECTED', 'ESCALATED'].includes(s)) return 'danger'
  if (['PENDING_OWNER_CLOSE', 'NO_IMPROVEMENT_NEEDED', 'PENDING_ACK'].includes(s)) return 'warning'
  if (['DISPUTED', 'EVIDENCE_PENDING_REVIEW', 'REOPEN_REQUESTED', 'RE_REVIEW'].includes(s)) return 'primary'
  return 'info'
}

const formatDate = (s?: string) => (s ? new Date(s).toLocaleString('zh-CN') : '-')

const load = async () => {
  loading.value = true
  try {
    rows.value = (await observationApi.list(
      filterCourse.value || undefined,
      filterInstructor.value || undefined,
      filterStatus.value || undefined
    )).data
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载观察列表失败')
  } finally {
    loading.value = false
  }
}

const openDetail = (id: number) => router.push(`/observations/${id}`)

const onCreated = (id: number) => {
  showCreate.value = false
  load()
  router.push(`/observations/${id}`)
}

onMounted(async () => {
  try {
    courses.value = (await courseApi.getAllList()).data
    if (!isInstructor) {
      instructors.value = (await instructorApi.getAllList()).data
    }
  } catch {
    // 下拉数据失败不阻塞列表
  }
  load()
})
</script>
