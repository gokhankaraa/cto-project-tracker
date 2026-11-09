// @vitest-environment node
import { describe, expect, it } from 'vitest'

import { addDays, checkReportWeek, hasWeek, isoWeekOf, mondayOf, reportYearOf, todayIso } from './isoWeek'

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

describe('hasWeek / mondayOf / addDays', () => {
  it.each([
    [2026, true],
    [2020, true],
    [2025, false],
    [2027, false],
  ])('%i yilinda 53. hafta: %s', (year, exists) => {
    expect(hasWeek(year, 53)).toBe(exists)
    expect(hasWeek(year, 52)).toBe(true)
  })

  it('haftanin pazartesisi ve gun ekleme', () => {
    expect(mondayOf(2026, 31)).toBe('2026-07-27')
    expect(mondayOf(2026, 53)).toBe('2026-12-28')
    expect(mondayOf(2025, 1)).toBe('2024-12-30')
    expect(addDays('2026-11-09', -7)).toBe('2026-11-02')
    expect(addDays('2026-12-28', 13)).toBe('2027-01-10')
  })
})

describe('reportYearOf (backend WeeklyReportYearTest ile ayni tablo)', () => {
  it.each([
    [31, '2026-08-03', 2026],
    [30, '2026-07-27', 2026],
    [53, '2026-12-31', 2026],
    [53, '2027-01-04', 2026],
    [52, '2027-01-04', 2026],
    [1, '2027-01-04', 2027],
    [1, '2024-12-30', 2025],
    [52, '2026-01-05', 2025],
  ])('hafta %i, tarih %s -> %i', (week, date, year) => {
    expect(reportYearOf(week, date)).toBe(year)
  })
})

describe('checkReportWeek (backend ile ayni kural ve mesajlar)', () => {
  it('rapor haftanin icinde ya da ertesi haftasinda yazilabilir', () => {
    expect(checkReportWeek(31, '2026-07-27')).toEqual({ ok: true, year: 2026, monday: '2026-07-27' })
    expect(checkReportWeek(31, '2026-08-09')).toEqual({ ok: true, year: 2026, monday: '2026-07-27' })
    expect(checkReportWeek(53, '2027-01-04')).toEqual({ ok: true, year: 2026, monday: '2026-12-28' })
  })

  it('haftadan once yazilan rapor', () => {
    expect(checkReportWeek(35, '2026-07-27')).toEqual({
      ok: false,
      field: 'reportDate',
      message: 'Rapor tarihi, anlattığı haftadan (2026 / 35. hafta, 24.08.2026) önce olamaz.',
    })
  })

  it('ertesi haftadan sonra yazilan rapor', () => {
    expect(checkReportWeek(29, '2026-07-27')).toEqual({
      ok: false,
      field: 'reportDate',
      message: 'Rapor en geç anlattığı haftanın ertesi haftasında yazılabilir (2026 / 29. hafta için son gün 26.07.2026).',
    })
  })

  it('53 haftalik olmayan yillarda 53. hafta', () => {
    expect(checkReportWeek(53, '2023-06-05')).toEqual({
      ok: false,
      field: 'weekNumber',
      message: 'Rapor tarihinin yılında ve komşu yıllarda 53. hafta yok.',
    })
  })
})
