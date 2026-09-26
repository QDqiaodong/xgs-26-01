<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElCheckbox, ElMessageBox } from 'element-plus'
import { courseApi, skillApi, matchApi, type Course, type CourseRequiredSkill, type MatchRecord } from '../api'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.id)
const course = ref<Course | null>(null)
const courseSkills = ref<CourseRequiredSkill[]>([])
const matchRecords = ref<MatchRecord[]>([])
const showAddSkillDialog = ref(false)
const skillForm = ref({
  skillName: '',
  skillType: 'QUALIFICATION',
  isMandatory: true,
  skillLevel: ''
})

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'ACTIVE': '启用',
    'DRAFT': '草稿',
    'INACTIVE': '停用'
  }
  return map[status] || status
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const loadCourse = async () => {
  try {
    const response = await courseApi.getById(courseId)
    course.value = response.data
  } catch (error) {
    ElMessage.error('加载课程信息失败')
  }
}

const loadCourseSkills = async () => {
  try {
    const response = await skillApi.getCourseSkills(courseId)
    courseSkills.value = response.data
  } catch (error) {
    ElMessage.error('加载课程技能失败')
  }
}

const loadMatchRecords = async () => {
  try {
    const response = await matchApi.getByCourseId(courseId)
    matchRecords.value = response.data
  } catch (error) {
    ElMessage.error('加载匹配记录失败')
  }
}

const addSkill = () => {
  skillForm.value = {
    skillName: '',
    skillType: 'QUALIFICATION',
    isMandatory: true,
    skillLevel: ''
  }
  showAddSkillDialog.value = true
}

const saveSkill = async () => {
  if (!skillForm.value.skillName) {
    ElMessage.warning('请输入技能名称')
    return
  }
  try {
    await skillApi.addCourseSkill(courseId, skillForm.value.skillName, skillForm.value.skillType, skillForm.value.isMandatory)
    ElMessage.success('技能添加成功')
    showAddSkillDialog.value = false
    loadCourseSkills()
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

const deleteSkill = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该技能？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await skillApi.removeCourseSkill(courseId, id)
    ElMessage.success('删除成功')
    loadCourseSkills()
  } catch (error) {
    ElMessage.info('已取消删除')
  }
}

const goBack = () => {
  router.push('/courses')
}

onMounted(() => {
  loadCourse()
  loadCourseSkills()
  loadMatchRecords()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center gap-4">
      <ElButton @click="goBack()">
        <ArrowLeft class="w-4 h-4 mr-1" />
        返回列表
      </ElButton>
      <div>
        <h2 class="text-xl font-bold text-gray-800">{{ course?.name }}</h2>
        <p class="text-sm text-gray-500">课程详情</p>
      </div>
    </div>

    <div v-if="course" class="bg-white rounded-xl shadow-sm p-6">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div class="bg-blue-50 rounded-lg p-4">
          <p class="text-sm text-gray-500">所属行业</p>
          <p class="font-medium">{{ course.industry }}</p>
        </div>
        <div class="bg-green-50 rounded-lg p-4">
          <p class="text-sm text-gray-500">难度等级</p>
          <p class="font-medium">{{ course.difficulty }}</p>
        </div>
        <div class="bg-yellow-50 rounded-lg p-4">
          <p class="text-sm text-gray-500">课程状态</p>
          <p class="font-medium">{{ getStatusText(course.status) }}</p>
        </div>
        <div class="bg-purple-50 rounded-lg p-4">
          <p class="text-sm text-gray-500">总课时</p>
          <p class="font-medium">{{ course.totalHours }}小时</p>
        </div>
      </div>

      <div class="border-t border-gray-200 pt-6">
        <h3 class="font-semibold text-gray-800 mb-4">课程信息</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="text-sm text-gray-500">必备资质</label>
            <p class="text-gray-800">{{ course.requiredQualifications || '-' }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">风格要求</label>
            <p class="text-gray-800">{{ course.styleRequirements || '-' }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">学员上限</label>
            <p class="text-gray-800">{{ course.maxStudents || '-' }}人</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">课程描述</label>
            <p class="text-gray-800">{{ course.description || '-' }}</p>
          </div>
        </div>
      </div>

      <div class="border-t border-gray-200 pt-6 mt-6">
        <h3 class="font-semibold text-gray-800 mb-4">课程技能要求</h3>
        <div class="flex items-center justify-between mb-4">
          <p class="text-sm text-gray-500">共 {{ courseSkills.length }} 项技能要求</p>
          <ElButton type="primary" @click="addSkill()">
            <Plus class="w-4 h-4 mr-1" />
            添加技能
          </ElButton>
        </div>
        
        <div v-if="courseSkills.length === 0" class="text-center py-8 text-gray-400">
          暂无技能要求
        </div>
        
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div v-for="skill in courseSkills" :key="skill.id" class="bg-gray-50 rounded-lg p-4">
            <div class="flex items-center justify-between">
              <span class="font-medium">{{ skill.skillName }}</span>
              <ElButton type="danger" size="small" @click="deleteSkill(skill.id)">
                <Delete class="w-4 h-4" />
              </ElButton>
            </div>
            <div class="mt-2 text-sm text-gray-500">
              <p>类型: {{ skill.skillType }}</p>
              <p>级别: {{ skill.skillLevel || '-' }}</p>
              <p>必需: {{ skill.isMandatory ? '是' : '否' }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="border-t border-gray-200 pt-6 mt-6">
        <h3 class="font-semibold text-gray-800 mb-4">匹配历史记录</h3>
        <div v-if="matchRecords.length === 0" class="text-center py-8 text-gray-400">
          暂无匹配记录
        </div>
        <div v-else class="space-y-3">
          <div v-for="record in matchRecords" :key="record.id" class="bg-gray-50 rounded-lg p-4">
            <div class="flex items-center justify-between">
              <div>
                <p class="font-medium">推演记录 #{{ record.id }}</p>
                <p class="text-sm text-gray-500">{{ formatDate(record.createdAt) }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm text-gray-500">最佳分数</p>
                <p class="font-bold text-blue-600">{{ record.bestScore }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ElDialog title="添加课程技能" v-model="showAddSkillDialog">
      <ElForm :model="skillForm" label-width="100px">
        <ElFormItem label="技能名称" required>
          <ElInput v-model="skillForm.skillName" />
        </ElFormItem>
        <ElFormItem label="技能类型">
          <ElSelect v-model="skillForm.skillType">
            <ElOption value="QUALIFICATION" label="资质证书" />
            <ElOption value="TECHNICAL" label="技术技能" />
            <ElOption value="SOFT" label="软技能" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="技能级别">
          <ElInput v-model="skillForm.skillLevel" placeholder="如：高级、中级、初级" />
        </ElFormItem>
        <ElFormItem>
          <ElCheckbox v-model="skillForm.isMandatory">必需技能</ElCheckbox>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="showAddSkillDialog = false">取消</ElButton>
        <ElButton type="primary" @click="saveSkill">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>