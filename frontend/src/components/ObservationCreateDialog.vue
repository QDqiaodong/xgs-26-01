<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="建立带教观察（按课程场次）"
    width="860px"
    @open="reset"
  >
    <el-form :model="form" label-width="110px">
      <div class="grid grid-cols-2 gap-x-4">
        <el-form-item label="课程" required>
          <el-select v-model="form.courseId" placeholder="选择课程" class="w-full">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程场次" required>
          <el-input v-model="form.sessionNo" placeholder="如：第3场/10月21日下午场" />
        </el-form-item>
        <el-form-item label="被观察讲师" required>
          <el-select v-model="form.instructorId" placeholder="选择讲师" class="w-full" filterable>
            <el-option v-for="i in instructors" :key="i.id" :label="i.name" :value="i.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授课发生时间" required>
          <el-date-picker v-model="form.observedAt" type="datetime" class="w-full"
            value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择授课时间" />
        </el-form-item>
        <el-form-item label="确认期限" class="col-span-2">
          <el-date-picker v-model="form.ackDueAt" type="datetime" class="w-64"
            value-format="YYYY-MM-DDTHH:mm:ss" placeholder="留空默认72小时" />
        </el-form-item>
      </div>
      <el-form-item label="观察标题" required>
        <el-input v-model="form.title" placeholder="如：Java高级开发实战 第3场带教观察" />
      </el-form-item>
      <el-form-item label="整体说明">
        <el-input v-model="form.summary" type="textarea" :rows="2"
          placeholder="督导整体说明（内部视角，按分级规则对讲师/查看者不可见）" />
      </el-form-item>
      <el-form-item label="附件来源">
        <el-input v-model="form.attachmentSource" placeholder="如：课堂录像2段、纸质听课记录表、学员问卷24份" />
      </el-form-item>

      <el-divider content-position="left">四个维度：事实 / 等级 / 改进建议（分级保存）</el-divider>

      <el-alert type="warning" :closable="false" class="mb-3">
        涉及学员身份、投诉原文的事实请选择“敏感”，系统保存原文并向讲师展示脱敏文本；
        内部判断仅督导/课程负责人可见；“与改进相关”内容讲师本人可见。
      </el-alert>

      <div v-for="item in form.items" :key="item.dimension" class="border rounded-lg p-3 mb-3 bg-gray-50">
        <div class="flex items-center justify-between mb-2">
          <span class="font-semibold">{{ dimensionLabels[item.dimension] }}</span>
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-500">等级</span>
            <el-select v-model="item.grade" class="w-24">
              <el-option label="A 优秀" value="A" />
              <el-option label="B 合格" value="B" />
              <el-option label="C 待改进" value="C" />
              <el-option label="D 不合格" value="D" />
            </el-select>
            <el-checkbox v-model="item.gradeInternal">内部等级</el-checkbox>
          </div>
        </div>
        <el-input v-model="item.fact" type="textarea" :rows="2" class="mb-2"
          :placeholder="`记录「${dimensionLabels[item.dimension]}」的课堂事实（发生了什么，不写主观推测）`" />
        <div class="flex items-center gap-2 mb-2 flex-wrap">
          <span class="text-sm text-gray-500">事实分级：</span>
          <el-radio-group v-model="item.factLevel" size="small">
            <el-radio-button label="IMPROVEMENT_RELATED">改进相关(讲师可见)</el-radio-button>
            <el-radio-button label="SENSITIVE">敏感(讲师看脱敏)</el-radio-button>
            <el-radio-button label="INTERNAL">内部判断</el-radio-button>
            <el-radio-button label="PUBLIC">公开结论</el-radio-button>
          </el-radio-group>
          <el-select v-if="item.factLevel === 'SENSITIVE'" v-model="item.sensitiveType"
            placeholder="敏感类型" class="w-44">
            <el-option label="学员身份" value="STUDENT_ID" />
            <el-option label="投诉原文" value="COMPLAINT" />
            <el-option label="内部判断" value="INTERNAL_JUDGEMENT" />
          </el-select>
        </div>
        <el-input v-model="item.suggestion" type="textarea" :rows="2"
          placeholder="改进建议（与本人改进相关，讲师可见）" />
      </div>

      <el-form-item label="附件登记">
        <el-input v-model="attachmentText" type="textarea" :rows="2"
          placeholder="每行一个附件：名称 | 来源说明（可选）" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">建立观察</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  observationApi,
  courseApi,
  instructorApi,
  type Course,
  type Instructor,
  type ObservationItemInput
} from '../api'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: [id: number]
}>()

const courses = ref<Course[]>([])
const instructors = ref<Instructor[]>([])
const submitting = ref(false)
const attachmentText = ref('')

const dimensions = [
  { key: 'CLASSROOM_ORGANIZATION', label: '课堂组织' },
  { key: 'CONTENT_ACCURACY', label: '内容准确性' },
  { key: 'INTERACTION_GUIDANCE', label: '互动引导' },
  { key: 'TIME_CONTROL', label: '时间控制' }
]
const dimensionLabels: Record<string, string> = Object.fromEntries(
  dimensions.map(d => [d.key, d.label])
)

const blankForm = () => ({
  courseId: undefined as number | undefined,
  sessionNo: '',
  instructorId: undefined as number | undefined,
  observedAt: '',
  ackDueAt: '',
  title: '',
  summary: '',
  attachmentSource: '',
  items: dimensions.map(d => ({
    dimension: d.key,
    fact: '',
    factLevel: 'IMPROVEMENT_RELATED',
    sensitiveType: 'STUDENT_ID',
    grade: 'B',
    gradeInternal: false,
    suggestion: '',
    suggestionLevel: 'IMPROVEMENT_RELATED'
  })) as ObservationItemInput[]
})

const form = ref(blankForm())

const reset = () => {
  form.value = blankForm()
  attachmentText.value = ''
}

const submit = async () => {
  const f = form.value
  if (!f.courseId || !f.instructorId) {
    ElMessage.warning('请选择课程与讲师')
    return
  }
  if (!f.sessionNo || !f.observedAt || !f.title) {
    ElMessage.warning('请填写场次、授课时间与标题')
    return
  }
  const items = f.items.filter(i => i.fact.trim())
  if (items.length === 0) {
    ElMessage.warning('至少填写一个维度的观察事实')
    return
  }
  const attachments = attachmentText.value.split('\n').map(l => l.trim()).filter(Boolean).map(line => {
    const [fileName, sourceNote] = line.split('|').map(s => s?.trim())
    return { fileName, sourceNote, sensitivityLevel: 'INTERNAL' }
  }).filter(a => a.fileName)

  submitting.value = true
  try {
    const res = await observationApi.create({
      courseId: f.courseId!,
      sessionNo: f.sessionNo,
      instructorId: f.instructorId!,
      observedAt: f.observedAt,
      title: f.title,
      summary: f.summary || undefined,
      attachmentSource: f.attachmentSource || undefined,
      ackDueAt: f.ackDueAt || undefined,
      items,
      attachments: attachments.length ? attachments : undefined
    })
    ElMessage.success('观察已建立，已生成讲师确认待办')
    emit('created', res.data.id)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '建立观察失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    courses.value = (await courseApi.getAllList()).data
    instructors.value = (await instructorApi.getAllList()).data
  } catch {
    // 下拉失败在提交时由后端兜底
  }
})
</script>
