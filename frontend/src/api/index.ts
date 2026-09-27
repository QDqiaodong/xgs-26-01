import axios from 'axios'

export const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface Course {
  id: number
  name: string
  industry: string
  difficulty: string
  requiredQualifications: string
  styleRequirements: string
  description: string
  status: string
  totalHours: number
  maxStudents: number
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
}

export interface Instructor {
  id: number
  name: string
  phone: string
  email: string
  certificates: string
  teachingYears: number
  studentRating: number
  expertiseAreas: string
  teachingStyle: string
  resume: string
  status: string
  available: boolean
  maxCoursesPerMonth: number
  currentMonthCourses: number
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
}

export interface WeightConfig {
  id: number
  name: string
  description: string
  teachingYearsWeight: number
  qualificationWeight: number
  ratingWeight: number
  expertiseWeight: number
  styleWeight: number
  gapThreshold: number
  isDefault: boolean
  active: boolean
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
}

export interface MatchRecord {
  id: number
  courseId: number
  courseName: string
  courseIndustry: string
  weightConfigId: number
  teachingYearsWeight: number
  qualificationWeight: number
  ratingWeight: number
  expertiseWeight: number
  styleWeight: number
  hasGap: boolean
  bestScore: number
  matchCount: number
  batchMatch: boolean
  batchIndustry: string
  batchId: string
  operator: string
  algorithmVersion: string
  remark: string
  createdAt: string
}

export interface MatchRecordDetail {
  id: number
  recordId: number
  instructorId: number
  instructorName: string
  totalScore: number
  teachingYearsScore: number
  qualificationScore: number
  ratingScore: number
  expertiseScore: number
  styleScore: number
  rank: number
}

export interface MatchResultResponse {
  recordId: number
  courseName: string
  courseIndustry: string
  hasGap: boolean
  bestScore: number
  matchCount: number
  instructors: {
    instructorId: number
    instructorName: string
    totalScore: number
    teachingYearsScore: number
    qualificationScore: number
    ratingScore: number
    expertiseScore: number
    styleScore: number
    rank: number
    available: boolean
  }[]
}

export interface GapWarningResponse {
  courseId: number
  courseName: string
  industry: string
  difficulty: string
  bestScore: number
  matchCount: number
  lastMatchTime: string
  warningLevel: string
}

// ---------------- 推演方案（评审发布） ----------------

export type PlanStatus =
  | 'DRAFT'
  | 'IN_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'PUBLISHED'
  | 'SUPERSEDED'
  | 'INVALIDATED'

export interface PlanReview {
  id: number
  reviewType: 'INSTRUCTOR' | 'COURSE'
  decision: 'APPROVED' | 'REJECTED'
  comment?: string
  reviewerUsername?: string
  reviewerName?: string
  createdAt: string
}

export interface PlanEvent {
  id: number
  eventType: string
  actorUsername?: string
  actorName?: string
  sourceType?: string
  sourceId?: number
  detail?: string
  createdAt: string
}

export interface PlanCandidate {
  id: number
  instructorId: number
  instructorName: string
  instructorStatus: string
  instructorAvailable: boolean
  teachingYears: number
  studentRating: number
  certificates: string
  expertiseAreas: string
  teachingStyle: string
  skills: string[]
  maxCoursesPerMonth: number
  totalScore: number
  teachingYearsScore: number
  qualificationScore: number
  ratingScore: number
  expertiseScore: number
  styleScore: number
  rankNo: number
  currentStatus?: string
  currentAvailable?: boolean
  currentMaxCoursesPerMonth?: number
  currentUsableForAssignment: boolean
  currentUsableReason?: string
}

