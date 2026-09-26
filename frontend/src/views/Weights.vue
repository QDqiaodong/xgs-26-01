<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessageBox, ElMessage } from 'element-plus'
import { Plus as PlusIcon, Edit as EditIcon, Delete as DeleteIcon } from '@element-plus/icons-vue'
import { weightApi, type WeightConfig } from '../api'

const configs = ref<WeightConfig[]>([])
const dialogVisible = ref(false)
const isEditing = ref(false)

const form = ref({
  name: '',
  description: '',
  teachingYearsWeight: 0.2,
  qualificationWeight: 0.2,
  ratingWeight: 0.25,
  expertiseWeight: 0.2,
  styleWeight: 0.15,
  gapThreshold: 60
})

const totalWeight = computed(() => {
  return form.value.teachingYearsWeight + form.value.qualificationWeight + 
         form.value.ratingWeight + form.value.expertiseWeight + form.value.styleWeight
})

const fetchConfigs = async () => {
  try {
    const response = await weightApi.getAll()
    configs.value = response.data
  } catch (error) {
    ElMessage.error('获取权重配置失败')
  }
}

const openDialog = (config?: WeightConfig) => {
  if (config) {
    isEditing.value = true
    form.value = {
      name: config.name,
      description: config.description || '',
      teachingYearsWeight: config.teachingYearsWeight,
      qualificationWeight: config.qualificationWeight,
      ratingWeight: config.ratingWeight,
      expertiseWeight: config.expertiseWeight,
      styleWeight: config.styleWeight,
      gapThreshold: config.gapThreshold
    }
  } else {
    isEditing.value = false
    form.value = {
      name: '',
      description: '',
      teachingYearsWeight: 0.2,
      qualificationWeight: 0.2,
      ratingWeight: 0.25,
      expertiseWeight: 0.2,
      styleWeight: 0.15,
      gapThreshold: 60
    }
  }
  dialogVisible.value = true
}

const saveConfig = async () => {
  if (Math.abs(totalWeight.value - 1) > 0.01) {
    ElMessage.warning('五维度权重之和必须等于1')
    return
  }
  
  try {
    if (isEditing.value) {
      const config = configs.value.find(c => c.name === form.value.name)
      if (config) {
        await weightApi.update(config.id, form.value)
        ElMessage.success('权重配置更新成功')
      }
    } else {
      await weightApi.create(form.value)
      ElMessage.success('权重配置创建成功')
    }
    dialogVisible.value = false
    fetchConfigs()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteConfig = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该权重配置吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await weightApi.delete(id)
    ElMessage.success('删除成功')
    fetchConfigs()
  } catch (error) {
    ElMessage.info('已取消删除')
  }
}

