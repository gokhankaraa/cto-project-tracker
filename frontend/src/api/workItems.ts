import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { api } from './client'
import { projectKeys } from './projects'
import { reportKeys } from './reports'
import type { PageResponse, WorkItem, WorkItemStatus } from './types'

/** POST /api/reports/{reportId}/work-items ve PUT /api/work-items/{id} govdesi (dto/WorkItemRequest.java). */
export interface WorkItemRequest {
  title: string
  description: string | null
  assignee: string | null
  status: WorkItemStatus
  plannedDate: string | null
  completedDate: string | null
}

/** Backend'deki @Size sinirlari. */
export const WORK_ITEM_LIMITS = { title: 255, description: 2000, assignee: 255 } as const

/**
 * GET /api/work-items filtreleri (docs/filtre-sozlesmesi.md, 4. bolum). Varsayilan olarak her
 * projenin degerlendirilen (en son) raporundaki is kalemleri listelenir; year/weekNumber gecmis bir nokta secer.
 */
export type WorkItemFilters = {
  projectId: number | null
  year: number | null
  weekNumber: number | null
  assignee: string | null
  status: WorkItemStatus[]
  delayed: boolean | null
}

export const NO_WORK_ITEM_FILTERS: WorkItemFilters = {
  projectId: null,
  year: null,
  weekNumber: null,
  assignee: null,
  status: [],
  delayed: null,
}

/** Backend'in kabul ettigi siralamalar; ikinci anahtar (id) backend'de eklenir. */
export const WORK_ITEM_SORTS = ['plannedDate,asc', 'plannedDate,desc', 'title,asc', 'projectName,asc'] as const
export type WorkItemSort = (typeof WORK_ITEM_SORTS)[number]

export const WORK_ITEM_PAGE_SIZE = 20

export const workItemKeys = {
  all: ['work-items'] as const,
  search: (filters: WorkItemFilters, page: number, sort: WorkItemSort) => ['work-items', filters, page, sort] as const,
  ofReport: (reportId: number) => [...reportKeys.detail(reportId), 'work-items'] as const,
}

/** Projeler arasi liste. page 0'dan baslar (backend). Sayfa/filtre degisirken onceki sayfa ekranda kalir. */
export function useWorkItemSearch(filters: WorkItemFilters, page: number, sort: WorkItemSort) {
  return useQuery({
    queryKey: workItemKeys.search(filters, page, sort),
    queryFn: ({ signal }) =>
      api.get<PageResponse<WorkItem>>(
        '/api/work-items',
        { ...filters, page, size: WORK_ITEM_PAGE_SIZE, sort },
        signal,
      ),
    placeholderData: keepPreviousData,
  })
}

/** Bir raporun is kalemleri. */
export function useReportWorkItems(reportId: number) {
  return useQuery({
    queryKey: workItemKeys.ofReport(reportId),
    queryFn: ({ signal }) => api.get<WorkItem[]>(`/api/reports/${reportId}/work-items`, undefined, signal),
  })
}

/**
 * Is kalemi degisince eskiyenler: raporun kendisi ve is kalemleri (canli task sayisi), projenin rapor
 * gecmisi ve rapor listesi (canli task sutunu), dashboard (gecikme, canli task) ve projeler arasi liste.
 */
function useInvalidateWorkItems(reportId: number, projectId: number) {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: reportKeys.detail(reportId) }),
      queryClient.invalidateQueries({ queryKey: projectKeys.reports(projectId) }),
      queryClient.invalidateQueries({ queryKey: ['reports', 'search'] }),
      queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      queryClient.invalidateQueries({ queryKey: workItemKeys.all }),
    ])
}

export function useCreateWorkItem(reportId: number, projectId: number) {
  const invalidate = useInvalidateWorkItems(reportId, projectId)
  return useMutation({
    mutationFn: (request: WorkItemRequest) => api.post<WorkItem>(`/api/reports/${reportId}/work-items`, request),
    onSuccess: invalidate,
  })
}

export function useUpdateWorkItem(reportId: number, projectId: number) {
  const invalidate = useInvalidateWorkItems(reportId, projectId)
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: WorkItemRequest }) =>
      api.put<WorkItem>(`/api/work-items/${id}`, request),
    onSuccess: invalidate,
  })
}

export function useDeleteWorkItem(reportId: number, projectId: number) {
  const invalidate = useInvalidateWorkItems(reportId, projectId)
  return useMutation({
    mutationFn: (id: number) => api.delete(`/api/work-items/${id}`),
    onSuccess: invalidate,
  })
}
