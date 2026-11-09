import { NO_DASHBOARD_FILTERS, type DashboardFilters } from '../../api/dashboard'
import { OVERALL_STATUSES, PROJECT_STATUSES, RISK_LEVELS } from '../../api/types'
import { parseBoolean, parseEnumList, parseIntInRange, toSearchString } from '../../lib/searchParams'

/**
 * Dashboard filtrelerini adresten okur. Backend'in reddedecegi degerler (tanimsiz enum, aralik disi
 * yil, yilsiz hafta) burada atilir; elle yazilmis bir adres 400 hatasi yerine filtresiz sonuc gosterir.
 */
export function parseDashboardFilters(params: URLSearchParams): DashboardFilters {
  const year = parseIntInRange(params.get('year'), 2000, 2100)
  return {
    projectStatus: parseEnumList(params.get('projectStatus'), PROJECT_STATUSES),
    ownerId: parseIntInRange(params.get('ownerId'), 1, Number.MAX_SAFE_INTEGER),
    year,
    // Backend: hafta yalnizca yil ile verilebilir.
    weekNumber: year === null ? null : parseIntInRange(params.get('weekNumber'), 1, 53),
    overallStatus: parseEnumList(params.get('overallStatus'), OVERALL_STATUSES),
    riskLevel: parseEnumList(params.get('riskLevel'), RISK_LEVELS),
    delayed: parseBoolean(params.get('delayed')),
  }
}

/** Filtreleri adrese yazilacak "?..." metnine cevirir; filtre yoksa bos metin. */
export function dashboardSearch(filters: DashboardFilters): string {
  return toSearchString(filters)
}

/** En az bir filtre secili mi ("Filtreleri temizle" dugmesi icin). */
export function hasActiveFilters(filters: DashboardFilters): boolean {
  return dashboardSearch(filters) !== dashboardSearch(NO_DASHBOARD_FILTERS)
}