onMounted(fetchConfigs)
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h3 class="font-semibold text-gray-800">权重配置管理</h3>
        <p class="text-sm text-gray-500 mt-1">五维度权重之和必须等于1，用于智能匹配算法计算</p>
      </div>
      <ElButton type="primary" @click="openDialog()">
        <PlusIcon class="w-4 h-4 mr-1" />
        添加配置
      </ElButton>
    </div>
    
    <ElTable :data="configs" border stripe row-key="id">
      <ElTableColumn prop="id" label="ID" width="60" />
      <ElTableColumn prop="name" label="配置名称" />
      <ElTableColumn prop="description" label="描述" show-overflow-tooltip />
      <ElTableColumn label="授课年限权重" width="120">
        <template #default="{ row }">{{ (row.teachingYearsWeight * 100).toFixed(0) }}%</template>
      </ElTableColumn>
      <ElTableColumn label="专业资质权重" width="120">
        <template #default="{ row }">{{ (row.qualificationWeight * 100).toFixed(0) }}%</template>
      </ElTableColumn>
      <ElTableColumn label="学员口碑权重" width="120">
        <template #default="{ row }">{{ (row.ratingWeight * 100).toFixed(0) }}%</template>
      </ElTableColumn>
      <ElTableColumn label="行业擅长权重" width="120">
        <template #default="{ row }">{{ (row.expertiseWeight * 100).toFixed(0) }}%</template>
      </ElTableColumn>
      <ElTableColumn label="授课风格权重" width="120">
        <template #default="{ row }">{{ (row.styleWeight * 100).toFixed(0) }}%</template>
      </ElTableColumn>
      <ElTableColumn label="缺口阈值" width="100">
        <template #default="{ row }">{{ row.gapThreshold }}分</template>
      </ElTableColumn>
      <ElTableColumn prop="createdAt" label="创建时间" width="180" />
      <ElTableColumn label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <ElButton type="primary" size="small" @click="openDialog(row as WeightConfig)">
            <EditIcon class="w-4 h-4" />
          </ElButton>
          <ElButton type="danger" size="small" @click="deleteConfig(row.id)">
            <DeleteIcon class="w-4 h-4" />
          </ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
    
    <ElDialog 
      :title="isEditing ? '编辑权重配置' : '添加权重配置'" 
      v-model="dialogVisible"
      width="600px"
    >
      <ElForm :model="form" label-width="120px">
        <ElFormItem label="配置名称" required>
          <ElInput v-model="form.name" />
        </ElFormItem>
        <ElFormItem label="配置描述">
          <ElInput v-model="form.description" />
        </ElFormItem>
        <div class="bg-gray-50 rounded-lg p-4 mb-4">
          <p class="text-sm font-medium text-gray-700 mb-3">五维度权重配置（总和必须等于1）</p>
          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">授课年限</span>
              <div class="flex items-center gap-3">
                <ElInputNumber v-model="form.teachingYearsWeight" :min="0" :max="1" :step="0.01" style="width: 100px" />
                <span class="text-sm text-gray-500">{{ (form.teachingYearsWeight * 100).toFixed(0) }}%</span>
              </div>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">专业资质</span>
              <div class="flex items-center gap-3">
                <ElInputNumber v-model="form.qualificationWeight" :min="0" :max="1" :step="0.01" style="width: 100px" />
                <span class="text-sm text-gray-500">{{ (form.qualificationWeight * 100).toFixed(0) }}%</span>
              </div>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">学员口碑</span>
              <div class="flex items-center gap-3">
                <ElInputNumber v-model="form.ratingWeight" :min="0" :max="1" :step="0.01" style="width: 100px" />
                <span class="text-sm text-gray-500">{{ (form.ratingWeight * 100).toFixed(0) }}%</span>
              </div>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">行业擅长</span>
              <div class="flex items-center gap-3">
                <ElInputNumber v-model="form.expertiseWeight" :min="0" :max="1" :step="0.01" style="width: 100px" />
                <span class="text-sm text-gray-500">{{ (form.expertiseWeight * 100).toFixed(0) }}%</span>
              </div>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-600">授课风格</span>
              <div class="flex items-center gap-3">
                <ElInputNumber v-model="form.styleWeight" :min="0" :max="1" :step="0.01" style="width: 100px" />
                <span class="text-sm text-gray-500">{{ (form.styleWeight * 100).toFixed(0) }}%</span>
              </div>
            </div>
            <div class="border-t border-gray-200 pt-3 flex items-center justify-between">
              <span class="text-sm font-medium text-gray-700">权重总和</span>
              <span :class="Math.abs(totalWeight - 1) > 0.01 ? 'text-red-500' : 'text-green-600'" class="text-sm font-medium">
                {{ totalWeight.toFixed(2) }}
              </span>
            </div>
          </div>
        </div>
        <ElFormItem label="缺口预警阈值">
          <div class="flex items-center gap-3">
            <ElInputNumber v-model="form.gapThreshold" :min="0" :max="100" style="width: 100px" />
            <span class="text-sm text-gray-500">分（低于此分数判定为师资缺口）</span>
          </div>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="saveConfig">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>