import { describe, expect, it } from 'vitest'

import { NO_TRACKING_FILTERS, parseTrackingList, toApiFilters, trackingListSearch } from './filters'

const parse = (search: string) => parseTrackingList(new URLSearchParams(search))

describe('risk ve aksiyon listesi adres durumu', () => {
  it('bos adres: kapanmamis kayitlar, 1. sayfa, takip tarihine gore', () => {
    expect(parse('')).toEqual({ filters: NO_TRACKING_FILTERS, page: 1, sort: 'followUpDate,asc' })
  })

  it('tum filtreleri okur, gecersiz degerleri atar', () => {
    const state = parse('projectId=2&type=RISK,SORUN,ENGEL&riskLevel=YUKSEK,COK&overdue=true&assignee=%20zeynep%20&scope=closed&page=3&sort=title,asc')
    expect(state).toEqual({
      filters: {
        projectId: 2,
        type: ['RISK', 'ENGEL'],
        riskLevel: ['YUKSEK'],
        overdue: true,
        assignee: 'zeynep',
        scope: 'closed',
      },
      page: 3,
      sort: 'title,asc',
    })
    expect(parse('scope=hepsi&overdue=evet&sort=assignee').filters.scope).toBe('open')
  })

  it('varsayilanlar adrese yazilmaz; durum adres uzerinden aynen geri okunur', () => {
    expect(trackingListSearch({ filters: NO_TRACKING_FILTERS, page: 1, sort: 'followUpDate,asc' })).toBe('')

    const state = parse('type=KARAR&scope=all&overdue=false&page=2&sort=createdAt,desc')
    expect(parse(trackingListSearch(state))).toEqual(state)
  })

  it('kapsam backend durum filtresine donusur: acik iki durum, kapali tek durum, tumu filtresiz', () => {
    expect(toApiFilters(NO_TRACKING_FILTERS).status).toEqual(['ACIK', 'DEVAM_EDIYOR'])
    expect(toApiFilters({ ...NO_TRACKING_FILTERS, scope: 'closed' }).status).toEqual(['KAPANDI'])
    expect(toApiFilters({ ...NO_TRACKING_FILTERS, scope: 'all' }).status).toEqual([])
    expect(toApiFilters(NO_TRACKING_FILTERS)).not.toHaveProperty('scope')
  })
})