export interface Plan {
  id: number
  matchRecordId: number
  courseId: number
  versionNo: number
  name: string
  status: PlanStatus
  courseName: string
  courseIndustry: string
  courseDifficulty: string
  courseRequiredQualifications: string
  courseStyleRequirements: string
  courseStatus: string
  weightConfigId: number
  weightConfigName: string
  teachingYearsWeight: number
  qualificationWeight: number
  ratingWeight: number
  expertiseWeight: number
  styleWeight: number
  gapThreshold: number
  bestScore: number
  candidateCount: number
  instructorReviewPassed: boolean
  courseReviewPassed: boolean
  reviews: PlanReview[]
  invalidSourceType?: string
  invalidSourceId?: number
  invalidReason?: string
  invalidatedAt?: string
  activePublishedPlanId?: number
  currentActivePublished: boolean
  selectedInstructorId?: number
  selectedInstructorName?: string
  assignmentId?: number
  assignedAt?: string
  supersededByPlanId?: number
  createdBy: string
  submittedBy?: string
  submittedAt?: string
  publishedBy?: string
  publishedAt?: string
  remark?: string
  createdAt: string
  updatedAt: string
  candidates: PlanCandidate[]
  events: PlanEvent[]
}

export interface FieldDiff {
  targetType: 'COURSE' | 'INSTRUCTOR' | 'WEIGHT'
  targetId: number
  targetName?: string
  field: string
  fieldLabel: string
  snapshotValue?: string
  currentValue?: string
  changed: boolean
}

export interface PlanSnapshotDiff {
  planId: number
  versionNo: number
  courseDiffs: FieldDiff[]
  weightDiffs: FieldDiff[]
  instructorDiffs: FieldDiff[]
  weightConfigDeleted: boolean
  hasAnyChange: boolean
  assignmentBlocked: boolean
  assignmentBlockReason?: string
}

export interface PlanAssignmentResult {
  planId: number
  versionNo: number
  status: string
  success: boolean
  assignmentId?: number
  instructorId?: number
  instructorName?: string
  candidateRank?: number
  matchScore?: number
  reason?: string
  assignedAt?: string
}

export interface LoginResponse {
  userId: number
  username: string
  realName: string
  instructorId?: number
  token: string
  roles: string[]
}

export interface User {
  id: number
  username: string
  password: string
  realName: string
  phone: string
  email: string
  enabled: boolean
  lastLoginTime: string
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
}

export interface InstructorSkill {
  id: number
  instructorId: number
  skillName: string
  skillType: string
  description: string
  acquiredDate: string
  expiryDate: string
  createdAt: string
}

export interface CourseRequiredSkill {
  id: number
  courseId: number
  skillName: string
  skillType: string
  isMandatory: boolean
  skillLevel: string
  createdAt: string
}

export interface CourseStats {
  total: number
  active: number
  draft: number
  inactive: number
  industryStats: { industry: string; count: number }[]
}

export interface InstructorStats {
  total: number
  active: number
  inactive: number
  available: number
  unavailable: number
  avgTeachingYears: number
  avgRating: number
}

export const courseApi = {
  getAll: (page: number = 0, size: number = 10, industry?: string, status?: string) => 
    api.get<PageResponse<Course>>('/courses', { params: { page, size, industry, status } }),
  getAllList: () => api.get<Course[]>('/courses/all'),
  getById: (id: number) => api.get<Course>(`/courses/${id}`),
  getByIndustry: (industry: string) => api.get<Course[]>(`/courses/industry/${industry}`),
  create: (data: Omit<Course, 'id' | 'createdBy' | 'updatedBy' | 'createdAt' | 'updatedAt'>) => 
    api.post<Course>('/courses', data),
  update: (id: number, data: Partial<Course>) => api.put<Course>(`/courses/${id}`, data),
  updateStatus: (id: number, status: string) => api.patch<Course>(`/courses/${id}/status`, {}, { params: { status } }),
  delete: (id: number) => api.delete(`/courses/${id}`),
  getStats: () => api.get<CourseStats>('/courses/stats')
}

