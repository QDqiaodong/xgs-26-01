<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElMessageBox, ElMessage, ElPagination, ElTag } from 'element-plus'
import { Plus as PlusIcon, Edit as EditIcon, Delete as DeleteIcon, Search as SearchIcon } from '@element-plus/icons-vue'
import { courseApi, type Course } from '../api'

const router = useRouter()
const courses = ref<Course[]>([])
const totalElements = ref(0)
const currentPage = ref(0)
const pageSize = ref(10)
const dialogVisible = ref(false)
const isEditing = ref(false)
const searchKeyword = ref('')
const filterIndustry = ref('')
const filterStatus = ref('')

const form = ref({
  name: '',
  industry: '',
  difficulty: '',
  requiredQualifications: '',
  styleRequirements: '',
  description: '',
  totalHours: 0,
  maxStudents: 0,
  status: 'ACTIVE'
})

const industries = ['IT互联网', '金融', '教育培训', '医疗健康', '制造业', '零售', '物流', '房地产']
const difficulties = ['初级', '中级', '高级']

const fetchCourses = async () => {
  try {
    const response = await courseApi.getAll(currentPage.value, pageSize.value, filterIndustry.value || undefined, filterStatus.value || undefined)
    const data = response.data
    courses.value = data.content
    totalElements.value = data.totalElements
  } catch (error) {
    ElMessage.error('获取课程列表失败')
  }
}

const filteredCourses = computed(() => {
  if (!searchKeyword.value) return courses.value
  const keyword = searchKeyword.value.toLowerCase()
  return courses.value.filter(c => 
    c.name.toLowerCase().includes(keyword) || 
    c.industry.toLowerCase().includes(keyword)
  )
})

const openDialog = (course?: Course) => {
  if (course) {
    isEditing.value = true
    form.value = {
      name: course.name,
      industry: course.industry,
      difficulty: course.difficulty,
      requiredQualifications: course.requiredQualifications || '',
      styleRequirements: course.styleRequirements || '',
      description: course.description || '',
      totalHours: course.totalHours || 0,
      maxStudents: course.maxStudents || 0,
      status: course.status || 'ACTIVE'
    }
  } else {
    isEditing.value = false
    form.value = {
      name: '',
      industry: '',
      difficulty: '',
      requiredQualifications: '',
      styleRequirements: '',
      description: '',
      totalHours: 0,
      maxStudents: 0,
      status: 'ACTIVE'
    }
  }
  dialogVisible.value = true
}

const saveCourse = async () => {
  try {
    if (isEditing.value) {
      const course = courses.value.find(c => c.name === form.value.name)
      if (course) {
        await courseApi.update(course.id, form.value)
        ElMessage.success('课程更新成功')
      }
    } else {
      await courseApi.create(form.value)
      ElMessage.success('课程创建成功')
    }
    dialogVisible.value = false
    fetchCourses()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteCourse = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该课程吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await courseApi.delete(id)
    ElMessage.success('删除成功')
    fetchCourses()
  } catch (error) {
    ElMessage.info('已取消删除')
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchCourses()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 0
  fetchCourses()
}

const handleFilter = () => {
  currentPage.value = 0
  fetchCourses()
}

const viewDetail = (id: number) => {
  router.push(`/courses/${id}`)
}

const getStatusTagType = (status: string) => {
  const map: Record<string, 'success' | 'warning' | 'info'> = {
    'ACTIVE': 'success',
    'DRAFT': 'warning',
    'INACTIVE': 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'ACTIVE': '启用',
    'DRAFT': '草稿',
    'INACTIVE': '停用'
  }
  return map[status] || status
}

onMounted(fetchCourses)
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
            placeholder="搜索课程名称或行业"
            class="pl-10 pr-4 py-2 border border-gray-200 rounded-lg text-sm w-64 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <ElSelect v-model="filterIndustry" placeholder="筛选行业" style="width: 160px">
          <ElOption v-for="item in industries" :key="item" :value="item" :label="item" />
        </ElSelect>
        <ElSelect v-model="filterStatus" placeholder="筛选状态" style="width: 120px">
          <ElOption value="ACTIVE" label="启用" />
          <ElOption value="DRAFT" label="草稿" />
          <ElOption value="INACTIVE" label="停用" />
        </ElSelect>
        <ElButton @click="handleFilter">筛选</ElButton>
      </div>
      <ElButton type="primary" @click="openDialog()">
        <PlusIcon class="w-4 h-4 mr-1" />
        添加课程
      </ElButton>
    </div>
    
    <ElTable :data="filteredCourses" border stripe row-key="id">
      <ElTableColumn prop="id" label="ID" width="60" />
      <ElTableColumn prop="name" label="课程名称" />
      <ElTableColumn prop="industry" label="所属行业" />
      <ElTableColumn prop="difficulty" label="难度" />
      <ElTableColumn prop="status" label="状态">
        <template #default="{ row }">
          <ElTag :type="getStatusTagType(row.status)">{{ getStatusText(row.status) }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="totalHours" label="总课时" />
      <ElTableColumn prop="maxStudents" label="学员上限" />
      <ElTableColumn prop="createdAt" label="创建时间" width="180" />
      <ElTableColumn label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <ElButton type="info" size="small" @click="viewDetail((row as Course).id)">
            详情
          </ElButton>
          <ElButton type="primary" size="small" @click="openDialog(row as Course)">
            <EditIcon class="w-4 h-4" />
          </ElButton>
          <ElButton type="danger" size="small" @click="deleteCourse(row.id)">
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
      :title="isEditing ? '编辑课程' : '添加课程'" 
      v-model="dialogVisible"
      width="500px"
    >
      <ElForm :model="form" label-width="100px">
        <ElFormItem label="课程名称" required>
          <ElInput v-model="form.name" />
        </ElFormItem>
        <ElFormItem label="所属行业" required>
          <ElSelect v-model="form.industry">
            <ElOption v-for="item in industries" :key="item" :value="item" :label="item" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="难度" required>
          <ElSelect v-model="form.difficulty">
            <ElOption v-for="item in difficulties" :key="item" :value="item" :label="item" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="课程状态">
          <ElSelect v-model="form.status">
            <ElOption value="ACTIVE" label="启用" />
            <ElOption value="DRAFT" label="草稿" />
            <ElOption value="INACTIVE" label="停用" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="总课时">
          <ElInput v-model.number="form.totalHours" type="number" placeholder="小时数" />
        </ElFormItem>
        <ElFormItem label="学员上限">
          <ElInput v-model.number="form.maxStudents" type="number" placeholder="人数" />
        </ElFormItem>
        <ElFormItem label="必备资质">
          <ElInput v-model="form.requiredQualifications" placeholder="多个资质用逗号分隔" />
        </ElFormItem>
        <ElFormItem label="风格要求">
          <ElInput v-model="form.styleRequirements" placeholder="多个风格用逗号分隔" />
        </ElFormItem>
        <ElFormItem label="课程描述">
          <ElInput v-model="form.description" type="textarea" :rows="3" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="saveCourse">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>