import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { api } from './client'
import { historyKeys } from './history'
import { projectKeys } from './projects'
import type {
  PageResponse,
  RiskLevel,
  TrackingItem,
  TrackingItemStatus,
  TrackingItemType,
} from './types'

/** POST /api/projects/{id}/tracking-items ve PUT /api/tracking-items/{id} govdesi (dto/TrackingItemRequest.java). */
export interface TrackingItemRequest {
  type: TrackingItemType
  title: string
  description: string | null
  riskLevel: RiskLevel | null
  impact: string | null
  actionPlan: string | null
  assignee: string | null
  followUpDate: string | null
  status: TrackingItemStatus
}

/** Backend'deki @Size sinirlari. */
export const TRACKING_ITEM_LIMITS = {
  title: 255,
  description: 2000,
  impact: 2000,
  actionPlan: 2000,
  assignee: 255,
} as const

/** Risk seviyesi yalnizca risk ve engel kayitlarinda vardir; backend digerlerinde reddeder. */
export const hasRiskLevel = (type: TrackingItemType) => type === 'RISK' || type === 'ENGEL'

/** GET /api/tracking-items filtreleri (docs/filtre-sozlesmesi.md, bolum 4a). Bos liste "filtre yok" demektir. */
export type TrackingItemFilters = {
  projectId: number | null
  type: TrackingItemType[]
  status: TrackingItemStatus[]
  riskLevel: RiskLevel[]
  overdue: boolean | null
  assignee: string | null
}

/** Backend'in kabul ettigi siralamalar; ikinci anahtar (id) backend'de eklenir. */
export const TRACKING_ITEM_SORTS = ['followUpDate,asc', 'createdAt,desc', 'title,asc', 'projectName,asc'] as const
export type TrackingItemSort = (typeof TRACKING_ITEM_SORTS)[number]

export const TRACKING_ITEM_PAGE_SIZE = 20

export const trackingItemKeys = {
  all: ['tracking-items'] as const,
  search: (filters: TrackingItemFilters, page: number, sort: TrackingItemSort) =>
    ['tracking-items', 'search', filters, page, sort] as const,
  ofProject: (projectId: number) => [...projectKeys.detail(projectId), 'tracking-items'] as const,
}

/** Projeler arasi liste. page 0'dan baslar (backend). Sayfa/filtre degisirken onceki sayfa ekranda kalir. */
export function useTrackingItemSearch(filters: TrackingItemFilters, page: number, sort: TrackingItemSort) {
  return useQuery({
    queryKey: trackingItemKeys.search(filters, page, sort),
    queryFn: ({ signal }) =>
      api.get<PageResponse<TrackingItem>>(
        '/api/tracking-items',
        { ...filters, page, size: TRACKING_ITEM_PAGE_SIZE, sort },
        signal,
      ),
    placeholderData: keepPreviousData,
  })
}

/** Projenin kayitlari: kapanmamislar takip tarihine gore ustte, kapalilar altta (siralama backend'de). */
export function useProjectTrackingItems(projectId: number) {
  return useQuery({
    queryKey: trackingItemKeys.ofProject(projectId),
    queryFn: ({ signal }) =>
      api.get<TrackingItem[]>(`/api/projects/${projectId}/tracking-items`, undefined, signal),
  })
}

/**
 * Kayit degisince eskiyenler: projenin listesi, projeler arasi liste, dashboard (takip sayaclari) ve ayni
 * sayfadaki degisiklik gecmisi.
 */
function useInvalidateTrackingItems(projectId: number) {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: trackingItemKeys.ofProject(projectId) }),
      queryClient.invalidateQueries({ queryKey: historyKeys.ofProject(projectId) }),
      queryClient.invalidateQueries({ queryKey: trackingItemKeys.all }),
      queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
    ])
}

export function useCreateTrackingItem(projectId: number) {
  const invalidate = useInvalidateTrackingItems(projectId)
  return useMutation({
    mutationFn: (request: TrackingItemRequest) =>
      api.post<TrackingItem>(`/api/projects/${projectId}/tracking-items`, request),
    onSuccess: invalidate,
  })
}

export function useUpdateTrackingItem(projectId: number) {
  const invalidate = useInvalidateTrackingItems(projectId)
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: TrackingItemRequest }) =>
      api.put<TrackingItem>(`/api/tracking-items/${id}`, request),
    onSuccess: invalidate,
  })
}

export function useDeleteTrackingItem(projectId: number) {
  const invalidate = useInvalidateTrackingItems(projectId)
  return useMutation({
    mutationFn: (id: number) => api.delete(`/api/tracking-items/${id}`),
    onSuccess: invalidate,
  })
}
