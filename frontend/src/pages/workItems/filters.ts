import { WORK_ITEM_STATUSES } from '../../api/types'
import { WORK_ITEM_SORTS, type WorkItemFilters, type WorkItemSort } from '../../api/workItems'
import {
  parseBoolean,
  parseEnumList,
  parseIntInRange,
  parsePage,
  parseSort,
  toSearchString,
} from '../../lib/searchParams'

export const DEFAULT_WORK_ITEM_SORT: WorkItemSort = 'plannedDate,asc'

/** Listenin adreste tutulan durumu. page kullaniciya gorunen sayfadir (1'den baslar); backend'e page-1 gider. */
export interface WorkItemListState {
  filters: WorkItemFilters
  page: number
  sort: WorkItemSort
}

/**
 * /work-items adresini okur. Gecersiz degerler atilir (bkz. dashboard/filters.ts); sorumlu aramasi
 * backend siniri olan 255 karakterle kesilir.
 */
export function parseWorkItemList(params: URLSearchParams): WorkItemListState {
  const year = parseIntInRange(params.get('year'), 2000, 2100)
  const assignee = params.get('assignee')?.trim().slice(0, 255) ?? ''
  return {
    filters: {
      projectId: parseIntInRange(params.get('projectId'), 1, Number.MAX_SAFE_INTEGER),
      year,
      weekNumber: year === null ? null : parseIntInRange(params.get('weekNumber'), 1, 53),
      assignee: assignee === '' ? null : assignee,
      status: parseEnumList(params.get('status'), WORK_ITEM_STATUSES),
      delayed: parseBoolean(params.get('delayed')),
    },
    page: parsePage(params.get('page')),
    sort: parseSort(params.get('sort'), WORK_ITEM_SORTS, DEFAULT_WORK_ITEM_SORT),
  }
}

/** Durumu adrese yazar; varsayilanlar (1. sayfa, varsayilan siralama) adrese yazilmaz. */
export function workItemListSearch({ filters, page, sort }: WorkItemListState): string {
  return toSearchString({
    ...filters,
    page: page > 1 ? page : null,
    sort: sort === DEFAULT_WORK_ITEM_SORT ? null : sort,
  })
}
