import {
  TRACKING_ITEM_SORTS,
  type TrackingItemFilters,
  type TrackingItemSort,
} from '../../api/trackingItems'
import { RISK_LEVELS, TRACKING_ITEM_TYPES, type RiskLevel, type TrackingItemType } from '../../api/types'
import {
  parseBoolean,
  parseEnumList,
  parseIntInRange,
  parsePage,
  parseSort,
  toSearchString,
} from '../../lib/searchParams'

/**
 * Hangi kayitlar: kapanmamislar (varsayilan), kapananlar ya da hepsi. Takip tarihine gore siralamada aylar
 * once kapanmis bir kayit listenin basina cikardi; asil takip edilmesi gerekenler acik kayitlardir.
 */
export const TRACKING_SCOPES = ['open', 'closed', 'all'] as const
export type TrackingScope = (typeof TRACKING_SCOPES)[number]

export interface TrackingListFilters {
  projectId: number | null
  type: TrackingItemType[]
  riskLevel: RiskLevel[]
  overdue: boolean | null
  assignee: string | null
  scope: TrackingScope
}

export const NO_TRACKING_FILTERS: TrackingListFilters = {
  projectId: null,
  type: [],
  riskLevel: [],
  overdue: null,
  assignee: null,
  scope: 'open',
}

export const DEFAULT_TRACKING_SORT: TrackingItemSort = 'followUpDate,asc'

/** Listenin adreste tutulan durumu. page kullaniciya gorunen sayfadir (1'den baslar); backend'e page-1 gider. */
export interface TrackingListState {
  filters: TrackingListFilters
  page: number
  sort: TrackingItemSort
}

/** Ekrandaki filtreleri backend parametrelerine cevirir: kapsam durum listesine donusur. */
export function toApiFilters({ scope, ...rest }: TrackingListFilters): TrackingItemFilters {
  const status: TrackingItemFilters['status'] =
    scope === 'open' ? ['ACIK', 'DEVAM_EDIYOR'] : scope === 'closed' ? ['KAPANDI'] : []
  return { ...rest, status }
}

/** /tracking-items adresini okur. Gecersiz degerler atilir; sorumlu aramasi 255 karakterle kesilir. */
export function parseTrackingList(params: URLSearchParams): TrackingListState {
  const assignee = params.get('assignee')?.trim().slice(0, 255) ?? ''
  const scope = params.get('scope')
  return {
    filters: {
      projectId: parseIntInRange(params.get('projectId'), 1, Number.MAX_SAFE_INTEGER),
      type: parseEnumList(params.get('type'), TRACKING_ITEM_TYPES),
      riskLevel: parseEnumList(params.get('riskLevel'), RISK_LEVELS),
      overdue: parseBoolean(params.get('overdue')),
      assignee: assignee === '' ? null : assignee,
      scope: scope === 'closed' || scope === 'all' ? scope : 'open',
    },
    page: parsePage(params.get('page')),
    sort: parseSort(params.get('sort'), TRACKING_ITEM_SORTS, DEFAULT_TRACKING_SORT),
  }
}

/** Durumu adrese yazar; varsayilanlar (acik kayitlar, 1. sayfa, varsayilan siralama) adrese yazilmaz. */
export function trackingListSearch({ filters, page, sort }: TrackingListState): string {
  return toSearchString({
    ...filters,
    scope: filters.scope === 'open' ? null : filters.scope,
    page: page > 1 ? page : null,
    sort: sort === DEFAULT_TRACKING_SORT ? null : sort,
  })
}
