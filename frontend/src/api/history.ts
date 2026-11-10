import { useInfiniteQuery } from '@tanstack/react-query'

import { api } from './client'
import { projectKeys } from './projects'
import type { AuditEntityType, AuditEvent, PageResponse } from './types'

export const HISTORY_PAGE_SIZE = 20

/**
 * Anahtar projenin altinda: proje guncellenince (projectKeys.all gecersiz kilinir) gecmis de yenilenir. Takip
 * kaydi degisikliklerinde ayrica gecersiz kilinir (useInvalidateTrackingItems); rapor ve is kalemi baska
 * sayfalarda degisir, proje sayfasi yeniden acilinca gecmis zaten tazelenir (staleTime 0).
 */
export const historyKeys = {
  ofProject: (projectId: number) => [...projectKeys.detail(projectId), 'history'] as const,
  filtered: (projectId: number, entityType: AuditEntityType | null) =>
    [...historyKeys.ofProject(projectId), entityType] as const,
}

/**
 * GET /api/projects/{id}/history (filtre sozlesmesi, bolum 4b): en yeni ustte, sayfa sayfa. "Daha fazla goster"
 * bir sonraki sayfayi ekler; tur degisince liste bastan yuklenir.
 */
export function useProjectHistory(projectId: number, entityType: AuditEntityType | null) {
  return useInfiniteQuery({
    queryKey: historyKeys.filtered(projectId, entityType),
    queryFn: ({ pageParam, signal }) =>
      api.get<PageResponse<AuditEvent>>(
        `/api/projects/${projectId}/history`,
        { entityType, page: pageParam, size: HISTORY_PAGE_SIZE },
        signal,
      ),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  })
}
