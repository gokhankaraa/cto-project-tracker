// @vitest-environment node
import { describe, expect, it } from 'vitest'

import { isoWeekOf, todayIso } from './isoWeek'

describe('isoWeekOf', () => {
  it.each([
    // Beklenen degerler Python'un date.isocalendar() sonucuyla karsilastirildi.
    ['2026-07-20', 2026, 30],
    ['2026-08-03', 2026, 32],
    // Yil sinirlari: hafta, persembesinin oldugu yila aittir.
    ['2027-01-01', 2026, 53],
    ['2024-12-30', 2025, 1],
    ['2026-01-01', 2026, 1],
    ['2021-01-03', 2020, 53],
  ])('%s -> %i / %i. hafta', (date, year, week) => {
    expect(isoWeekOf(date)).toEqual({ year, week })
  })

  it.each(['', '2026-02-30', '2026-13-01', '20.07.2026'])('gecersiz tarih "%s" -> null', (date) => {
    expect(isoWeekOf(date)).toBeNull()
  })
})

describe('todayIso', () => {
  it('yerel tarihi YYYY-MM-DD yazar', () => {
    expect(todayIso(new Date(2026, 0, 5, 23, 30))).toBe('2026-01-05')
  })
})