export const instructorApi = {
  getAll: (page: number = 0, size: number = 10, status?: string, available?: boolean) => 
    api.get<PageResponse<Instructor>>('/instructors', { params: { page, size, status, available } }),
  getAllList: () => api.get<Instructor[]>('/instructors/all'),
  getById: (id: number) => api.get<Instructor>(`/instructors/${id}`),
  create: (data: Omit<Instructor, 'id' | 'status' | 'available' | 'maxCoursesPerMonth' | 'currentMonthCourses' | 'createdBy' | 'updatedBy' | 'createdAt' | 'updatedAt'>) => 
    api.post<Instructor>('/instructors', data),
  update: (id: number, data: Partial<Instructor>) => api.put<Instructor>(`/instructors/${id}`, data),
  updateStatus: (id: number, status: string) => api.patch<Instructor>(`/instructors/${id}/status`, {}, { params: { status } }),
  updateAvailability: (id: number, available: boolean) => api.patch<Instructor>(`/instructors/${id}/available`, {}, { params: { available } }),
  delete: (id: number) => api.delete(`/instructors/${id}`),
  getStats: () => api.get<InstructorStats>('/instructors/stats')
}

export const weightApi = {
  getAll: () => api.get<WeightConfig[]>('/weights'),
  getById: (id: number) => api.get<WeightConfig>(`/weights/${id}`),
  getDefault: () => api.get<WeightConfig>('/weights/default'),
  create: (data: Omit<WeightConfig, 'id' | 'isDefault' | 'active' | 'createdBy' | 'updatedBy' | 'createdAt' | 'updatedAt'>) => 
    api.post<WeightConfig>('/weights', data),
  update: (id: number, data: Partial<WeightConfig>) => api.put<WeightConfig>(`/weights/${id}`, data),
  delete: (id: number) => api.delete(`/weights/${id}`)
}

export const matchApi = {
  match: (courseId: number, weightConfigId: number) => 
    api.post<MatchResultResponse>('/matches', { courseId, weightConfigId }),
  batchMatch: (industry: string, weightConfigId: number) => 
    api.post<MatchResultResponse[]>('/matches/batch', { industry, weightConfigId }),
  getAll: (page: number = 0, size: number = 20) => 
    api.get<PageResponse<MatchRecord>>('/matches', { params: { page, size } }),
  getById: (id: number) => api.get<MatchRecord>(`/matches/${id}`),
  getDetails: (id: number) => api.get<MatchRecordDetail[]>(`/matches/${id}/details`),
  getGaps: () => api.get<GapWarningResponse[]>('/matches/gaps'),
  getByCourseId: (courseId: number) => api.get<MatchRecord[]>(`/matches/course/${courseId}`),
  getByIndustry: (industry: string) => api.get<MatchRecord[]>(`/matches/industry/${industry}`),
  delete: (id: number) => api.delete(`/matches/${id}`)
}

export const authApi = {
  login: (username: string, password: string) => 
    api.post<ApiResponse<LoginResponse>>('/auth/login', { username, password }),
  logout: () => api.post('/auth/logout'),
  getAllUsers: () => api.get<User[]>('/auth/users'),
  getUserById: (id: number) => api.get<User>(`/auth/users/${id}`),
  createUser: (data: Omit<User, 'id' | 'enabled' | 'lastLoginTime' | 'createdBy' | 'updatedBy' | 'createdAt' | 'updatedAt'>) => 
    api.post<User>('/auth/users', data),
  updateUser: (id: number, data: Partial<User>) => api.put<User>(`/auth/users/${id}`, data),
  deleteUser: (id: number) => api.delete(`/auth/users/${id}`)
}

