<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold">推演方案评审发布</h1>
      <div class="flex items-center gap-3">
        <el-select v-model="filterCourse" placeholder="全部课程" clearable class="w-56" @change="loadPlans">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="全部状态" clearable class="w-40" @change="loadPlans">
          <el-option v-for="(label, key) in statusLabels" :key="key" :label="label" :value="key" />
        </el-select>
        <el-button @click="loadPlans">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="plans" border v-loading="loading" empty-text="暂无方案，可在匹配结果页把某次推演保存为方案">
        <el-table-column label="版本" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">v{{ row.versionNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="方案名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabels[row.status] || row.status }}</el-tag>
            <el-tag v-if="row.currentActivePublished" type="success" size="small" effect="dark" class="ml-1">
              当前有效
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评审进度" width="170">
          <template #default="{ row }">
            <div class="flex flex-col gap-1 text-xs">
              <span>
                <el-icon v-if="row.instructorReviewPassed" color="#67C23A"><CircleCheckFilled /></el-icon>
                <el-icon v-else color="#C0C4CC"><RemoveFilled /></el-icon>
                讲师管理员
              </span>
              <span>
                <el-icon v-if="row.courseReviewPassed" color="#67C23A"><CircleCheckFilled /></el-icon>
                <el-icon v-else color="#C0C4CC"><RemoveFilled /></el-icon>
                课程管理员
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最佳匹配" width="100">
          <template #default="{ row }">
            <span :class="row.bestScore >= 60 ? 'text-green-600 font-semibold' : 'text-red-600 font-semibold'">
              {{ row.bestScore }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="候选" prop="candidateCount" width="70" />
        <el-table-column label="正式指派" min-width="150">
          <template #default="{ row }">
            <span v-if="row.assignmentId" class="text-green-700">
              ✓ {{ row.selectedInstructorName }}（#{{ row.assignmentId }}）
            </span>
            <span v-else class="text-gray-400">未指派</span>
          </template>
        </el-table-column>
        <el-table-column label="失效原因" min-width="200">
          <template #default="{ row }">
            <el-tooltip v-if="row.invalidReason" :content="row.invalidReason" placement="top">
              <span class="text-red-600 text-xs cursor-help">{{ row.invalidReason }}</span>
            </el-tooltip>
            <span v-else class="text-gray-300">-</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openDetail(row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, RemoveFilled } from '@element-plus/icons-vue'
import { planApi, courseApi, type Plan, type Course, type PlanStatus } from '../api'

const router = useRouter()
const plans = ref<Plan[]>([])
const courses = ref<Course[]>([])
const loading = ref(false)
const filterCourse = ref<number | undefined>(undefined)
const filterStatus = ref<PlanStatus | ''>('')

const statusLabels: Record<string, string> = {
  DRAFT: '草稿',
  IN_REVIEW: '评审中',
  APPROVED: '评审通过',
  REJECTED: '已驳回',
  PUBLISHED: '已发布',
  SUPERSEDED: '历史版本',
  INVALIDATED: '已失效'
}

const statusTagType = (status: string): 'info' | 'warning' | 'primary' | 'danger' | 'success' => {
  switch (status) {
    case 'DRAFT': return 'info'
    case 'IN_REVIEW': return 'warning'
    case 'APPROVED': return 'primary'
    case 'REJECTED': return 'danger'
    case 'PUBLISHED': return 'success'
    case 'SUPERSEDED': return 'info'
    case 'INVALIDATED': return 'danger'
    default: return 'info'
  }
}

const formatDate = (s?: string) => (s ? new Date(s).toLocaleString('zh-CN') : '-')

const loadPlans = async () => {
  loading.value = true
  try {
    const res = await planApi.list(filterCourse.value || undefined, filterStatus.value || undefined)
    plans.value = res.data
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载方案列表失败')
  } finally {
    loading.value = false
  }
}

const openDetail = (id: number) => router.push(`/plans/${id}`)

onMounted(async () => {
  try {
    courses.value = (await courseApi.getAllList()).data
  } catch {
    // 课程列表加载失败不阻塞方案列表
  }
  loadPlans()
})
</script>
