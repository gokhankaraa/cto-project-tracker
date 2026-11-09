// @vitest-environment node
import { describe, expect, it } from 'vitest'

import { NO_REPORT_FILTERS } from '../../api/reports'
import { parseReportList, reportListSearch } from './listFilters'

const parse = (search: string) => parseReportList(new URLSearchParams(search))

describe('parseReportList', () => {
  it('tum filtreleri, sayfayi ve siralamayi okur', () => {
    expect(
      parse(
        'projectId=1&ownerId=2&year=2026&weekFrom=29&weekTo=30&dateFrom=2026-07-01&dateTo=2026-08-31' +
          '&overallStatus=RISKLI&riskLevel=ORTA,YUKSEK&progressStage=TEST&page=2&sort=projectName,asc',
      ),
    ).toEqual({
      filters: {
        projectId: 1,
        ownerId: 2,
        year: 2026,
        weekFrom: 29,
        weekTo: 30,
        dateFrom: '2026-07-01',
        dateTo: '2026-08-31',
        overallStatus: ['RISKLI'],
        riskLevel: ['ORTA', 'YUKSEK'],
        progressStage: ['TEST'],
      },
      page: 2,
      sort: 'projectName,asc',
    })
  })

  it('bos adreste varsayilanlar: en yeni rapor ustte, 1. sayfa', () => {
    expect(parse('')).toEqual({ filters: NO_REPORT_FILTERS, page: 1, sort: 'reportDate,desc' })
  })

  it('yilsiz hafta araligini atar', () => {
    expect(parse('weekFrom=29&weekTo=30').filters).toEqual(NO_REPORT_FILTERS)
  })

  it('ters araliklarda bitisi atar (backend 400 dondururdu)', () => {
    const { filters } = parse('year=2026&weekFrom=30&weekTo=29&dateFrom=2026-08-01&dateTo=2026-07-01')
    expect(filters).toMatchObject({ weekFrom: 30, weekTo: null, dateFrom: '2026-08-01', dateTo: null })
  })

  it('gecersiz tarih, enum ve siralamayi atar', () => {
    expect(parse('dateFrom=2026-02-30&dateTo=yarin&riskLevel=FOO&sort=id,asc')).toEqual({
      filters: NO_REPORT_FILTERS,
      page: 1,
      sort: 'reportDate,desc',
    })
  })
})

describe('reportListSearch', () => {
  it('varsayilanlari yazmaz; okunan durum ayni adrese geri yazilir', () => {
    expect(reportListSearch({ filters: NO_REPORT_FILTERS, page: 1, sort: 'reportDate,desc' })).toBe('')
    const search = '?ownerId=2&year=2026&weekFrom=29&riskLevel=YUKSEK&page=3&sort=weekNumber,desc'
    expect(reportListSearch(parse(search))).toBe(search)
  })
})