export const skillApi = {
  getInstructorSkills: (instructorId: number) => api.get<InstructorSkill[]>(`/skills/instructors/${instructorId}`),
  addInstructorSkill: (instructorId: number, skillName: string, skillType?: string) => 
    api.post<InstructorSkill>(`/skills/instructors/${instructorId}`, { skillName, skillType: skillType || 'CERTIFICATE' }),
  removeInstructorSkill: (instructorId: number, skillId: number) => 
    api.delete(`/skills/instructors/${instructorId}/${skillId}`),
  batchAddInstructorSkills: (instructorId: number, skillNames: string[], skillType?: string) => 
    api.post(`/skills/instructors/${instructorId}/batch`, { skillNames, skillType: skillType || 'CERTIFICATE' }),
  
  getCourseSkills: (courseId: number) => api.get<CourseRequiredSkill[]>(`/skills/courses/${courseId}`),
  addCourseSkill: (courseId: number, skillName: string, skillType?: string, isMandatory?: boolean) => 
    api.post<CourseRequiredSkill>(`/skills/courses/${courseId}`, { skillName, skillType: skillType || 'QUALIFICATION', isMandatory: isMandatory !== undefined ? isMandatory : true }),
  removeCourseSkill: (courseId: number, skillId: number) => 
    api.delete(`/skills/courses/${courseId}/${skillId}`),
  batchAddCourseSkills: (courseId: number, skillNames: string[], skillType?: string, isMandatory?: boolean) => 
    api.post(`/skills/courses/${courseId}/batch`, { skillNames, skillType: skillType || 'QUALIFICATION', isMandatory: isMandatory !== undefined ? isMandatory : true }),
  
  calculateMatchScore: (courseId: number, instructorId: number) =>
    api.get<{ matchScore: number }>(`/skills/match/${courseId}/${instructorId}`)
}

// ---------------- 讲师带教观察与改进周期 ----------------

export interface ObservationItem {
  id: number
  dimension: string
  sensitivityLevel: 'PUBLIC' | 'INSTRUCTOR_RELATED' | 'RESTRICTED'
  fact: string
  grade: string
  advice?: string
  effectiveStatus: string
  revisionNo: number
  masked: boolean
}

export interface ObservationRevision extends ObservationItem {
  itemId: number
  changeType: string
  changedBy?: string
  changeReason?: string
  disputeId?: number
  createdAt: string
}

export interface ObservationDispute {
  id: number
  itemId?: number
  reason: string
  raisedByName: string
  raisedAt: string
  status: string
  reviewerName?: string
  reviewComment?: string
  reviewedAt?: string
}

export interface GoalEvidence {
  id: number
  content: string
  materialUri?: string
  submittedByName?: string
  submittedAt: string
  status: string
  reviewComment?: string
  reviewedByName?: string
  reviewedAt?: string
}

export interface ObservationGoal {
  id: number
  stageNo: number
  title: string
  measurementCriteria: string
  dueDate: string
  required: boolean
  status: string
  activatedAt?: string
  completedAt?: string
  evidence: GoalEvidence[]
}

export interface CycleDecision {
  id: number
  versionNo: number
  decisionType: string
  conclusion: string
  basis: string
  decidedByName: string
  replacesDecisionId?: number
  status: string
  createdAt: string
}

export interface TimelineEvent {
  id: number
  eventType: string
  sourceType?: string
  sourceId?: number
  detail: string
  actorName?: string
  createdAt: string
}

export interface ObservationReminder {
  id: number
  reminderType: string
  assigneeRole: string
  title: string
  message: string
  status: string
  dueAt?: string
  createdAt: string
}

export interface TeachingObservation {
  id: number
  courseId: number
  assignmentId?: number
  courseName: string
  sessionNo: string
  instructorId: number
  instructorName: string
  observerName?: string
  observerUsername?: string
  occurredAt: string
  attachmentSource?: string
  status: string
  statusText: string
  needsImprovement: boolean
  summary?: string
  currentConclusion?: string
  currentConclusionId?: number
  versionLock: number
  viewLevel: 'FULL' | 'INSTRUCTOR' | 'PUBLIC'
  responseDueAt?: string
  followupDueAt?: string
  acknowledgedAt?: string
  responseText?: string
  respondedAt?: string
  closedAt?: string
  items: ObservationItem[]
  revisions: ObservationRevision[]
  addenda: { id: number; sensitivityLevel: string; content: string; authorName: string; createdAt: string; masked: boolean }[]
  disputes: ObservationDispute[]
  goals: ObservationGoal[]
  decisions: CycleDecision[]
  timeline: TimelineEvent[]
  reminders: ObservationReminder[]
}

