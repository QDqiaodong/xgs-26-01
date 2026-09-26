<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElMessageBox } from 'element-plus'
import { instructorApi, skillApi, matchApi, type Instructor, type InstructorSkill, type MatchRecord } from '../api'

const route = useRoute()
const router = useRouter()
const instructorId = Number(route.params.id)
const inst = ref<Instructor | null>(null)
const instructorSkills = ref<InstructorSkill[]>([])
const matchRecords = ref<MatchRecord[]>([])
const showAddSkillDialog = ref(false)
const skillForm = ref({
  skillName: '',
  skillType: 'QUALIFICATION',
  description: '',
  acquiredDate: '',
  expiryDate: ''
})

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'ACTIVE': '在职',
    'INACTIVE': '离职'
  }
  return map[status] || status
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const loadInstructor = async () => {
  try {
    const response = await instructorApi.getById(instructorId)
    inst.value = response.data
  } catch (error) {
    ElMessage.error('加载讲师信息失败')
  }
}

const loadInstructorSkills = async () => {
  try {
    const response = await skillApi.getInstructorSkills(instructorId)
    instructorSkills.value = response.data
  } catch (error) {
    ElMessage.error('加载讲师技能失败')
  }
}

const loadMatchRecords = async () => {
  try {
    const response = await matchApi.getAll(0, 100)
    matchRecords.value = response.data.content || []
  } catch (error) {
    ElMessage.error('加载匹配记录失败')
  }
}

const addSkill = () => {
  skillForm.value = {
    skillName: '',
    skillType: 'QUALIFICATION',
    description: '',
    acquiredDate: '',
    expiryDate: ''
  }
  showAddSkillDialog.value = true
}

const saveSkill = async () => {
  if (!skillForm.value.skillName) {
    ElMessage.warning('请输入技能名称')
    return
  }
  try {
    await skillApi.addInstructorSkill(instructorId, skillForm.value.skillName, skillForm.value.skillType)
    ElMessage.success('技能添加成功')
    showAddSkillDialog.value = false
    loadInstructorSkills()
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
    await skillApi.removeInstructorSkill(instructorId, id)
    ElMessage.success('删除成功')
    loadInstructorSkills()
  } catch (error) {
    ElMessage.info('已取消删除')
  }
}

const goBack = () => {
  router.push('/instructors')
}

onMounted(() => {
  loadInstructor()
  loadInstructorSkills()
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
        <h2 class="text-xl font-bold text-gray-800">{{ inst?.name }}</h2>
        <p class="text-sm text-gray-500">讲师详情</p>
      </div>
    </div>

    <div v-if="inst" class="bg-white rounded-xl shadow-sm p-6">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div class="bg-blue-50 rounded-lg p-4">
          <p class="text-sm text-gray-500">讲师状态</p>
          <p class="font-medium">{{ getStatusText(inst.status) }}</p>
        </div>
        <div class="bg-green-50 rounded-lg p-4">
          <p class="text-sm text-gray-500">可用性</p>
          <p class="font-medium">{{ inst.available ? '可授课' : '不可用' }}</p>
        </div>
        <div class="bg-yellow-50 rounded-lg p-4">
          <p class="text-sm text-gray-500">教龄</p>
          <p class="font-medium">{{ inst.teachingYears }}年</p>
        </div>
        <div class="bg-purple-50 rounded-lg p-4">
          <p class="text-sm text-gray-500">学员评分</p>
          <p class="font-medium">{{ inst.studentRating.toFixed(1) }}</p>
        </div>
      </div>

      <div class="border-t border-gray-200 pt-6">
        <h3 class="font-semibold text-gray-800 mb-4">基本信息</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="text-sm text-gray-500">手机号</label>
            <p class="text-gray-800">{{ inst.phone || '-' }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">邮箱</label>
            <p class="text-gray-800">{{ inst.email || '-' }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">证书资质</label>
            <p class="text-gray-800">{{ inst.certificates || '-' }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">擅长领域</label>
            <p class="text-gray-800">{{ inst.expertiseAreas || '-' }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">授课风格</label>
            <p class="text-gray-800">{{ inst.teachingStyle || '-' }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">月授课上限</label>
            <p class="text-gray-800">{{ inst.maxCoursesPerMonth }}门</p>
          </div>
        </div>
      </div>

      <div class="border-t border-gray-200 pt-6 mt-6">
        <h3 class="font-semibold text-gray-800 mb-4">个人简介</h3>
        <p class="text-gray-800 bg-gray-50 rounded-lg p-4">{{ inst.resume || '暂无简介' }}</p>
      </div>

      <div class="border-t border-gray-200 pt-6 mt-6">
        <h3 class="font-semibold text-gray-800 mb-4">技能资质</h3>
        <div class="flex items-center justify-between mb-4">
          <p class="text-sm text-gray-500">共 {{ instructorSkills.length }} 项技能</p>
          <ElButton type="primary" @click="addSkill()">
            <Plus class="w-4 h-4 mr-1" />
            添加技能
          </ElButton>
        </div>
        
        <div v-if="instructorSkills.length === 0" class="text-center py-8 text-gray-400">
          暂无技能资质
        </div>
        
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div v-for="skill in instructorSkills" :key="skill.id" class="bg-gray-50 rounded-lg p-4">
            <div class="flex items-center justify-between">
              <span class="font-medium">{{ skill.skillName }}</span>
              <ElButton type="danger" size="small" @click="deleteSkill(skill.id)">
                <Delete class="w-4 h-4" />
              </ElButton>
            </div>
            <div class="mt-2 text-sm text-gray-500">
              <p>类型: {{ skill.skillType }}</p>
              <p>描述: {{ skill.description || '-' }}</p>
              <p>获得时间: {{ formatDate(skill.acquiredDate) }}</p>
              <p v-if="skill.expiryDate">到期时间: {{ formatDate(skill.expiryDate) }}</p>
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
                <p class="font-medium">{{ record.courseName }}</p>
                <p class="text-sm text-gray-500">{{ formatDate(record.createdAt) }}</p>
              </div>
              <div class="text-right">
                <p class="text-sm text-gray-500">匹配分数</p>
                <p class="font-bold text-blue-600">{{ record.bestScore }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ElDialog title="添加技能" v-model="showAddSkillDialog">
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
        <ElFormItem label="技能描述">
          <ElInput v-model="skillForm.description" type="textarea" :rows="2" />
        </ElFormItem>
        <ElFormItem label="获得时间">
          <ElInput v-model="skillForm.acquiredDate" type="date" />
        </ElFormItem>
        <ElFormItem label="到期时间">
          <ElInput v-model="skillForm.expiryDate" type="date" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="showAddSkillDialog = false">取消</ElButton>
        <ElButton type="primary" @click="saveSkill">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>