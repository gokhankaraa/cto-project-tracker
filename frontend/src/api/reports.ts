import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { api } from './client'
import { projectKeys } from './projects'
import {
  PROGRESS_STAGES,
  type OverallStatus,
  type PageResponse,
  type ProgressStage,
  type RiskLevel,
  type WeeklyReport,
} from './types'

/**
 * POST /api/projects/{projectId}/reports ve PUT /api/reports/{id} govdesi (dto/WeeklyReportRequest.java).
 * Yil gonderilmez; backend anlatilan haftanin yilini hafta ve rapor tarihinden turetir (lib/isoWeek.ts,
 * reportYearOf). Rapor tarihi haftanin icinde ya da ertesi haftasinda olmalidir. Ilerleme yuzdesi kademeden gelir.
 */
export interface WeeklyReportRequest {
  weekNumber: number
  reportDate: string
  progressStage: ProgressStage
  overallStatus: OverallStatus
  riskLevel: RiskLevel
  done: string | null
  planned: string | null
  risks: string | null
  note: string | null
}

/** Backend'deki @Size sinirlari (dort metin alani da 2000). */
export const REPORT_TEXT_LIMIT = 2000

/** ProgressStage.java'daki yuzdeler. */
export const STAGE_PERCENTAGES: Record<ProgressStage, number> = {
  ANALIZ: 25,
  GELISTIRME: 50,
  TEST: 75,
  TAMAMLANDI: 100,
}

/**
 * Guncellemede secilebilecek kademeler (backend: WeeklyReportService.validateStageTransition):
 * mevcut kademe ya da bir sonraki. Geri donmek ve sira atlamak reddedilir.
 */
export function allowedStages(current: ProgressStage): ProgressStage[] {
  const index = PROGRESS_STAGES.indexOf(current)
  return PROGRESS_STAGES.slice(index, index + 2)
}

/**
 * GET /api/reports filtreleri (docs/filtre-sozlesmesi.md, 3. bolum). year raporun yilina esitliktir;
 * weekFrom/weekTo yalnizca year ile ve o yilin icinde gecerlidir; yillar arasi aralik icin tarih kullanilir.
 */
export type ReportFilters = {
  projectId: number | null
  ownerId: number | null
  year: number | null
  weekFrom: number | null
  weekTo: number | null
  dateFrom: string | null
  dateTo: string | null
  overallStatus: OverallStatus[]
  riskLevel: RiskLevel[]
  progressStage: ProgressStage[]
}

export const NO_REPORT_FILTERS: ReportFilters = {
  projectId: null,
  ownerId: null,
  year: null,
  weekFrom: null,
  weekTo: null,
  dateFrom: null,
  dateTo: null,
  overallStatus: [],
  riskLevel: [],
  progressStage: [],
}

/** Backend'in kabul ettigi siralamalar; ikinci anahtar (id) backend'de eklenir. */
export const REPORT_SORTS = ['reportDate,desc', 'reportDate,asc', 'weekNumber,desc', 'projectName,asc'] as const
export type ReportSort = (typeof REPORT_SORTS)[number]

export const REPORT_PAGE_SIZE = 20

export const reportKeys = {
  all: ['reports'] as const,
  detail: (id: number) => ['reports', id] as const,
  search: (filters: ReportFilters, page: number, sort: ReportSort) => ['reports', 'search', filters, page, sort] as const,
}

/** Projeler arasi rapor listesi. page 0'dan baslar (backend). */
export function useReportSearch(filters: ReportFilters, page: number, sort: ReportSort) {
  return useQuery({
    queryKey: reportKeys.search(filters, page, sort),
    queryFn: ({ signal }) =>
      api.get<PageResponse<WeeklyReport>>('/api/reports', { ...filters, page, size: REPORT_PAGE_SIZE, sort }, signal),
    placeholderData: keepPreviousData,
  })
}

export function useReport(id: number) {
  return useQuery({
    queryKey: reportKeys.detail(id),
    queryFn: ({ signal }) => api.get<WeeklyReport>(`/api/reports/${id}`, undefined, signal),
  })
}

/**
 * Rapor degisince projenin rapor gecmisi, rapor sorgulari, dashboard (son durum) ve is kalemleri listesi
 * (her projenin son raporundaki kalemler) eskir.
 */
function useInvalidateReports() {
  const queryClient = useQueryClient()
  return (projectId: number) =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: reportKeys.all }),
      queryClient.invalidateQueries({ queryKey: projectKeys.reports(projectId) }),
      queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      queryClient.invalidateQueries({ queryKey: ['work-items'] }),
    ])
}

export function useCreateReport(projectId: number) {
  const invalidate = useInvalidateReports()
  return useMutation({
    mutationFn: (request: WeeklyReportRequest) =>
      api.post<WeeklyReport>(`/api/projects/${projectId}/reports`, request),
    onSuccess: () => invalidate(projectId),
  })
}

export function useUpdateReport(id: number) {
  const invalidate = useInvalidateReports()
  return useMutation({
    mutationFn: (request: WeeklyReportRequest) => api.put<WeeklyReport>(`/api/reports/${id}`, request),
    onSuccess: (saved) => invalidate(saved.projectId),
  })
}

/** Rapora bagli is kalemleri de silinir (backend'de cascade). */
export function useDeleteReport(report: WeeklyReport) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.delete(`/api/reports/${report.id}`),
    onSuccess: () => {
      // Silinen raporun kendi sorgusuna dokunulmaz (bkz. useDeleteProject): yenilense 404 alirdi.
      void queryClient.invalidateQueries({ queryKey: ['reports', 'search'] })
      void queryClient.invalidateQueries({ queryKey: projectKeys.reports(report.projectId) })
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      void queryClient.invalidateQueries({ queryKey: ['work-items'] })
    },
  })
}