export interface ObservationSummary {
  id: number
  courseId: number
  courseName: string
  sessionNo: string
  instructorId: number
  instructorName: string
  observerName?: string
  occurredAt: string
  status: string
  statusText: string
  needsImprovement: boolean
  currentConclusion?: string
  pendingReminderCount: number
  versionLock: number
  viewLevel: 'FULL' | 'INSTRUCTOR' | 'PUBLIC'
}

export const observationApi = {
  list: (params?: { courseId?: number; instructorId?: number; status?: string }) =>
    api.get<ApiResponse<ObservationSummary[]>>('/teaching-observations', { params }),
  get: (id: number) => api.get<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}`),
  reminders: () => api.get<ApiResponse<ObservationReminder[]>>('/teaching-observations/reminders'),
  create: (data: any) => api.post<ApiResponse<TeachingObservation>>('/teaching-observations', data),
  addendum: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/addenda`, data),
  acknowledge: (id: number, expectedVersion?: number) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/acknowledge`, { expectedVersion }),
  respond: (id: number, responseText: string, expectedVersion?: number) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/respond`, { responseText, expectedVersion }),
  refuse: (id: number, reason: string, expectedVersion?: number) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/refuse`, { reason, expectedVersion }),
  dispute: (id: number, data: { itemId?: number; reason: string; expectedVersion?: number }) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/disputes`, data),
  reviewDispute: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/disputes/review`, data),
  goals: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/goals`, data),
  confirmGoals: (id: number, expectedVersion?: number) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/goals/confirm`, { expectedVersion }),
  evidence: (goalId: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/goals/${goalId}/evidence`, data),
  reviewEvidence: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/evidence/review`, data),
  close: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/close`, data),
  extend: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/extend`, data),
  requestEscalation: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/escalation-request`, data),
  escalate: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/escalate`, data),
  reopen: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/reopen`, data),
  approveReopen: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/reopen/approve`, data),
  declineReopen: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/reopen/decline`, data),
  reopenedConclusion: (id: number, data: any) =>
    api.post<ApiResponse<TeachingObservation>>(`/teaching-observations/${id}/reopened-conclusion`, data)
}

export const planApi = {
  createFromRecord: (matchRecordId: number, name?: string, remark?: string) =>
    api.post<Plan>('/plans', { matchRecordId, name, remark }),
  updateCandidates: (id: number, instructorIds: number[]) =>
    api.put<Plan>(`/plans/${id}/candidates`, { instructorIds }),
  submit: (id: number) => api.post<Plan>(`/plans/${id}/submit`),
  reviewInstructor: (id: number, decision: 'APPROVED' | 'REJECTED', comment: string) =>
    api.post<Plan>(`/plans/${id}/reviews/instructor`, { decision, comment }),
  reviewCourse: (id: number, decision: 'APPROVED' | 'REJECTED', comment: string) =>
    api.post<Plan>(`/plans/${id}/reviews/course`, { decision, comment }),
  reject: (id: number, reason: string) => api.post<Plan>(`/plans/${id}/reject`, { reason }),
  publish: (id: number) => api.post<Plan>(`/plans/${id}/publish`),
  assign: (id: number) => api.post<PlanAssignmentResult>(`/plans/${id}/assign`),
  discard: (id: number) => api.delete(`/plans/${id}`),
  list: (courseId?: number, status?: string) =>
    api.get<Plan[]>('/plans', { params: { courseId, status } }),
  getById: (id: number) => api.get<Plan>(`/plans/${id}`),
  getDiff: (id: number) => api.get<PlanSnapshotDiff>(`/plans/${id}/diff`)
}
