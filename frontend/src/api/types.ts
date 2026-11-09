/**
 * Backend yanit tipleri. Alan adlari backend'deki DTO'larla birebir aynidir
 * (backend/src/main/java/com/kolaysoft/ctotracker/dto ve entity).
 */

// --- Enum'lar: degerler backend'deki Java enum'larinin adlariyla ayni ve ayni sirada ---

export const PROJECT_STATUSES = ['PLANLANDI', 'DEVAM_EDIYOR', 'TAMAMLANDI', 'RISKLI', 'BLOKE'] as const
export type ProjectStatus = (typeof PROJECT_STATUSES)[number]

export const OVERALL_STATUSES = ['YOLUNDA', 'RISKLI', 'GECIKMELI', 'BLOKE'] as const
export type OverallStatus = (typeof OVERALL_STATUSES)[number]

export const RISK_LEVELS = ['DUSUK', 'ORTA', 'YUKSEK'] as const
export type RiskLevel = (typeof RISK_LEVELS)[number]

/** Siralidir: kademe her guncellemede en fazla bir adim ilerleyebilir. */
export const PROGRESS_STAGES = ['ANALIZ', 'GELISTIRME', 'TEST', 'TAMAMLANDI'] as const
export type ProgressStage = (typeof PROGRESS_STAGES)[number]

export const WORK_ITEM_STATUSES = [
  'PLANLANDI',
  'DEVAM_EDIYOR',
  'TESTTE',
  'TAMAMLANDI',
  'GECIKTI',
  'RISKLI',
  'BLOKE',
] as const
export type WorkItemStatus = (typeof WORK_ITEM_STATUSES)[number]

export const ROLES = ['PROJECT_MANAGER', 'CTO', 'ADMIN'] as const
export type Role = (typeof ROLES)[number]

// --- Hata ve ortak yapilar ---

/** Dogrulama hatasindaki tek bir alan (ApiError.fieldErrors). */
export interface FieldError {
  field: string
  message: string
}

/**
 * Backend'in tum hatalarda dondurdugu ortak govde (common/error/ApiError.java).
 * fieldErrors yalnizca VALIDATION_ERROR'da bulunur.
 */
export interface ApiError {
  timestamp: string
  status: number
  code: string
  message: string
  path: string
  fieldErrors?: FieldError[]
}

/** Sayfali liste yaniti (dto/PageResponse.java). page 0'dan baslar. */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// --- Kaynaklar ---

/** GET /api/health */
export interface HealthResponse {
  status: string
  application: string
  databaseStatus: string
  database: string
  timestamp: string
}

/** GET /api/users */
export interface User {
  id: number
  fullName: string
  role: Role
}

/** dto/ProjectResponse.java */
export interface Project {
  id: number
  name: string
  customer: string | null
  description: string | null
  status: ProjectStatus
  ownerId: number | null
  ownerFullName: string | null
}

/** dto/WeeklyReportResponse.java; year istekte gonderilmez, anlatilan haftanin yilidir (hafta + rapor tarihinden). */
export interface WeeklyReport {
  id: number
  projectId: number
  projectName: string
  year: number
  weekNumber: number
  reportDate: string
  progressStage: ProgressStage
  progressPercentage: number
  liveTaskCount: number
  overallStatus: OverallStatus
  riskLevel: RiskLevel
  done: string | null
  planned: string | null
  risks: string | null
  note: string | null
}

/** dto/WorkItemResponse.java */
export interface WorkItem {
  id: number
  weeklyReportId: number
  projectId: number
  projectName: string
  year: number
  weekNumber: number
  title: string
  description: string | null
  assignee: string | null
  status: WorkItemStatus
  plannedDate: string | null
  completedDate: string | null
  delayed: boolean
}

/** dto/DashboardProjectSummary.java; rapor yoksa rapora ait alanlar null. */
export interface DashboardProjectSummary {
  projectId: number
  name: string
  customer: string | null
  ownerFullName: string | null
  projectStatus: ProjectStatus
  hasReport: boolean
  lastYear: number | null
  lastWeekNumber: number | null
  progressStage: ProgressStage | null
  progressPercentage: number
  overallStatus: OverallStatus | null
  riskLevel: RiskLevel | null
  liveTaskCount: number
  delayed: boolean
}

/** GET /api/dashboard/summary */
export interface DashboardSummary {
  totalProjects: number
  highRiskProjects: number
  projectsWithoutReport: number
  totalLiveTasks: number
  delayedProjects: number
  projects: DashboardProjectSummary[]
}
