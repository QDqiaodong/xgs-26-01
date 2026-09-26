<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { courseApi, instructorApi, matchApi, type CourseStats, type InstructorStats } from '../api'
import { 
  Notebook as CourseIcon, 
  User as InstructorIcon, 
  Warning as WarningIcon,
  Clock as HistoryIcon,
  TrendCharts as TrendIcon,
  CirclePlus as MatchIcon,
  Star as RatingIcon,
  User as UsersIcon,
  Clock as ClockIcon
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const courseStats = ref<CourseStats>({ total: 0, active: 0, draft: 0, inactive: 0, industryStats: [] })
const instructorStats = ref<InstructorStats>({ total: 0, active: 0, inactive: 0, available: 0, unavailable: 0, avgTeachingYears: 0, avgRating: 0 })
const gapCount = ref(0)
const recordCount = ref(0)

const pieChartRef = ref<HTMLDivElement>()
const barChartRef = ref<HTMLDivElement>()
const industryChartRef = ref<HTMLDivElement>()

const fetchStats = async () => {
  try {
    const [courseStatsRes, instructorStatsRes, warningsRes, recordsRes] = await Promise.all([
      courseApi.getStats(),
      instructorApi.getStats(),
      matchApi.getGaps(),
      matchApi.getAll(0, 1)
    ])
    courseStats.value = courseStatsRes.data
    instructorStats.value = instructorStatsRes.data
    gapCount.value = warningsRes.data.length
    recordCount.value = recordsRes.data.totalElements || 0
    initCharts()
  } catch (error) {
    console.error('Failed to fetch stats:', error)
  }
}

const initCharts = () => {
  initPieChart()
  initBarChart()
  initIndustryChart()
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  
  const chart = echarts.init(pieChartRef.value)
  chart.setOption({
    title: {
      text: '五维度权重分布',
      left: 'center',
      textStyle: { fontSize: 16 }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}% ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      data: ['授课年限', '专业资质', '学员口碑', '行业擅长', '授课风格']
    },
    series: [{
      name: '权重分布',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}: {c}%'
      },
      data: [
        { value: 20, name: '授课年限', itemStyle: { color: '#5470c6' } },
        { value: 25, name: '专业资质', itemStyle: { color: '#91cc75' } },
        { value: 25, name: '学员口碑', itemStyle: { color: '#fac858' } },
        { value: 15, name: '行业擅长', itemStyle: { color: '#ee6666' } },
        { value: 15, name: '授课风格', itemStyle: { color: '#73c0de' } }
      ]
    }]
  })
}

const initBarChart = () => {
  if (!barChartRef.value) return
  
  const chart = echarts.init(barChartRef.value)
  chart.setOption({
    title: {
      text: '课程状态分布',
      left: 'center',
      textStyle: { fontSize: 16 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['启用', '草稿', '停用']
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      name: '课程数',
      type: 'bar',
      barWidth: '50%',
      data: [
        { value: courseStats.value.active, itemStyle: { color: '#67C23A' } },
        { value: courseStats.value.draft, itemStyle: { color: '#E6A23C' } },
        { value: courseStats.value.inactive, itemStyle: { color: '#909399' } }
      ]
    }]
  })
}

const initIndustryChart = () => {
  if (!industryChartRef.value) return
  
  const chart = echarts.init(industryChartRef.value)
  const data = courseStats.value.industryStats || []
  chart.setOption({
    title: {
      text: '课程行业分布',
      left: 'center',
      textStyle: { fontSize: 16 }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}门 ({d}%)'
    },
    series: [{
      name: '行业分布',
      type: 'pie',
      radius: '60%',
      label: {
        show: true,
        formatter: '{b}: {c}门'
      },
      data: data.map(item => ({
        value: item.count,
        name: item.industry,
        itemStyle: {
          color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de'][data.indexOf(item) % 5]
        }
      }))
    }]
  })
}

onMounted(() => {
  fetchStats()
})
</script>

