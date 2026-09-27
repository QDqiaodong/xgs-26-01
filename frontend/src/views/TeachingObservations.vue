<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold">讲师带教观察与改进周期</h1>
        <p class="text-sm text-gray-500 mt-1">分级可见、异议隔离复核、阶段目标证据链与结案版本全程可追溯</p>
      </div>
      <el-button v-if="isSupervisor" type="primary" @click="openCreate">建立观察</el-button>
    </div>

    <el-card shadow="never">
      <div class="flex flex-wrap gap-3 items-center mb-4">
        <el-select v-model="filter.courseId" placeholder="全部课程" clearable class="w-56">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filter.instructorId" placeholder="全部讲师" clearable class="w-44" filterable>
          <el-option v-for="i in instructors" :key="i.id" :label="i.name" :value="i.id" />
        </el-select>
        <el-input v-model="filter.status" placeholder="状态码筛选" clearable class="w-48" />
        <el-button @click="load">刷新</el-button>
      </div>

      <el-alert v-for="r in reminders" :key="r.id" class="mb-2" type="warning" :closable="false" show-icon
        :title="r.title" :description="r.message" />

      <el-table :data="observations" border v-loading="loading" empty-text="暂无带教观察">
        <el-table-column label="课程场次" min-width="210">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">{{ row.courseName }} / {{ row.sessionNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="instructorName" label="讲师" width="100" />
        <el-table-column label="观察人" width="110">
          <template #default="{ row }">{{ row.observerName || '受限不可见' }}</template>
        </el-table-column>
        <el-table-column label="发生时间" width="170">
          <template #default="{ row }">{{ formatDate(row.occurredAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="170">
          <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.statusText }}</el-tag></template>
        </el-table-column>
        <el-table-column label="最终结论" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.viewLevel === 'PUBLIC'" class="text-gray-600">{{ row.currentConclusion || '周期进行中，暂无最终结论' }}</span>
            <span v-else-if="row.currentConclusion">{{ row.currentConclusion }}</span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="待办" width="80">
          <template #default="{ row }">
            <el-badge v-if="row.pendingReminderCount" :value="row.pendingReminderCount" type="danger" />
            <span v-else class="text-gray-300">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }"><el-button size="small" @click="openDetail(row.id)">详情</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 建立观察 -->
    <el-dialog v-model="createVisible" title="按课程场次建立带教观察" width="820px">
      <el-form :model="createForm" label-width="110px">
        <el-form-item label="课程" required>
          <el-select v-model="createForm.courseId" filterable class="w-full">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="场次" required><el-input v-model="createForm.sessionNo" /></el-form-item>
        <el-form-item label="讲师" required>
          <el-select v-model="createForm.instructorId" filterable class="w-full">
            <el-option v-for="i in instructors" :key="i.id" :label="i.name" :value="i.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发生时间" required>
          <el-date-picker v-model="createForm.occurredAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="w-full" />
        </el-form-item>
        <el-form-item label="回应截止" required>
          <el-date-picker v-model="createForm.responseDueAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="w-full" />
        </el-form-item>
        <el-form-item label="附件来源"><el-input v-model="createForm.attachmentSource" placeholder="录像/听课表/附件 URI" /></el-form-item>
        <el-form-item label="周期判断"><el-switch v-model="createForm.needsImprovement" active-text="需要改进" /></el-form-item>
        <el-form-item label="概述"><el-input v-model="createForm.summary" type="textarea" :rows="2" /></el-form-item>

        <div v-for="(item, idx) in createForm.items" :key="idx" class="border rounded p-3 mb-3 bg-gray-50">
          <div class="flex gap-2 mb-2">
            <el-select v-model="item.dimension" placeholder="维度" class="w-40">
              <el-option v-for="(label, key) in dimensions" :key="key" :label="label" :value="key" />
            </el-select>
            <el-select v-model="item.grade" placeholder="等级" class="w-40">
              <el-option v-for="(label, key) in grades" :key="key" :label="label" :value="key" />
            </el-select>
            <el-select v-model="item.sensitivityLevel" placeholder="可见级别" class="w-48">
              <el-option label="公开（非敏感）" value="PUBLIC" />
              <el-option label="讲师改进相关（脱敏）" value="INSTRUCTOR_RELATED" />
              <el-option label="受限（学员身份/投诉/内部判断）" value="RESTRICTED" />
            </el-select>
            <el-button text type="danger" @click="createForm.items.splice(idx, 1)">删除</el-button>
          </div>
          <el-input v-model="item.fact" type="textarea" :rows="2" placeholder="可核验事实，不写主观推断" class="mb-2" />
          <el-input v-model="item.advice" placeholder="改进建议" />
        </div>
        <el-button @click="addItem">增加维度</el-button>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">保存观察</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { courseApi, instructorApi, observationApi, type Course, type Instructor, type ObservationSummary, type ObservationReminder } from '../api'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const roles: string[] = user.roles || []
const isSupervisor = roles.includes('SUPERVISOR')

const observations = ref<ObservationSummary[]>([])
const reminders = ref<ObservationReminder[]>([])
const courses = ref<Course[]>([])
const instructors = ref<Instructor[]>([])
const loading = ref(false)
const saving = ref(false)
const createVisible = ref(false)
const filter = reactive<{ courseId?: number; instructorId?: number; status?: string }>({})
const dimensions: Record<string, string> = {
  ORGANIZATION: '课堂组织', ACCURACY: '内容准确性', INTERACTION: '互动引导', TIME_CONTROL: '时间控制', OTHER: '其他'
}
const grades: Record<string, string> = {
  EXCELLENT: '优秀', QUALIFIED: '合格', NEEDS_IMPROVEMENT: '待改进', SERIOUS_ISSUE: '严重问题'
}
const createForm = reactive<any>({ items: [] })

const statusType = (status: string) => {
  if (status.includes('OVERDUE') || status === 'INSTRUCTOR_REFUSED') return 'danger'
  if (status.includes('PENDING') || status === 'ESCALATION_REQUESTED') return 'warning'
  if (status === 'CLOSED') return 'success'
  if (status === 'INTENSIVE_COACHING') return 'danger'
  return 'primary'
}
const formatDate = (s?: string) => s ? new Date(s).toLocaleString('zh-CN') : '-'

const load = async () => {
  loading.value = true
  try {
    const [list, reminderList] = await Promise.all([
      observationApi.list(filter),
      observationApi.reminders().catch(() => ({ data: { data: [] } }))
    ])
    observations.value = list.data.data
    reminders.value = reminderList.data.data || []
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载观察列表失败')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  Object.assign(createForm, {
    courseId: undefined, sessionNo: '', instructorId: undefined, occurredAt: undefined,
    responseDueAt: undefined, attachmentSource: '', summary: '', needsImprovement: true, items: []
  })
  addItem()
  createVisible.value = true
}
const addItem = () => createForm.items.push({
  dimension: 'ORGANIZATION', grade: 'QUALIFIED', sensitivityLevel: 'INSTRUCTOR_RELATED', fact: '', advice: ''
})
const submitCreate = async () => {
  saving.value = true
  try {
    const res = await observationApi.create(createForm)
    ElMessage.success('观察已建立')
    createVisible.value = false
    router.push(`/teaching-observations/${res.data.data.id}`)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '建立观察失败')
  } finally {
    saving.value = false
  }
}
const openDetail = (id: number) => router.push(`/teaching-observations/${id}`)

onMounted(async () => {
  try {
    courses.value = (await courseApi.getAllList()).data
    instructors.value = (await instructorApi.getAllList()).data
  } catch { /* 不阻塞列表 */ }
  load()
})
</script>
