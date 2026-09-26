<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber, ElSelect, ElOption, ElMessageBox, ElMessage, ElPagination, ElTag, ElRate } from 'element-plus'
import { Plus as PlusIcon, Edit as EditIcon, Delete as DeleteIcon, Search as SearchIcon } from '@element-plus/icons-vue'
import { instructorApi, type Instructor } from '../api'

const router = useRouter()
const instructors = ref<Instructor[]>([])
const totalElements = ref(0)
const currentPage = ref(0)
const pageSize = ref(10)
const dialogVisible = ref(false)
const isEditing = ref(false)
const searchKeyword = ref('')
const filterStatus = ref('')
const filterAvailable = ref<boolean | undefined>(undefined)

const form = ref({
  name: '',
  phone: '',
  email: '',
  certificates: '',
  teachingYears: 0,
  studentRating: 0,
  expertiseAreas: '',
  teachingStyle: '',
  resume: '',
  status: 'ACTIVE',
  available: true,
  maxCoursesPerMonth: 4
})

const fetchInstructors = async () => {
  try {
    const response = await instructorApi.getAll(currentPage.value, pageSize.value, filterStatus.value || undefined, filterAvailable.value)
    const data = response.data
    instructors.value = data.content
    totalElements.value = data.totalElements
  } catch (error) {
    ElMessage.error('获取讲师列表失败')
  }
}

const filteredInstructors = computed(() => {
  if (!searchKeyword.value) return instructors.value
  const keyword = searchKeyword.value.toLowerCase()
  return instructors.value.filter(i => 
    i.name.toLowerCase().includes(keyword) || 
    i.expertiseAreas.toLowerCase().includes(keyword)
  )
})

const openDialog = (instructor?: Instructor) => {
  if (instructor) {
    isEditing.value = true
    form.value = {
      name: instructor.name,
      phone: instructor.phone || '',
      email: instructor.email || '',
      certificates: instructor.certificates || '',
      teachingYears: instructor.teachingYears,
      studentRating: instructor.studentRating,
      expertiseAreas: instructor.expertiseAreas || '',
      teachingStyle: instructor.teachingStyle || '',
      resume: instructor.resume || '',
      status: instructor.status || 'ACTIVE',
      available: instructor.available,
      maxCoursesPerMonth: instructor.maxCoursesPerMonth
    }
  } else {
    isEditing.value = false
    form.value = {
      name: '',
      phone: '',
      email: '',
      certificates: '',
      teachingYears: 0,
      studentRating: 0,
      expertiseAreas: '',
      teachingStyle: '',
      resume: '',
      status: 'ACTIVE',
      available: true,
      maxCoursesPerMonth: 4
    }
  }
  dialogVisible.value = true
}

