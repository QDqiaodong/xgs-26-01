<template>
  <div class="space-y-5" v-loading="loading">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <el-button @click="router.push('/plans')">返回列表</el-button>
        <h1 class="text-2xl font-bold">{{ plan?.name }}</h1>
        <el-tag v-if="plan" :type="statusTagType(plan.status)" size="large">
          {{ statusLabels[plan.status] }}
        </el-tag>
        <el-tag v-if="plan?.currentActivePublished" type="success" size="large" effect="dark">
          当前唯一有效发布版本
        </el-tag>
      </div>
      <div class="flex items-center gap-2" v-if="plan">
        <el-button @click="loadAll">刷新</el-button>
        <el-button v-if="canEditCandidates" @click="showCandidateEditor = true">调整候选</el-button>
        <el-button v-if="canDiscard" type="warning" plain @click="handleDiscard">废弃</el-button>
        <el-button v-if="canSubmit" type="warning" @click="handleSubmit">提交评审</el-button>
        <el-button v-if="canInstructorReview" type="primary" @click="openReview('INSTRUCTOR')">
          讲师管理员评审
        </el-button>
        <el-button v-if="canCourseReview" type="primary" @click="openReview('COURSE')">
          课程管理员评审
        </el-button>
        <el-button v-if="canAdminReject" type="danger" plain @click="openAdminReject">系统管理员驳回</el-button>
        <el-button v-if="canPublish" type="success" :loading="publishing" @click="handlePublish">
          发布方案
        </el-button>
        <el-button v-if="canAssign" type="success" :loading="assigning" @click="handleAssign">
          正式指派第1名
        </el-button>
      </div>
    </div>

    <template v-if="plan">
      <!-- 失效横幅 -->
      <el-alert
        v-if="plan.status === 'INVALIDATED'"
        type="error"
        :closable="false"
        show-icon
        title="方案已失效，不能继续评审或发布"
      >
        <template #default>
          <div>变化来源：{{ invalidSourceLabel(plan.invalidSourceType) }}
            <span v-if="plan.invalidSourceId"> #{{ plan.invalidSourceId }}</span>
          </div>
          <div>{{ plan.invalidReason }}</div>
          <div class="mt-1">请基于最新资料重新发起推演并保存为新版本。</div>
        </template>
      </el-alert>

      <!-- 评审进度 -->
      <el-card shadow="never">
        <template #header><span class="font-semibold">双角色评审进度（两类意见都通过才可发布）</span></template>
        <el-steps :active="reviewStep" align-center finish-status="success">
          <el-step title="提交评审" :description="plan.submittedBy ? plan.submittedBy + ' 提交' : '未提交'" />
          <el-step title="讲师管理员核对" :description="reviewDesc('INSTRUCTOR')" />
          <el-step title="课程管理员核对" :description="reviewDesc('COURSE')" />
          <el-step title="发布" :description="plan.publishedBy ? plan.publishedBy + ' 发布' : '未发布'" />
        </el-steps>
      </el-card>

      <!-- 快照信息 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <el-card shadow="never">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-semibold">课程要求快照</span>
              <el-tag size="small" type="info">创建于 {{ formatDate(plan.createdAt) }}</el-tag>
            </div>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="课程名称">{{ plan.courseName }}</el-descriptions-item>
            <el-descriptions-item label="行业 / 难度">{{ plan.courseIndustry }} / {{ plan.courseDifficulty }}</el-descriptions-item>
            <el-descriptions-item label="资质要求">{{ plan.courseRequiredQualifications || '-' }}</el-descriptions-item>
            <el-descriptions-item label="风格要求">{{ plan.courseStyleRequirements || '-' }}</el-descriptions-item>
            <el-descriptions-item label="课程状态(快照)">{{ plan.courseStatus }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-semibold">采用权重配置快照</span>
              <span class="text-sm text-gray-500">{{ plan.weightConfigName || '#' + plan.weightConfigId }}</span>
            </div>
          </template>
          <div class="grid grid-cols-5 gap-2 text-center mb-3">
            <div class="bg-gray-50 rounded p-2">
              <div class="text-lg font-bold text-primary">{{ pct(plan.teachingYearsWeight) }}</div>
              <div class="text-xs text-gray-500">授课年限</div>
            </div>
            <div class="bg-gray-50 rounded p-2">
              <div class="text-lg font-bold text-green-600">{{ pct(plan.qualificationWeight) }}</div>
              <div class="text-xs text-gray-500">专业资质</div>
            </div>
            <div class="bg-gray-50 rounded p-2">
              <div class="text-lg font-bold text-yellow-600">{{ pct(plan.ratingWeight) }}</div>
              <div class="text-xs text-gray-500">学员口碑</div>
            </div>
            <div class="bg-gray-50 rounded p-2">
              <div class="text-lg font-bold text-red-500">{{ pct(plan.expertiseWeight) }}</div>
              <div class="text-xs text-gray-500">行业擅长</div>
            </div>
            <div class="bg-gray-50 rounded p-2">
              <div class="text-lg font-bold text-blue-500">{{ pct(plan.styleWeight) }}</div>
              <div class="text-xs text-gray-500">授课风格</div>
            </div>
          </div>
          <div class="text-xs text-gray-500">缺口阈值：{{ plan.gapThreshold ?? '-' }}　最佳匹配：{{ plan.bestScore }}</div>
        </el-card>
      </div>

      <!-- 快照 vs 当前 差异 -->
      <el-card shadow="never">
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-semibold">快照与当前资料差异（仅展示，方案评分与排序不会重算）</span>
            <el-button size="small" @click="loadDiff" :loading="diffLoading">重新核对</el-button>
          </div>
        </template>
        <el-alert
          v-if="diff && diff.hasAnyChange"
          :type="diff.assignmentBlocked ? 'error' : 'warning'"
          :closable="false"
          show-icon
          class="mb-3"
          :title="diff.assignmentBlocked
            ? '入选讲师当前不可指派：' + diff.assignmentBlockReason
            : '草稿创建后相关资料已发生变化（未发布方案应已被标记失效；已发布版本指派前仍会复核容量与可用状态）'"
        />
        <el-alert v-else-if="diff && !diff.hasAnyChange" type="success" :closable="false" show-icon
          title="快照与当前资料一致" class="mb-3" />
        <el-table v-if="diff" :data="changedDiffs" border size="small" empty-text="无差异">
          <el-table-column label="对象" min-width="160">
            <template #default="{ row }">
              <el-tag size="small" :type="diffTargetTag(row.targetType)">{{ diffTargetLabel(row.targetType) }}</el-tag>
              <span class="ml-2">{{ row.targetName || ('#' + row.targetId) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="fieldLabel" label="变化项" width="130" />
          <el-table-column label="方案快照" min-width="180">
            <template #default="{ row }"><span class="text-gray-500">{{ row.snapshotValue || '-' }}</span></template>
          </el-table-column>
          <el-table-column label="当前资料" min-width="180">
            <template #default="{ row }"><span class="text-red-600 font-medium">{{ row.currentValue || '-' }}</span></template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 候选排序与五项评分明细 -->
      <el-card shadow="never">
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-semibold">候选排序与五项评分明细（快照固化，提交后锁定）</span>
            <span class="text-sm text-gray-500">正式指派只采用第 1 名，不自动改派</span>
          </div>
        </template>
        <el-table :data="plan.candidates" border size="small">
          <el-table-column label="排名" width="70">
            <template #default="{ row }">
              <span v-if="row.rankNo === 1" class="text-yellow-500 font-bold">🥇 1</span>
              <span v-else-if="row.rankNo === 2" class="text-gray-400 font-bold">🥈 2</span>
              <span v-else-if="row.rankNo === 3" class="text-amber-600 font-bold">🥉 3</span>
              <span v-else>{{ row.rankNo }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="instructorName" label="讲师" width="100" />
          <el-table-column label="总分" width="80">
            <template #default="{ row }">
              <span class="font-semibold" :class="row.totalScore >= 60 ? 'text-green-600' : 'text-red-600'">
                {{ row.totalScore }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="teachingYearsScore" label="年限" width="70" />
          <el-table-column prop="qualificationScore" label="资质" width="70" />
          <el-table-column prop="ratingScore" label="口碑" width="70" />
          <el-table-column prop="expertiseScore" label="行业" width="70" />
          <el-table-column prop="styleScore" label="风格" width="70" />
          <el-table-column label="快照资料" min-width="220">
            <template #default="{ row }">
              <div class="text-xs text-gray-500">
                {{ row.instructorStatus }} / {{ row.instructorAvailable ? '可授课' : '不可授课' }}
                ｜年限 {{ row.teachingYears }}｜评分 {{ row.studentRating }}
                ｜容量上限 {{ row.maxCoursesPerMonth }}
              </div>
              <div class="text-xs text-gray-400">资质：{{ (row.skills || []).join('、') || row.certificates || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="当前状态(指派复核)" min-width="160">
            <template #default="{ row }">
              <el-tag v-if="row.currentUsableForAssignment" type="success" size="small">当前可指派</el-tag>
              <el-tooltip v-else :content="row.currentUsableReason || ''" placement="top">
                <el-tag type="danger" size="small">{{ row.currentUsableReason || '当前不可指派' }}</el-tag>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 评审意见 -->
      <el-card shadow="never">
        <template #header><span class="font-semibold">评审意见（查看者只读）</span></template>
        <el-table :data="plan.reviews" border size="small" empty-text="暂无评审意见">
          <el-table-column label="评审角色" width="200">
            <template #default="{ row }">
              {{ row.reviewType === 'INSTRUCTOR' ? '讲师管理员（资质/可授课状态）' : '课程管理员（课程要求/权重）' }}
            </template>
          </el-table-column>
          <el-table-column label="结论" width="100">
            <template #default="{ row }">
              <el-tag :type="row.decision === 'APPROVED' ? 'success' : 'danger'" size="small">
                {{ row.decision === 'APPROVED' ? '通过' : '驳回' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="comment" label="意见" min-width="200" show-overflow-tooltip />
          <el-table-column label="评审人" width="140">
            <template #default="{ row }">{{ row.reviewerName || row.reviewerUsername }}</template>
          </el-table-column>
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 轨迹 -->
      <el-card shadow="never">
        <template #header><span class="font-semibold">方案轨迹（审计事实，不可修改）</span></template>
        <el-timeline>
          <el-timeline-item
            v-for="e in [...plan.events].reverse()"
            :key="e.id"
            :type="eventColor(e.eventType)"
            :timestamp="formatDate(e.createdAt)"
          >
            <div class="text-sm">
              <el-tag size="small" :type="eventColor(e.eventType)" class="mr-2">{{ eventLabel(e.eventType) }}</el-tag>
              <span class="text-gray-600">{{ e.actorName || e.actorUsername }}</span>
            </div>
            <div class="text-xs text-gray-500 mt-1">{{ e.detail }}</div>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </template>

    <!-- 评审弹窗 -->
    <el-dialog v-model="reviewDialog" :title="reviewType === 'INSTRUCTOR' ? '讲师管理员评审：资质与可授课状态' : '课程管理员评审：课程要求与权重'" width="520px">
      <el-form label-width="80px">
        <el-form-item label="结论">
          <el-radio-group v-model="reviewForm.decision">
            <el-radio-button label="APPROVED">通过</el-radio-button>
            <el-radio-button label="REJECTED">驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="3"
            :placeholder="reviewType === 'INSTRUCTOR'
              ? '请核对入选讲师资质证书与当前可授课状态'
              : '请核对课程要求与方案采用的权重配置'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialog = false">取消</el-button>
        <el-button type="primary" :loading="submittingReview" @click="submitReview">提交意见</el-button>
      </template>
    </el-dialog>

    <!-- 系统管理员驳回弹窗 -->
    <el-dialog v-model="adminRejectDialog" title="系统管理员驳回（不替代专业评审）" width="520px">
      <el-input v-model="adminRejectReason" type="textarea" :rows="3" placeholder="请填写驳回原因" />
      <template #footer>
        <el-button @click="adminRejectDialog = false">取消</el-button>
        <el-button type="danger" :loading="submittingReview" @click="submitAdminReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 调整候选弹窗 -->
    <el-dialog v-model="showCandidateEditor" title="调整草稿候选（按来源推演得分排序固化）" width="640px">
      <el-table :data="candidateEditorRows" border max-height="420">
        <el-table-column width="55">
          <template #default="{ row }">
            <el-checkbox v-model="candidateSelected[row.instructorId]" />
          </template>
        </el-table-column>
        <el-table-column prop="rankNo" label="原排名" width="80" />
        <el-table-column prop="instructorName" label="讲师" width="100" />
        <el-table-column prop="totalScore" label="得分" width="80" />
        <el-table-column label="快照状态">
          <template #default="{ row }">
            {{ row.instructorStatus }} / {{ row.instructorAvailable ? '可授课' : '不可授课' }}
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showCandidateEditor = false">取消</el-button>
        <el-button type="primary" :loading="savingCandidates" @click="saveCandidates">保存候选</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  planApi,
  type Plan,
  type PlanSnapshotDiff,
  type FieldDiff,
  type PlanCandidate
} from '../api'

const route = useRoute()
const router = useRouter()
const planId = Number(route.params.id)

const plan = ref<Plan | null>(null)
const diff = ref<PlanSnapshotDiff | null>(null)
const loading = ref(false)
const diffLoading = ref(false)
const publishing = ref(false)
const assigning = ref(false)

const user = JSON.parse(localStorage.getItem('user') || '{}')
const roles: string[] = user.roles || []
const hasRole = (r: string) => roles.includes(r)
const isCourseAdmin = hasRole('COURSE_ADMIN')
const isInstructorAdmin = hasRole('INSTRUCTOR_ADMIN')
const isAdmin = hasRole('ADMIN')

const statusLabels: Record<string, string> = {
  DRAFT: '草稿',
  IN_REVIEW: '评审中',
  APPROVED: '评审通过',
  REJECTED: '已驳回',
  PUBLISHED: '已发布',
  SUPERSEDED: '历史版本',
  INVALIDATED: '已失效'
}

const statusTagType = (s: string): 'info' | 'warning' | 'primary' | 'danger' | 'success' => {
  return ({
    DRAFT: 'info',
    IN_REVIEW: 'warning',
    APPROVED: 'primary',
    REJECTED: 'danger',
    PUBLISHED: 'success',
    SUPERSEDED: 'info',
    INVALIDATED: 'danger'
  } as const)[s] || 'info'
}

const pct = (v?: number) => (v == null ? '-' : Math.round(v * 100) + '%')
const formatDate = (s?: string) => (s ? new Date(s).toLocaleString('zh-CN') : '-')

// ---------------- 权限 + 状态驱动按钮（服务端同样强制，前端仅体验控制） ----------------
const canEditCandidates = computed(() =>
  isCourseAdmin && plan.value && ['DRAFT', 'REJECTED'].includes(plan.value.status))
const canDiscard = computed(() =>
  isCourseAdmin && plan.value &&
  ['DRAFT', 'REJECTED', 'IN_REVIEW', 'APPROVED', 'INVALIDATED'].includes(plan.value.status))
const canSubmit = computed(() =>
  isCourseAdmin && plan.value && ['DRAFT', 'REJECTED'].includes(plan.value.status))
const canInstructorReview = computed(() =>
  isInstructorAdmin && plan.value?.status === 'IN_REVIEW' &&
  !plan.value.reviews.some(r => r.reviewType === 'INSTRUCTOR'))
const canCourseReview = computed(() =>
  isCourseAdmin && plan.value?.status === 'IN_REVIEW' &&
  !plan.value.reviews.some(r => r.reviewType === 'COURSE'))
const canAdminReject = computed(() =>
  isAdmin && plan.value && ['IN_REVIEW', 'APPROVED', 'DRAFT', 'REJECTED'].includes(plan.value.status))
const canPublish = computed(() => isCourseAdmin && plan.value?.status === 'APPROVED')
const canAssign = computed(() =>
  isCourseAdmin && plan.value?.currentActivePublished && !plan.value?.assignmentId)

const reviewStep = computed(() => {
  if (!plan.value) return 0
  if (plan.value.status === 'PUBLISHED' || plan.value.status === 'SUPERSEDED') {
    return plan.value.courseReviewPassed && plan.value.instructorReviewPassed ? 4 : 3
  }
  let step = plan.value.submittedAt ? 1 : 0
  if (plan.value.instructorReviewPassed) step = 2
  if (plan.value.instructorReviewPassed && plan.value.courseReviewPassed) step = 3
  return step
})

const reviewDesc = (type: 'INSTRUCTOR' | 'COURSE') => {
  const r = plan.value?.reviews.find(x => x.reviewType === type)
  if (!r) return '待评审'
  return (r.decision === 'APPROVED' ? '✓ 已通过' : '✗ 已驳回') + '（' + (r.reviewerName || r.reviewerUsername) + '）'
}

const changedDiffs = computed<FieldDiff[]>(() => {
  if (!diff.value) return []
  return [
    ...diff.value.courseDiffs,
    ...diff.value.weightDiffs,
    ...diff.value.instructorDiffs
  ].filter(d => d.changed)
})

const diffTargetLabel = (t: string) =>
  ({ COURSE: '课程', INSTRUCTOR: '讲师', WEIGHT: '权重' } as Record<string, string>)[t] || t
const diffTargetTag = (t: string): 'primary' | 'success' | 'warning' =>
  ({ COURSE: 'primary', INSTRUCTOR: 'success', WEIGHT: 'warning' } as const)[t] || 'primary'

const invalidSourceLabel = (t?: string) =>
  ({
    COURSE: '课程要求变化',
    INSTRUCTOR: '讲师资料/资质变化',
    SKILL: '资质技能变化',
    WEIGHT: '权重配置变化',
    CAPACITY: '讲师月度容量变化',
    DATA: '发布/提交前复核发现资料不一致'
  } as Record<string, string>)[t || ''] || t || '-'

const eventLabel = (t: string) =>
  ({
    CREATED: '创建草稿',
    CANDIDATES_UPDATED: '调整候选',
    SUBMITTED: '提交评审',
    REVIEWED: '评审',
    REJECTED: '驳回',
    INVALIDATED: '资料变更失效',
    PUBLISHED: '发布',
    SUPERSEDED: '被新版本替代',
    ASSIGNMENT_SUCCEEDED: '正式指派成功',
    ASSIGNMENT_FAILED: '正式指派失败',
    DISCARDED: '废弃'
  } as Record<string, string>)[t] || t

const eventColor = (t: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' => {
  if (['PUBLISHED', 'ASSIGNMENT_SUCCEEDED'].includes(t)) return 'success'
  if (['REJECTED', 'INVALIDATED', 'ASSIGNMENT_FAILED'].includes(t)) return 'danger'
  if (['REVIEWED', 'SUBMITTED', 'CANDIDATES_UPDATED'].includes(t)) return 'primary'
  if (t === 'SUPERSEDED') return 'warning'
  return 'info'
}

// ---------------- 操作 ----------------
const loadPlan = async () => {
  loading.value = true
  try {
    plan.value = (await planApi.getById(planId)).data
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载方案失败')
  } finally {
    loading.value = false
  }
}

const loadDiff = async () => {
  diffLoading.value = true
  try {
    diff.value = (await planApi.getDiff(planId)).data
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载差异失败')
  } finally {
    diffLoading.value = false
  }
}

const loadAll = async () => {
  await loadPlan()
  await loadDiff()
}

const handleSubmit = async () => {
  try {
    await ElMessageBox.confirm('提交后候选讲师与排序将锁定，需两类评审均通过才能发布，确认提交？', '提交评审', {
      type: 'warning'
    })
    plan.value = (await planApi.submit(planId)).data
    ElMessage.success('已提交评审')
    loadDiff()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '提交失败')
  }
}

const handleDiscard = async () => {
  try {
    await ElMessageBox.confirm('废弃后方案与评审轨迹将删除（已发布版本不可废弃），确认？', '废弃方案', {
      type: 'warning'
    })
    await planApi.discard(planId)
    ElMessage.success('已废弃')
    router.push('/plans')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '废弃失败')
  }
}

const handlePublish = async () => {
  publishing.value = true
  try {
    plan.value = (await planApi.publish(planId)).data
    ElMessage.success('发布成功：该版本成为当前唯一有效发布版本')
    loadDiff()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '发布失败')
    loadPlan()
  } finally {
    publishing.value = false
  }
}

const handleAssign = async () => {
  assigning.value = true
  try {
    const res = await planApi.assign(planId)
    if (res.data.success) {
      ElMessage.success('正式指派成功：' + res.data.instructorName)
    } else {
      ElMessage.warning('正式指派失败：' + res.data.reason + '（未生成指派，请重新推演形成新版本）')
    }
    await loadPlan()
    await loadDiff()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '正式指派失败')
    await loadPlan()
  } finally {
    assigning.value = false
  }
}

// ---------------- 评审弹窗 ----------------
const reviewDialog = ref(false)
const reviewType = ref<'INSTRUCTOR' | 'COURSE'>('INSTRUCTOR')
const reviewForm = ref<{ decision: 'APPROVED' | 'REJECTED'; comment: string }>({
  decision: 'APPROVED',
  comment: ''
})
const submittingReview = ref(false)

const openReview = (type: 'INSTRUCTOR' | 'COURSE') => {
  reviewType.value = type
  reviewForm.value = { decision: 'APPROVED', comment: '' }
  reviewDialog.value = true
}

const submitReview = async () => {
  submittingReview.value = true
  try {
    const api = reviewType.value === 'INSTRUCTOR' ? planApi.reviewInstructor : planApi.reviewCourse
    plan.value = (await api(planId, reviewForm.value.decision, reviewForm.value.comment)).data
    ElMessage.success('评审意见已提交')
    reviewDialog.value = false
    loadDiff()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '评审提交失败')
    await loadPlan()
  } finally {
    submittingReview.value = false
  }
}

// ---------------- 管理员驳回 ----------------
const adminRejectDialog = ref(false)
const adminRejectReason = ref('')
const openAdminReject = () => {
  adminRejectReason.value = ''
  adminRejectDialog.value = true
}
const submitAdminReject = async () => {
  if (!adminRejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  submittingReview.value = true
  try {
    plan.value = (await planApi.reject(planId, adminRejectReason.value)).data
    ElMessage.success('已驳回')
    adminRejectDialog.value = false
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '驳回失败')
  } finally {
    submittingReview.value = false
  }
}

// ---------------- 候选编辑 ----------------
const showCandidateEditor = ref(false)
const savingCandidates = ref(false)
const candidateEditorRows = ref<PlanCandidate[]>([])
const candidateSelected = ref<Record<number, boolean>>({})

watch(showCandidateEditor, (v) => {
  if (v && plan.value) {
    candidateEditorRows.value = plan.value.candidates
    const selected: Record<number, boolean> = {}
    plan.value.candidates.forEach(c => { selected[c.instructorId] = true })
    candidateSelected.value = selected
  }
})

const saveCandidates = async () => {
  const ids = candidateEditorRows.value
    .filter(r => candidateSelected.value[r.instructorId])
    .sort((a, b) => a.rankNo - b.rankNo)
    .map(r => r.instructorId)
  if (ids.length === 0) {
    ElMessage.warning('至少保留一名候选')
    return
  }
  savingCandidates.value = true
  try {
    plan.value = (await planApi.updateCandidates(planId, ids)).data
    ElMessage.success('候选已更新，排序按来源推演得分固化')
    showCandidateEditor.value = false
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存候选失败')
  } finally {
    savingCandidates.value = false
  }
}

onMounted(loadAll)
</script>
