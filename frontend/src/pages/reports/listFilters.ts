import { REPORT_SORTS, type ReportFilters, type ReportSort } from '../../api/reports'
import { OVERALL_STATUSES, PROGRESS_STAGES, RISK_LEVELS } from '../../api/types'
import {
  parseEnumList,
  parseIntInRange,
  parseIsoDate,
  parsePage,
  parseSort,
  toSearchString,
} from '../../lib/searchParams'

export const DEFAULT_REPORT_SORT: ReportSort = 'reportDate,desc'

/** /reports adresinde tutulan durum; page 1'den baslar, backend'e page-1 gider. */
export interface ReportListState {
  filters: ReportFilters
  page: number
  sort: ReportSort
}

const id = (raw: string | null) => parseIntInRange(raw, 1, Number.MAX_SAFE_INTEGER)

/**
 * /reports adresini okur. Backend'in 400 dondurecegi her sey atilir: tanimsiz enum, aralik disi sayi,
 * gecersiz tarih, yilsiz hafta ve ters aralik (weekFrom > weekTo ya da dateFrom > dateTo ise bitis atilir).
 */
export function parseReportList(params: URLSearchParams): ReportListState {
  const year = parseIntInRange(params.get('year'), 2000, 2100)
  const weekFrom = year === null ? null : parseIntInRange(params.get('weekFrom'), 1, 53)
  const weekTo = year === null ? null : parseIntInRange(params.get('weekTo'), 1, 53)
  const dateFrom = parseIsoDate(params.get('dateFrom'))
  const dateTo = parseIsoDate(params.get('dateTo'))
  return {
    filters: {
      projectId: id(params.get('projectId')),
      ownerId: id(params.get('ownerId')),
      year,
      weekFrom,
      weekTo: weekFrom !== null && weekTo !== null && weekTo < weekFrom ? null : weekTo,
      dateFrom,
      // ISO tarihler metin olarak karsilastirilabilir ("2026-07-20" < "2026-08-03").
      dateTo: dateFrom !== null && dateTo !== null && dateTo < dateFrom ? null : dateTo,
      overallStatus: parseEnumList(params.get('overallStatus'), OVERALL_STATUSES),
      riskLevel: parseEnumList(params.get('riskLevel'), RISK_LEVELS),
      progressStage: parseEnumList(params.get('progressStage'), PROGRESS_STAGES),
    },
    page: parsePage(params.get('page')),
    sort: parseSort(params.get('sort'), REPORT_SORTS, DEFAULT_REPORT_SORT),
  }
}

/** Durumu adrese yazar; varsayilanlar (1. sayfa, en yeni rapor ustte) yazilmaz. */
export function reportListSearch({ filters, page, sort }: ReportListState): string {
  return toSearchString({
    ...filters,
    page: page > 1 ? page : null,
    sort: sort === DEFAULT_REPORT_SORT ? null : sort,
  })
}