const saveInstructor = async () => {
  try {
    if (isEditing.value) {
      const instructor = instructors.value.find(i => i.name === form.value.name)
      if (instructor) {
        await instructorApi.update(instructor.id, form.value)
        ElMessage.success('讲师信息更新成功')
      }
    } else {
      await instructorApi.create(form.value)
      ElMessage.success('讲师创建成功')
    }
    dialogVisible.value = false
    fetchInstructors()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteInstructor = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该讲师吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await instructorApi.delete(id)
    ElMessage.success('删除成功')
    fetchInstructors()
  } catch (error) {
    ElMessage.info('已取消删除')
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchInstructors()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 0
  fetchInstructors()
}

const handleFilter = () => {
  currentPage.value = 0
  fetchInstructors()
}

const viewDetail = (id: number) => {
  router.push(`/instructors/${id}`)
}

const getStatusTagType = (status: string) => {
  const map: Record<string, 'success' | 'info'> = {
    'ACTIVE': 'success',
    'INACTIVE': 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'ACTIVE': '在职',
    'INACTIVE': '离职'
  }
  return map[status] || status
}

onMounted(fetchInstructors)
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm p-6">
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-3">
        <div class="relative">
          <SearchIcon class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input 
            v-model="searchKeyword"
            type="text" 
            placeholder="搜索讲师姓名或擅长领域"
            class="pl-10 pr-4 py-2 border border-gray-200 rounded-lg text-sm w-64 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <ElSelect v-model="filterStatus" placeholder="筛选状态" style="width: 120px">
          <ElOption value="ACTIVE" label="在职" />
          <ElOption value="INACTIVE" label="离职" />
        </ElSelect>
        <ElSelect v-model="filterAvailable" placeholder="筛选可用性" style="width: 120px">
          <ElOption :value="true" label="可授课" />
          <ElOption :value="false" label="不可用" />
        </ElSelect>
        <ElButton @click="handleFilter">筛选</ElButton>
      </div>
      <ElButton type="primary" @click="openDialog()">
        <PlusIcon class="w-4 h-4 mr-1" />
        添加讲师
      </ElButton>
    </div>
    
    <ElTable :data="filteredInstructors" border stripe row-key="id">
      <ElTableColumn prop="id" label="ID" width="60" />
      <ElTableColumn prop="name" label="姓名" />
      <ElTableColumn prop="phone" label="手机号" />
      <ElTableColumn prop="email" label="邮箱" />
      <ElTableColumn prop="status" label="状态">
        <template #default="{ row }">
          <ElTag :type="getStatusTagType(row.status)">{{ getStatusText(row.status) }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="available" label="可用性">
        <template #default="{ row }">
          <ElTag :type="row.available ? 'success' : 'danger'">{{ row.available ? '可授课' : '不可用' }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="teachingYears" label="教龄(年)" width="80" />
      <ElTableColumn prop="studentRating" label="学员评分" width="80">
        <template #default="{ row }">
          <ElRate :value="row.studentRating" disabled allow-half />
        </template>
      </ElTableColumn>
      <ElTableColumn prop="expertiseAreas" label="擅长领域" show-overflow-tooltip />
      <ElTableColumn prop="teachingStyle" label="授课风格" />
      <ElTableColumn prop="currentMonthCourses" label="本月课程" width="80">
        <template #default="{ row }">
          <span>{{ row.currentMonthCourses }}/{{ row.maxCoursesPerMonth }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="createdAt" label="创建时间" width="180" />
      <ElTableColumn label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <ElButton type="info" size="small" @click="viewDetail((row as Instructor).id)">
            详情
          </ElButton>
          <ElButton type="primary" size="small" @click="openDialog(row as Instructor)">
            <EditIcon class="w-4 h-4" />
          </ElButton>
          <ElButton type="danger" size="small" @click="deleteInstructor(row.id)">
            <DeleteIcon class="w-4 h-4" />
          </ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
    
    <div class="flex justify-center mt-6">
      <ElPagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="totalElements"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
    
    <ElDialog 
      :title="isEditing ? '编辑讲师' : '添加讲师'" 
      v-model="dialogVisible"
      width="600px"
    >
      <ElForm :model="form" label-width="100px">
        <div class="grid grid-cols-2 gap-4">
          <ElFormItem label="姓名" required>
            <ElInput v-model="form.name" />
          </ElFormItem>
          <ElFormItem label="手机号">
            <ElInput v-model="form.phone" />
          </ElFormItem>
          <ElFormItem label="邮箱">
            <ElInput v-model="form.email" />
          </ElFormItem>
          <ElFormItem label="教龄(年)" required>
            <ElInputNumber v-model="form.teachingYears" :min="0" :max="50" />
          </ElFormItem>
          <ElFormItem label="学员评分" required>
            <ElInputNumber v-model="form.studentRating" :min="0" :max="5" :step="0.1" />
          </ElFormItem>
          <ElFormItem label="月授课上限">
            <ElInputNumber v-model="form.maxCoursesPerMonth" :min="1" :max="20" />
          </ElFormItem>
        </div>
        <ElFormItem label="证书资质">
          <ElInput v-model="form.certificates" placeholder="多个证书用逗号分隔" />
        </ElFormItem>
        <ElFormItem label="擅长领域">
          <ElInput v-model="form.expertiseAreas" placeholder="多个领域用逗号分隔" />
        </ElFormItem>
        <ElFormItem label="授课风格">
          <ElInput v-model="form.teachingStyle" />
        </ElFormItem>
        <ElFormItem label="个人简介">
          <ElInput v-model="form.resume" type="textarea" :rows="3" />
        </ElFormItem>
        <div class="grid grid-cols-2 gap-4">
          <ElFormItem label="状态">
            <ElSelect v-model="form.status">
              <ElOption value="ACTIVE" label="在职" />
              <ElOption value="INACTIVE" label="离职" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="可用性">
            <ElSelect v-model="form.available">
              <ElOption :value="true" label="可授课" />
              <ElOption :value="false" label="不可用" />
            </ElSelect>
          </ElFormItem>
        </div>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="saveInstructor">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>