<template>
  <div class="space-y-6">
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="bg-white rounded-xl shadow-sm p-6 flex items-center gap-4">
        <div class="w-12 h-12 rounded-lg bg-blue-50 flex items-center justify-center">
          <CourseIcon class="w-6 h-6 text-blue-600" />
        </div>
        <div>
          <p class="text-sm text-gray-500">课程总数</p>
          <p class="text-2xl font-bold text-gray-800">{{ courseStats.total }}</p>
        </div>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm p-6 flex items-center gap-4">
        <div class="w-12 h-12 rounded-lg bg-green-50 flex items-center justify-center">
          <InstructorIcon class="w-6 h-6 text-green-600" />
        </div>
        <div>
          <p class="text-sm text-gray-500">讲师总数</p>
          <p class="text-2xl font-bold text-gray-800">{{ instructorStats.total }}</p>
        </div>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm p-6 flex items-center gap-4">
        <div class="w-12 h-12 rounded-lg bg-orange-50 flex items-center justify-center">
          <WarningIcon class="w-6 h-6 text-orange-600" />
        </div>
        <div>
          <p class="text-sm text-gray-500">师资缺口</p>
          <p class="text-2xl font-bold text-gray-800">{{ gapCount }}</p>
        </div>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm p-6 flex items-center gap-4">
        <div class="w-12 h-12 rounded-lg bg-purple-50 flex items-center justify-center">
          <HistoryIcon class="w-6 h-6 text-purple-600" />
        </div>
        <div>
          <p class="text-sm text-gray-500">推演记录</p>
          <p class="text-2xl font-bold text-gray-800">{{ recordCount }}</p>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="bg-white rounded-xl shadow-sm p-4">
        <div class="flex items-center justify-between">
          <span class="text-sm text-gray-500">可用讲师</span>
          <UsersIcon class="w-5 h-5 text-green-500" />
        </div>
        <p class="text-xl font-bold text-green-600 mt-2">{{ instructorStats.available }}</p>
      </div>
      <div class="bg-white rounded-xl shadow-sm p-4">
        <div class="flex items-center justify-between">
          <span class="text-sm text-gray-500">平均教龄</span>
          <ClockIcon class="w-5 h-5 text-blue-500" />
        </div>
        <p class="text-xl font-bold text-blue-600 mt-2">{{ instructorStats.avgTeachingYears }}年</p>
      </div>
      <div class="bg-white rounded-xl shadow-sm p-4">
        <div class="flex items-center justify-between">
          <span class="text-sm text-gray-500">平均评分</span>
          <RatingIcon class="w-5 h-5 text-yellow-500" />
        </div>
        <p class="text-xl font-bold text-yellow-600 mt-2">{{ Number(instructorStats.avgRating || 0).toFixed(1) }}</p>
      </div>
      <div class="bg-white rounded-xl shadow-sm p-4">
        <div class="flex items-center justify-between">
          <span class="text-sm text-gray-500">启用课程</span>
          <MatchIcon class="w-5 h-5 text-green-500" />
        </div>
        <p class="text-xl font-bold text-green-600 mt-2">{{ courseStats.active }}</p>
      </div>
    </div>
    
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="bg-white rounded-xl shadow-sm p-6">
        <div class="flex items-center gap-2 mb-4">
          <TrendIcon class="w-5 h-5 text-blue-600" />
          <h3 class="font-semibold text-gray-800">权重分布</h3>
        </div>
        <div ref="pieChartRef" class="h-64"></div>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm p-6">
        <div class="flex items-center gap-2 mb-4">
          <CourseIcon class="w-5 h-5 text-green-600" />
          <h3 class="font-semibold text-gray-800">课程状态分布</h3>
        </div>
        <div ref="barChartRef" class="h-64"></div>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm p-6">
        <div class="flex items-center gap-2 mb-4">
          <MatchIcon class="w-5 h-5 text-purple-600" />
          <h3 class="font-semibold text-gray-800">课程行业分布</h3>
        </div>
        <div ref="industryChartRef" class="h-64"></div>
      </div>
    </div>
    
    <div class="bg-white rounded-xl shadow-sm p-6">
      <h3 class="font-semibold text-gray-800 mb-4">系统功能简介</h3>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="flex items-start gap-3 p-4 bg-blue-50 rounded-lg">
          <div class="w-8 h-8 rounded bg-blue-100 flex items-center justify-center flex-shrink-0">
            <span class="text-blue-600 font-bold text-sm">1</span>
          </div>
          <div>
            <h4 class="font-medium text-gray-800">课程需求档案配置</h4>
            <p class="text-sm text-gray-500">行业、难度、必备资质、风格要求等多维度课程档案管理</p>
          </div>
        </div>
        <div class="flex items-start gap-3 p-4 bg-green-50 rounded-lg">
          <div class="w-8 h-8 rounded bg-green-100 flex items-center justify-center flex-shrink-0">
            <span class="text-green-600 font-bold text-sm">2</span>
          </div>
          <div>
            <h4 class="font-medium text-gray-800">讲师综合资质档案录入</h4>
            <p class="text-sm text-gray-500">证书、教龄、学员评分、擅长领域等全方位讲师信息管理</p>
          </div>
        </div>
        <div class="flex items-start gap-3 p-4 bg-orange-50 rounded-lg">
          <div class="w-8 h-8 rounded bg-orange-100 flex items-center justify-center flex-shrink-0">
            <span class="text-orange-600 font-bold text-sm">3</span>
          </div>
          <div>
            <h4 class="font-medium text-gray-800">多维度权重智能匹配</h4>
            <p class="text-sm text-gray-500">五维度可自定义权重，自动计算适配分值并降序推荐</p>
          </div>
        </div>
        <div class="flex items-start gap-3 p-4 bg-red-50 rounded-lg">
          <div class="w-8 h-8 rounded bg-red-100 flex items-center justify-center flex-shrink-0">
            <span class="text-red-600 font-bold text-sm">4</span>
          </div>
          <div>
            <h4 class="font-medium text-gray-800">师资缺口风险预警</h4>
            <p class="text-sm text-gray-500">自动识别无适配讲师课程，输出师资缺口预警</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
