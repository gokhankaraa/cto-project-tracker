// @vitest-environment node
import { describe, expect, it } from 'vitest'

import { NO_WORK_ITEM_FILTERS } from '../../api/workItems'
import { parseWorkItemList, workItemListSearch } from './filters'

const parse = (search: string) => parseWorkItemList(new URLSearchParams(search))

describe('parseWorkItemList', () => {
  it('filtreleri, sayfayi ve siralamayi okur', () => {
    expect(parse('projectId=1&year=2026&weekNumber=29&assignee=mehmet&status=DEVAM_EDIYOR,GECIKTI&delayed=true&page=3&sort=title,asc')).toEqual({
      filters: {
        projectId: 1,
        year: 2026,
        weekNumber: 29,
        assignee: 'mehmet',
        status: ['DEVAM_EDIYOR', 'GECIKTI'],
        delayed: true,
      },
      page: 3,
      sort: 'title,asc',
    })
  })

  it('bos adreste varsayilanlar: 1. sayfa, planlanan tarihe gore', () => {
    expect(parse('')).toEqual({ filters: NO_WORK_ITEM_FILTERS, page: 1, sort: 'plannedDate,asc' })
  })

  it('gecersiz degerleri atar', () => {
    expect(parse('weekNumber=29&status=FOO&page=0&sort=id,asc&assignee=%20%20')).toEqual({
      filters: NO_WORK_ITEM_FILTERS,
      page: 1,
      sort: 'plannedDate,asc',
    })
  })
})

describe('workItemListSearch', () => {
  it('varsayilanlari adrese yazmaz', () => {
    expect(workItemListSearch({ filters: NO_WORK_ITEM_FILTERS, page: 1, sort: 'plannedDate,asc' })).toBe('')
  })

  it('okunan durum ayni adrese geri yazilir', () => {
    const search = '?projectId=1&year=2026&status=GECIKTI&page=2&sort=projectName,asc'
    expect(workItemListSearch(parse(search))).toBe(search)
  })
})
