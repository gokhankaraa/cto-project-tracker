import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { api } from './client'
import type { DashboardSummary, OverallStatus, ProjectStatus, RiskLevel } from './types'

/**
 * GET /api/dashboard/summary filtreleri (docs/filtre-sozlesmesi-T13.md, 2. bolum). Bos liste ve null
 * "filtre yok" demektir ve istege yazilmaz. weekNumber yalnizca year ile gecerlidir.
 */
export type DashboardFilters = {
  projectStatus: ProjectStatus[]
  ownerId: number | null
  year: number | null
  weekNumber: number | null
  overallStatus: OverallStatus[]
  riskLevel: RiskLevel[]
  delayed: boolean | null
}

export const NO_DASHBOARD_FILTERS: DashboardFilters = {
  projectStatus: [],
  ownerId: null,
  year: null,
  weekNumber: null,
  overallStatus: [],
  riskLevel: [],
  delayed: null,
}

/**
 * CTO portfoy ozeti. Filtre degisince yeni yanit gelene kadar onceki veri ekranda kalir
 * (keepPreviousData); tablo her secimde yukleniyor gostergesine donup ziplamaz.
 */
export function useDashboardSummary(filters: DashboardFilters) {
  return useQuery({
    queryKey: ['dashboard', 'summary', filters],
    queryFn: ({ signal }) => api.get<DashboardSummary>('/api/dashboard/summary', filters, signal),
    placeholderData: keepPreviousData,
  })
}
