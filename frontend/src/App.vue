<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Menu as MenuIcon,
  Notebook as CourseIcon,
  User as InstructorIcon,
  Setting as WeightIcon,
  MagicStick as MatchIcon,
  Warning as WarningIcon,
  Clock as HistoryIcon,
  Files as PlanIcon,
  Monitor as HomeIcon,
  Document as LogIcon,
  UserFilled as UserIcon,
  Monitor as LogoutIcon
} from '@element-plus/icons-vue'
import { ElDropdown, ElDropdownMenu, ElDropdownItem, ElMessage } from 'element-plus'
import { authApi } from './api'
import Login from './views/Login.vue'
import Home from './views/Home.vue'
import Courses from './views/Courses.vue'
import CourseDetail from './views/CourseDetail.vue'
import Instructors from './views/Instructors.vue'
import InstructorDetail from './views/InstructorDetail.vue'
import Weights from './views/Weights.vue'
import Match from './views/Match.vue'
import MatchResult from './views/MatchResult.vue'
import Warnings from './views/Warnings.vue'
import History from './views/History.vue'
import Logs from './views/Logs.vue'
import Users from './views/Users.vue'
import Plans from './views/Plans.vue'
import PlanDetail from './views/PlanDetail.vue'

const router = useRouter()
const currentRoute = useRoute()
const collapsed = ref(false)
const isLoggedIn = ref(!!localStorage.getItem('token'))
const userInfo = ref(JSON.parse(localStorage.getItem('user') || '{}'))

const routes = [
  { name: '首页', path: '/', icon: HomeIcon },
  { name: '课程管理', path: '/courses', icon: CourseIcon },
  { name: '讲师管理', path: '/instructors', icon: InstructorIcon },
  { name: '权重配置', path: '/weights', icon: WeightIcon },
  { name: '智能匹配', path: '/match', icon: MatchIcon },
  { name: '缺口预警', path: '/warnings', icon: WarningIcon },
  { name: '历史记录', path: '/history', icon: HistoryIcon },
  { name: '方案评审发布', path: '/plans', icon: PlanIcon },
  { name: '用户管理', path: '/users', icon: UserIcon },
  { name: '操作日志', path: '/logs', icon: LogIcon }
]

const routeKey = computed(() => currentRoute.fullPath)
const activePath = computed(() => currentRoute.path)
const currentView = computed(() => {
  const path = currentRoute.path
  if (path === '/login') return Login
  if (path === '/courses') return Courses
  if (path.startsWith('/courses/')) return CourseDetail
  if (path === '/instructors') return Instructors
  if (path.startsWith('/instructors/')) return InstructorDetail
  if (path === '/weights') return Weights
  if (path === '/match') return Match
  if (path.startsWith('/match/result/')) return MatchResult
  if (path === '/warnings') return Warnings
  if (path === '/history') return History
  if (path === '/plans') return Plans
  if (path.startsWith('/plans/')) return PlanDetail
  if (path === '/users') return Users
  if (path === '/logs') return Logs
  return Home
})

const handleLogout = async () => {
  try {
    await authApi.logout()
  } catch (error) {
    console.log('Logout failed')
  }
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  isLoggedIn.value = false
  ElMessage.success('退出成功')
  router.push('/login')
}

const handleMenuClick = (path: string) => {
  if (currentRoute.path !== path) {
    window.location.href = path
  }
}
</script>

<template>
  <div class="flex h-screen bg-gray-100">
    <aside 
      :class="[
        'bg-blue-600 text-white transition-all duration-300 flex flex-col',
        collapsed ? 'w-20' : 'w-60'
      ]"
    >
      <div class="flex items-center justify-between p-4 border-b border-blue-500">
        <div v-if="!collapsed" class="text-lg font-bold">课程匹配系统</div>
        <div v-else class="w-8 h-8 bg-blue-500 rounded-lg flex items-center justify-center">
          <span class="text-xs font-bold">CM</span>
        </div>
        <button 
          @click="collapsed = !collapsed"
          class="p-2 hover:bg-blue-500 rounded-lg transition-colors"
        >
          <MenuIcon class="w-5 h-5" />
        </button>
      </div>
      
      <nav class="flex-1 p-4 space-y-2">
        <button
          v-for="route in routes"
          :key="route.path"
          @click="handleMenuClick(route.path)"
          class="w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors hover:bg-blue-500"
          :class="{ 'bg-blue-500': activePath === route.path }"
        >
          <component :is="route.icon" class="w-5 h-5 flex-shrink-0" />
          <span v-if="!collapsed" class="text-sm">{{ route.name }}</span>
        </button>
      </nav>
      
      <div v-if="!collapsed" class="p-4 border-t border-blue-500">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
            <UserIcon class="w-4 h-4" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium truncate">{{ userInfo.realName || userInfo.username }}</p>
            <p class="text-xs text-blue-200 truncate">{{ userInfo.username }}</p>
          </div>
          <ElDropdown>
            <button class="p-2 hover:bg-blue-500 rounded-lg transition-colors">
              <LogoutIcon class="w-4 h-4" />
            </button>
            <template #dropdown>
              <ElDropdownMenu>
                <ElDropdownItem @click="handleLogout">退出登录</ElDropdownItem>
              </ElDropdownMenu>
            </template>
          </ElDropdown>
        </div>
      </div>
    </aside>
    
    <main class="flex-1 overflow-auto">
      <div class="p-6">
        <component :is="currentView" :key="routeKey" />
      </div>
    </main>
  </div>
</template>
