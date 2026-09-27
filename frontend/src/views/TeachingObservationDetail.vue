<template>
  <div class="space-y-5" v-loading="loading">
    <div class="flex items-start justify-between gap-4">
      <div>
        <el-button @click="router.push('/teaching-observations')">返回列表</el-button>
        <h1 class="text-2xl font-bold mt-2">{{ detail?.courseName }} / {{ detail?.sessionNo }}</h1>
        <div class="text-gray-500 mt-1">
          讲师：{{ detail?.instructorName }}
          <span v-if="detail?.observerName">｜观察人：{{ detail.observerName }}</span>
         ｜发生时间：{{ formatDate(detail?.occurredAt) }}
        </div>
      </div>
      <el-tag v-if="detail" :type="statusType(detail.status)" size="large">{{ detail.statusText }}</el-tag>
    </div>

    <template v-if="detail">
      <el-alert title="服务端分级视图" :type="detail.viewLevel === 'FULL' ? 'success' : detail.viewLevel === 'INSTRUCTOR' ? 'warning' : 'info'"
        :closable="false" :description="viewDescription" class="mb-4" />

      <el-alert v-for="r in visibleReminders" :key="r.id" class="mb-2" type="warning" show-icon :closable="false"
        :title="r.title" :description="r.message" />

      <!-- 普通查看者：仅状态与最终结论 -->
      <el-card v-if="detail.viewLevel === 'PUBLIC'" shadow="never">
        <template #header><span class="font-semibold">周期状态与最终结论</span></template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="当前状态">{{ detail.statusText }}</el-descriptions-item>
          <el-descriptions-item label="最终结论">{{ detail.currentConclusion || '周期进行中，暂无最终结论' }}</el-descriptions-item>
        </el-descriptions>
        <el-timeline class="mt-4">
          <el-timeline-item v-for="e in detail.timeline" :key="e.id" :timestamp="formatDate(e.createdAt)">
            {{ e.detail }} <span v-if="e.actorName">（{{ e.actorName }}）</span>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <template v-else>
        <!-- 操作区 -->
        <el-card shadow="never">
          <template #header><span class="font-semibold">当前待处理：{{ detail.statusText }}</span></template>
          <div class="flex flex-wrap gap-2">
            <el-button @click="load">刷新当前版本</el-button>
            <el-button v-if="isSupervisor" @click="openAddendum">追加说明</el-button>
            <template v-if="isInstructor && detail.instructorId === user.instructorId">
              <el-button v-if="canAck" type="primary" @click="ack">确认已阅</el-button>
              <el-button v-if="canRespond" type="primary" @click="openRespond">回应</el-button>
              <el-button v-if="canAck" type="warning" @click="openDispute">提出事实异议</el-button>
              <el-button v-if="canAck" type="danger" plain @click="openRefuse">拒绝确认</el-button>
              <el-button v-if="canEvidence" type="success" @click="openEvidence">回报进展/证明</el-button>
            </template>
            <el-button v-if="isSupervisor && canReviewDispute" type="warning" @click="openDisputeReview">隔离复核异议</el-button>
            <el-button v-if="(isSupervisor || isInstructor) && detail.status === 'GOAL_SETTING'" type="primary" @click="openGoals">
              拟定/追加阶段目标
            </el-button>
            <el-button v-if="(isSupervisor || isInstructor) && detail.status === 'GOAL_SETTING' && detail.goals.length"
              type="success" @click="confirmGoals">确认阶段目标
            </el-button>
            <el-button v-if="isSupervisor && detail.status === 'FOLLOWUP_REVIEW'" type="primary" @click="openEvidenceReview">
              复查证明
            </el-button>
            <el-button v-if="isSupervisor && canEscalateRequest" type="warning" @click="openEscalationRequest">申请升级</el-button>
            <template v-if="isOwner">
              <el-button v-if="detail.status === 'AWAIT_COURSE_OWNER'" type="success" @click="openClose">决定结案</el-button>
              <el-button v-if="canExtendOrEscalate" type="warning" @click="openExtend">延长周期</el-button>
              <el-button v-if="canExtendOrEscalate" type="danger" @click="openEscalate">升级强化辅导</el-button>
              <el-button v-if="detail.status === 'REOPEN_PENDING'" @click="approveReopen">批准重开</el-button>
              <el-button v-if="detail.status === 'REOPEN_PENDING'" type="warning" @click="declineReopen">驳回重开</el-button>
              <el-button v-if="detail.status === 'REOPEN_REVIEW'" type="primary" @click="openNewConclusion">登记新结论</el-button>
            </template>
            <el-button v-if="canReopen" @click="openReopen">申请重开（证据错误）</el-button>
          </div>
        </el-card>

        <!-- 当前有效观察项 -->
        <el-card shadow="never">
          <template #header>
            <div class="flex justify-between"><span class="font-semibold">当前有效观察项（改进目标仅依据 ACTIVE 项）</span>
              <el-tag size="small">版本 {{ detail.versionLock }}</el-tag>
            </div>
          </template>
          <el-table :data="detail.items" border size="small">
            <el-table-column label="维度" width="130">
              <template #default="{ row }">{{ dimensions[row.dimension] || row.dimension }}</template>
            </el-table-column>
            <el-table-column label="等级" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="gradeType(row.grade)">{{ grades[row.grade] || row.grade }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="可见级别" width="150">
              <template #default="{ row }">
                <el-tooltip :content="sensitivityHint(row.sensitivityLevel)"><span>{{ sensitivityText(row.sensitivityLevel) }}</span></el-tooltip>
                <el-tag v-if="row.masked" size="small" class="ml-1">已脱敏</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fact" label="事实" min-width="240" show-overflow-tooltip />
            <el-table-column prop="advice" label="改进建议" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.effectiveStatus === 'ACTIVE' ? 'success' : 'info'">
                  {{ row.effectiveStatus === 'ACTIVE' ? '有效' : '已撤销' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="revisionNo" label="版本" width="70" />
          </el-table>
          <el-divider />
          <h3 class="font-semibold mb-2">事后补充（仅追加）</h3>
          <el-timeline>
            <el-timeline-item v-for="a in detail.addenda" :key="a.id" :timestamp="formatDate(a.createdAt)">
              <el-tag size="small" class="mr-2">{{ sensitivityText(a.sensitivityLevel) }}</el-tag>
              {{ a.content }} <span class="text-gray-400">— {{ a.authorName }}</span>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 目标与证据 -->
        <el-card shadow="never">
          <template #header><span class="font-semibold">分阶段目标与证据回报</span></template>
          <el-empty v-if="!detail.goals.length" description="尚未形成改进目标" />
          <div v-for="g in detail.goals" :key="g.id" class="border rounded p-3 mb-3">
            <div class="flex justify-between items-center">
              <div><b>阶段 {{ g.stageNo }}：{{ g.title }}</b>
                <el-tag size="small" class="ml-2">{{ goalStatus(g.status) }}</el-tag>
                <el-tag size="small" type="info" class="ml-1">{{ g.required ? '必需' : '可选' }}</el-tag>
              </div>
              <div class="text-sm text-gray-500">截止：{{ g.dueDate }}</div>
            </div>
            <p class="text-sm mt-1">衡量标准：{{ g.measurementCriteria }}</p>
            <el-table :data="g.evidence" size="small" border class="mt-2">
              <el-table-column prop="content" label="进展/证明" min-width="240" show-overflow-tooltip />
              <el-table-column prop="materialUri" label="附件" min-width="160" show-overflow-tooltip />
              <el-table-column label="状态" width="120">
                <template #default="{ row }"><el-tag size="small">{{ evidenceStatus(row.status) }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="reviewComment" label="复查意见" min-width="180" show-overflow-tooltip />
              <el-table-column label="时间" width="160">
                <template #default="{ row }">{{ formatDate(row.submittedAt) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>

        <!-- 异议复核 -->
        <el-card shadow="never">
          <template #header><span class="font-semibold">讲师异议与另一名督导复核</span></template>
          <el-empty v-if="!detail.disputes.length" description="暂无异议" />
          <el-timeline v-else>
            <el-timeline-item v-for="d in detail.disputes" :key="d.id" :timestamp="formatDate(d.raisedAt)">
              <b>{{ d.raisedByName }}</b>：{{ d.reason }}
              <el-tag size="small" class="ml-2">{{ disputeStatus(d.status) }}</el-tag>
              <div v-if="d.reviewedAt" class="text-sm text-gray-600 mt-1">
                复核人：{{ d.reviewerName }}｜{{ d.reviewComment }}｜{{ formatDate(d.reviewedAt) }}
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 原始版本/历史版本 -->
        <el-card shadow="never">
          <template #header><span class="font-semibold">观察项原文、原等级与处理意见追溯</span></template>
          <el-table :data="detail.revisions" border size="small">
            <el-table-column prop="revisionNo" label="版本" width="70" />
            <el-table-column label="维度" width="120">
              <template #default="{ row }">{{ dimensions[row.dimension] || row.dimension }}</template>
            </el-table-column>
            <el-table-column prop="grade" label="原/当时等级" width="120" />
            <el-table-column prop="fact" label="原/当时事实" min-width="240" show-overflow-tooltip />
            <el-table-column prop="changeType" label="处理类型" width="150" />
            <el-table-column prop="changedBy" label="处理人" width="100" />
            <el-table-column prop="changeReason" label="处理意见" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-card>

        <!-- 周期结论版本 -->
        <el-card shadow="never">
          <template #header><span class="font-semibold">周期结论版本链（重开不覆盖旧结论）</span></template>
          <el-timeline>
            <el-timeline-item v-for="d in detail.decisions" :key="d.id" :timestamp="formatDate(d.createdAt)">
              <el-tag size="small" :type="d.status === 'ACTIVE' ? 'success' : 'info'" class="mr-2">v{{ d.versionNo }} {{ decisionType(d.decisionType) }}</el-tag>
              <b>{{ d.conclusion }}</b>
              <div class="text-sm text-gray-600">依据：{{ d.basis }}</div>
              <div v-if="d.replacesDecisionId" class="text-xs text-orange-600">替代历史结论 #{{ d.replacesDecisionId }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </template>

      <!-- 完整时间线 -->
      <el-card shadow="never">
        <template #header><span class="font-semibold">完整时间线</span></template>
        <el-timeline>
          <el-timeline-item v-for="e in detail.timeline" :key="e.id" :timestamp="formatDate(e.createdAt)">
            {{ e.detail }} <span v-if="e.actorName" class="text-gray-400">— {{ e.actorName }}</span>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { observationApi, type TeachingObservation } from '../api'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const user = JSON.parse(localStorage.getItem('user') || '{}')
const roles: string[] = user.roles || []
const isSupervisor = roles.includes('SUPERVISOR')
const isInstructor = roles.includes('INSTRUCTOR')
const isOwner = roles.includes('COURSE_OWNER')

const detail = ref<TeachingObservation>()
const loading = ref(false)

const dimensions: Record<string, string> = { ORGANIZATION: '课堂组织', ACCURACY: '内容准确性', INTERACTION: '互动引导', TIME_CONTROL: '时间控制', OTHER: '其他' }
const grades: Record<string, string> = { EXCELLENT: '优秀', QUALIFIED: '合格', NEEDS_IMPROVEMENT: '待改进', SERIOUS_ISSUE: '严重问题' }

const formatDate = (s?: string) => s ? new Date(s).toLocaleString('zh-CN') : '-'
const statusType = (s: string) => s.includes('OVERDUE') || s === 'INSTRUCTOR_REFUSED' ? 'danger' : s.includes('PENDING') || s === 'ESCALATION_REQUESTED' ? 'warning' : s === 'CLOSED' ? 'success' : 'primary'
const gradeType = (g: string) => g === 'SERIOUS_ISSUE' ? 'danger' : g === 'NEEDS_IMPROVEMENT' ? 'warning' : g === 'EXCELLENT' ? 'success' : 'info'
const sensitivityText = (s: string) => ({ PUBLIC: '公开', INSTRUCTOR_RELATED: '讲师相关脱敏', RESTRICTED: '受限隐藏' } as any)[s] || s
const sensitivityHint = (s: string) => s === 'RESTRICTED' ? '学员身份、投诉原文或内部判断不对讲师展示' : s === 'INSTRUCTOR_RELATED' ? '讲师可见但服务端已脱敏' : '非敏感内容'
const goalStatus = (s: string) => ({ DRAFT: '草稿', PROPOSED: '待双方确认', ACTIVE: '待证明', SUBMITTED: '待复查', APPROVED: '已通过', REWORKED: '需补充/退回', SUPERSEDED: '已替代' } as any)[s] || s
const evidenceStatus = (s: string) => ({ SUBMITTED: '待复查', SUPPLEMENT_REQUESTED: '需补充', REJECTED: '已退回', APPROVED: '有效' } as any)[s] || s
const disputeStatus = (s: string) => ({ OPEN: '待复核', UPHELD: '维持', CORRECTED: '已修正', REVOKED: '已撤销' } as any)[s] || s
const decisionType = (s: string) => ({ CLOSE: '结案', EXTEND: '延长', ESCALATE: '强化辅导', REOPEN_APPROVAL: '重开批准', REOPENED_CONCLUSION: '重开新结论' } as any)[s] || s

const canAck = computed(() => detail.value?.status === 'PENDING_ACK' && !detail.value.acknowledgedAt)
const canRespond = computed(() => detail.value && ['PENDING_ACK', 'INSTRUCTOR_OVERDUE'].includes(detail.value.status))
const canEvidence = computed(() => detail.value && ['EVIDENCE_PENDING', 'GOAL_OVERDUE'].includes(detail.value.status) && detail.value.goals.some(g => ['ACTIVE', 'REWORKED'].includes(g.status)))
const canReviewDispute = computed(() => detail.value?.status === 'OBJECTION_PENDING_REVIEW')
const canEscalateRequest = computed(() => detail.value && !['CLOSED', 'INTENSIVE_COACHING', 'REOPEN_PENDING', 'REOPEN_REVIEW', 'ESCALATION_REQUESTED'].includes(detail.value.status))
const canExtendOrEscalate = computed(() => detail.value && ['AWAIT_COURSE_OWNER', 'EVIDENCE_PENDING', 'GOAL_OVERDUE', 'FOLLOWUP_REVIEW', 'SUPERVISOR_REVIEW_OVERDUE', 'ESCALATION_REQUESTED'].includes(detail.value.status))
const canReopen = computed(() => detail.value?.status === 'CLOSED' && (isSupervisor || isOwner || (isInstructor && detail.value.instructorId === user.instructorId)))
const visibleReminders = computed(() => detail.value?.reminders || [])
const viewDescription = computed(() => detail.value?.viewLevel === 'FULL' ? '当前账号可见完整事实、受限内容、版本与处理意见。' : '当前账号仅可见与本人改进相关内容，受限信息已由服务端脱敏。')

async function load() {
  loading.value = true
  try {
    detail.value = (await observationApi.get(id)).data.data
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载详情失败')
  } finally {
    loading.value = false
  }
}

async function call(action: () => Promise<any>, success: string) {
  try {
    await action()
    ElMessage.success(success)
    await load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function ack() {
  await call(() => observationApi.acknowledge(id, detail.value!.versionLock), '已确认')
}
async function openRespond() {
  const responseText = await ElMessageBox.prompt('请输入回应内容', '讲师回应', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (responseText) await call(() => observationApi.respond(id, responseText, detail.value!.versionLock), '回应已提交')
}
async function openRefuse() {
  const reason = await ElMessageBox.prompt('请说明拒绝确认原因', '拒绝确认', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (reason) await call(() => observationApi.refuse(id, reason, detail.value!.versionLock), '已记录拒绝确认')
}
async function openDispute() {
  const reason = await ElMessageBox.prompt('请说明哪项事实有误（确认前可异议）', '提出异议', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (reason) await call(() => observationApi.dispute(id, { reason, expectedVersion: detail.value!.versionLock }), '异议已提交')
}
async function openAddendum() {
  const { value } = await ElMessageBox.prompt('请输入追加说明（不会修改原始记录）', '追加说明', { inputType: 'textarea' }).catch(() => ({ value: null }))
  if (value) await call(() => observationApi.addendum(id, { sensitivityLevel: 'INSTRUCTOR_RELATED', content: value }), '补充说明已追加')
}
async function openDisputeReview() {
  const open = detail.value?.disputes.find(d => d.status === 'OPEN')
  if (!open) return ElMessage.warning('没有待复核异议')
  const decision = await ElMessageBox({
    title: '异议复核（原观察人不可处理）',
    message: '请输入 UPHELD 维持 / CORRECTED 修正 / REVOKED 撤销，并用换行填写复核意见',
    showCancelButton: true,
    inputType: 'textarea'
  }).then(r => r.value).catch(() => null)
  if (!decision) return
  const [d, reviewComment] = decision.split('\n')
  await call(() => observationApi.reviewDispute(id, {
    disputeId: open.id, decision: d.trim(), reviewComment: reviewComment || d,
    expectedVersion: detail.value!.versionLock
  }), '复核完成')
}
async function openGoals() {
  const stageNo = detail.value!.goals.length + 1
  const title = await ElMessageBox.prompt('阶段目标标题', `阶段 ${stageNo}`).then(r => r.value).catch(() => null)
  if (!title) return
  const measurementCriteria = await ElMessageBox.prompt('衡量标准', '目标').then(r => r.value).catch(() => null)
  if (!measurementCriteria) return
  const dueDate = await ElMessageBox.prompt('截止日期 YYYY-MM-DD', '目标').then(r => r.value).catch(() => null)
  if (!dueDate) return
  await call(() => observationApi.goals(id, { goals: [{ stageNo, title, measurementCriteria, dueDate, required: true, activate: false }], expectedVersion: detail.value!.versionLock }), '目标已暂存，需双方确认')
}
async function confirmGoals() {
  await call(() => observationApi.confirmGoals(id, detail.value!.versionLock), '目标确认已记录')
}
async function openEvidence() {
  const goal = detail.value!.goals.find(g => ['ACTIVE', 'REWORKED'].includes(g.status))
  const content = await ElMessageBox.prompt(`目标：${goal?.title}\n请填写进展`, '回报进展', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (!content) return
  const materialUri = await ElMessageBox.prompt('证明材料 URI（可空）', '证明材料').then(r => r.value).catch(() => '')
  await call(() => observationApi.evidence(goal!.id, {
    content, materialUri, clientToken: crypto.randomUUID(), expectedVersion: detail.value!.versionLock
  }), '证据已提交')
}
async function openEvidenceReview() {
  const ev = detail.value!.goals.flatMap(g => g.evidence).find(e => e.status === 'SUBMITTED')
  if (!ev) return ElMessage.warning('没有待复查证明')
  const value = await ElMessageBox.prompt('APPROVE 通过 / SUPPLEMENT 补充 / REJECT 退回，换行填写意见', '复查证明', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (!value) return
  const [decision, comment] = value.split('\n')
  await call(() => observationApi.reviewEvidence(id, { evidenceId: ev.id, decision: decision.trim(), comment: comment || decision, expectedVersion: detail.value!.versionLock }), '复查完成')
}
async function basisPrompt(title: string) {
  return await ElMessageBox.prompt('必须写明依据', title, { inputType: 'textarea' }).then(r => ({ basis: r.value, clientToken: crypto.randomUUID(), expectedVersion: detail.value!.versionLock })).catch(() => null)
}
async function openClose() {
  const data = await basisPrompt('决定结案')
  if (data) await call(() => observationApi.close(id, { ...data, conclusion: '必需目标证据有效，准予结案。' }), '已结案')
}
async function openExtend() {
  const data = await basisPrompt('延长周期')
  if (data) await call(() => observationApi.extend(id, { ...data, conclusion: '基于持续风险延长周期。' }), '已延长')
}
async function openEscalationRequest() {
  const data = await basisPrompt('申请升级')
  if (data) await call(() => observationApi.requestEscalation(id, data), '升级申请已提交')
}
async function openEscalate() {
  const data = await basisPrompt('升级为强化辅导')
  if (data) await call(() => observationApi.escalate(id, { ...data, conclusion: '升级为强化辅导。' }), '已升级')
}
async function openReopen() {
  const reason = await ElMessageBox.prompt('重开原因', '申请重开', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (!reason) return
  const evidenceError = await ElMessageBox.prompt('发现的证据错误', '申请重开', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (evidenceError) await call(() => observationApi.reopen(id, { reason, evidenceError, clientToken: crypto.randomUUID(), expectedVersion: detail.value!.versionLock }), '重开申请已提交')
}
async function approveReopen() {
  const comment = await ElMessageBox.prompt('批准意见', '批准重开', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (comment) await call(() => observationApi.approveReopen(id, { comment, clientToken: crypto.randomUUID(), expectedVersion: detail.value!.versionLock }), '已批准重开')
}
async function declineReopen() {
  const comment = await ElMessageBox.prompt('驳回意见', '驳回重开', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (comment) await call(() => observationApi.declineReopen(id, { comment, expectedVersion: detail.value!.versionLock }), '已驳回重开')
}
async function openNewConclusion() {
  const conclusion = await ElMessageBox.prompt('新结论', '重开新结论', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (!conclusion) return
  const basis = await ElMessageBox.prompt('依据', '重开新结论', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (!basis) return
  const replacementNote = await ElMessageBox.prompt('说明与旧结论的替代关系', '重开新结论', { inputType: 'textarea' }).then(r => r.value).catch(() => null)
  if (replacementNote) await call(() => observationApi.reopenedConclusion(id, { conclusion, basis, replacementNote, clientToken: crypto.randomUUID(), expectedVersion: detail.value!.versionLock }), '新结论已登记')
}

onMounted(load)
</script>
