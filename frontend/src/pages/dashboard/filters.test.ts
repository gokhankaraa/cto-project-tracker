// @vitest-environment node
import { describe, expect, it } from 'vitest'

import { NO_DASHBOARD_FILTERS } from '../../api/dashboard'
import { dashboardSearch, hasActiveFilters, parseDashboardFilters } from './filters'

const parse = (search: string) => parseDashboardFilters(new URLSearchParams(search))

describe('parseDashboardFilters', () => {
  it('adresteki tum filtreleri okur', () => {
    expect(
      parse('projectStatus=DEVAM_EDIYOR&ownerId=1&year=2026&weekNumber=29&overallStatus=RISKLI&riskLevel=ORTA,YUKSEK&delayed=true'),
    ).toEqual({
      projectStatus: ['DEVAM_EDIYOR'],
      ownerId: 1,
      year: 2026,
      weekNumber: 29,
      overallStatus: ['RISKLI'],
      riskLevel: ['ORTA', 'YUKSEK'],
      delayed: true,
    })
  })

  it('yil yoksa haftayi atar (backend yilsiz haftayi 400 ile reddeder)', () => {
    expect(parse('weekNumber=29')).toEqual(NO_DASHBOARD_FILTERS)
  })

  it('gecersiz degerleri atar', () => {
    expect(parse('year=1999&weekNumber=60&riskLevel=FOO&ownerId=abc&delayed=maybe')).toEqual(NO_DASHBOARD_FILTERS)
  })
})

describe('dashboardSearch', () => {
  it('okunan filtreler ayni adrese geri yazilir', () => {
    const search = '?projectStatus=DEVAM_EDIYOR&ownerId=1&year=2026&weekNumber=29&riskLevel=ORTA,YUKSEK&delayed=false'
    expect(dashboardSearch(parse(search))).toBe(search)
  })

  it('filtre yoksa bos adres ve aktif filtre yok', () => {
    expect(dashboardSearch(NO_DASHBOARD_FILTERS)).toBe('')
    expect(hasActiveFilters(NO_DASHBOARD_FILTERS)).toBe(false)
    expect(hasActiveFilters(parse('delayed=false'))).toBe(true)
  })
})
