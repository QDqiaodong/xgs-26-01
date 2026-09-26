import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Home from '../views/Home.vue'
import Courses from '../views/Courses.vue'
import CourseDetail from '../views/CourseDetail.vue'
import Instructors from '../views/Instructors.vue'
import InstructorDetail from '../views/InstructorDetail.vue'
import Weights from '../views/Weights.vue'
import Match from '../views/Match.vue'
import MatchResult from '../views/MatchResult.vue'
import Warnings from '../views/Warnings.vue'
import History from '../views/History.vue'
import Logs from '../views/Logs.vue'
import Users from '../views/Users.vue'
import Plans from '../views/Plans.vue'
import PlanDetail from '../views/PlanDetail.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true }
  },
  {
    path: '/courses',
    name: 'Courses',
    component: Courses,
    meta: { requiresAuth: true }
  },
  {
    path: '/courses/:id',
    name: 'CourseDetail',
    component: CourseDetail,
    meta: { requiresAuth: true }
  },
  {
    path: '/instructors',
    name: 'Instructors',
    component: Instructors,
    meta: { requiresAuth: true }
  },
  {
    path: '/instructors/:id',
    name: 'InstructorDetail',
    component: InstructorDetail,
    meta: { requiresAuth: true }
  },
  {
    path: '/weights',
    name: 'Weights',
    component: Weights,
    meta: { requiresAuth: true }
  },
  {
    path: '/match',
    name: 'Match',
    component: Match,
    meta: { requiresAuth: true }
  },
  {
    path: '/match/result/:id',
    name: 'MatchResult',
    component: MatchResult,
    meta: { requiresAuth: true }
  },
  {
    path: '/warnings',
    name: 'Warnings',
    component: Warnings,
    meta: { requiresAuth: true }
  },
  {
    path: '/history',
    name: 'History',
    component: History,
    meta: { requiresAuth: true }
  },
  {
    path: '/plans',
    name: 'Plans',
    component: Plans,
    meta: { requiresAuth: true }
  },
  {
    path: '/plans/:id',
    name: 'PlanDetail',
    component: PlanDetail,
    meta: { requiresAuth: true }
  },
  {
    path: '/logs',
    name: 'Logs',
    component: Logs,
    meta: { requiresAuth: true }
  },
  {
    path: '/users',
    name: 'Users',
    component: Users,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const isAuthenticated = localStorage.getItem('token')
  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else if (to.path === '/login' && isAuthenticated) {
    next('/')
  } else {
    next()
  }
})

export default router
