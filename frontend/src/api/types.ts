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

/** Risk seviyesi yalnizca RISK ve ENGEL kayitlarinda bulunur (bkz. hasRiskLevel). */
export const TRACKING_ITEM_TYPES = ['RISK', 'ENGEL', 'KARAR', 'AKSIYON'] as const
export type TrackingItemType = (typeof TRACKING_ITEM_TYPES)[number]

/** KAPANDI disindaki durumlar "acik" sayilir: sorumlu ve takip tarihi zorunludur, gecikebilir. */
export const TRACKING_ITEM_STATUSES = ['ACIK', 'DEVAM_EDIYOR', 'KAPANDI'] as const
export type TrackingItemStatus = (typeof TRACKING_ITEM_STATUSES)[number]

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

/** dto/LoginResponse.java */
export interface LoginResponse {
  /** Sonraki isteklerde 'Authorization: Bearer <token>' basliginda gonderilir. */
  token: string
  /** Token'in gecerlilik sonu (ISO-8601, UTC). */
  expiresAt: string
  user: User
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

/** dto/TrackingItemResponse.java: projenin risk, engel, karar ya da aksiyon kaydi. */
export interface TrackingItem {
  id: number
  projectId: number
  projectName: string
  type: TrackingItemType
  title: string
  description: string | null
  riskLevel: RiskLevel | null
  impact: string | null
  actionPlan: string | null
  assignee: string | null
  followUpDate: string | null
  status: TrackingItemStatus
  closedDate: string | null
  createdAt: string
  /** Kapanmamis ve takip tarihi bugunden once. */
  overdue: boolean
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
  /** Kapanmamis risk, engel, karar ve aksiyon kaydi; hafta referansindan bagimsiz, bugunku durum. */
  openTrackingItems: number
  overdueTrackingItems: number
  /** Kapanmamis risk ve engellerin en yuksek seviyesi; yoksa null. */
  highestOpenRiskLevel: RiskLevel | null
}

/** dto/TrendWeek.java: portfoyun bir haftadaki hali (her proje o haftaya kadarki en son raporuyla). */
export interface TrendWeek {
  year: number
  weekNumber: number
  /** Haftanin pazartesisi (ISO tarih). */
  weekStart: string
  totalProjects: number
  projectsWithReport: number
  projectsByStatus: Record<OverallStatus, number>
  highRiskProjects: number
  delayedProjects: number
  /** Raporlu proje yoksa null. */
  averageProgress: number | null
  /** Hafta sonunda acik kayitlar (olusturulma ve kapanis tarihinden). */
  openRisks: number
  openBlockers: number
  pendingDecisions: number
  openActions: number
  openedTrackingItems: number
  closedTrackingItems: number
}

/** GET /api/dashboard/trend */
export interface DashboardTrend {
  /** Eskiden yeniye. */
  weeks: TrendWeek[]
}

/** GET /api/dashboard/summary */
export interface DashboardSummary {
  totalProjects: number
  highRiskProjects: number
  projectsWithoutReport: number
  totalLiveTasks: number
  delayedProjects: number
  /** Takip kayitlari sayaclari: listelenen projelerden, bugunku durum. */
  openRisks: number
  openBlockers: number
  pendingDecisions: number
  overdueTrackingItems: number
  projects: DashboardProjectSummary[]
}
