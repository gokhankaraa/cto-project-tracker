import type { Project, User, WeeklyReport, WorkItem } from '../api/types'

/** Ekran testlerinin ortak verisi (seed verisinin kisaltilmisi). */
export const USERS: User[] = [
  { id: 1, fullName: 'Ayse Yilmaz', role: 'PROJECT_MANAGER' },
  { id: 2, fullName: 'Mehmet Kaya', role: 'PROJECT_MANAGER' },
]

export const ATLAS: Project = {
  id: 1,
  name: 'Atlas IK',
  customer: 'Demo Musteri A.S.',
  description: 'Insan kaynaklari surecleri urunu',
  status: 'DEVAM_EDIYOR',
  ownerId: 1,
  ownerFullName: 'Ayse Yilmaz',
}

export const PROJECTS: Project[] = [
  ATLAS,
  { ...ATLAS, id: 2, name: 'e-Belge', description: null, ownerId: 2, ownerFullName: 'Mehmet Kaya' },
  { ...ATLAS, id: 3, name: 'Nova POS', customer: null, status: 'PLANLANDI' },
]

export function report(id: number, weekNumber: number, overrides: Partial<WeeklyReport> = {}): WeeklyReport {
  return {
    id,
    projectId: 1,
    projectName: 'Atlas IK',
    year: 2026,
    weekNumber,
    reportDate: '2026-07-20',
    progressStage: 'GELISTIRME',
    progressPercentage: 50,
    liveTaskCount: 1,
    overallStatus: 'YOLUNDA',
    riskLevel: 'ORTA',
    done: null,
    planned: null,
    risks: null,
    note: null,
    ...overrides,
  }
}

export function workItem(id: number, overrides: Partial<WorkItem> = {}): WorkItem {
  return {
    id,
    weeklyReportId: 4,
    projectId: 1,
    projectName: 'Atlas IK',
    year: 2026,
    weekNumber: 31,
    title: `Is kalemi ${id}`,
    description: null,
    assignee: 'Mehmet Kaya',
    status: 'DEVAM_EDIYOR',
    plannedDate: '2026-07-30',
    completedDate: null,
    delayed: false,
    ...overrides,
  }
}
