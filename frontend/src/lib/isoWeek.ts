/**
 * ISO 8601 haftasi: haftalar pazartesi baslar; yilin ilk haftasi, o yilin ilk persembesini iceren haftadir.
 * Bu yuzden yil sinirinda takvim yilindan farkli olabilir: 2027-01-01 (cuma) 2026'nin 53. haftasidir.
 *
 * Raporun yili, anlattigi haftanin yilidir (backend: WeeklyReport.yearOf(week, reportDate) ve
 * WeeklyReportService.validateReportWeek); {@link checkReportWeek} ayni kurali ve mesajlari uygular.
 */
export interface IsoWeek {
  year: number
  week: number
}

const DAY_MS = 24 * 60 * 60 * 1000

/** "2026-08-03" -> { year: 2026, week: 32 }; gecersiz tarihte null. Hesap UTC'de yapilir, saat dilimi etkilemez. */
export function isoWeekOf(isoDate: string): IsoWeek | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate)
  if (!match) return null
  const [year, month, day] = [Number(match[1]), Number(match[2]), Number(match[3])]
  const date = new Date(Date.UTC(year, month - 1, day))
  // 2026-02-30 gibi tasan tarihleri reddet (Date bunlari sessizce mart ayina kaydirir).
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return null

  // Haftanin persembesi hangi yildaysa hafta o yila aittir.
  const weekday = date.getUTCDay() || 7 // pazartesi 1 ... pazar 7
  const thursday = new Date(date.getTime() + (4 - weekday) * DAY_MS)
  const weekYear = thursday.getUTCFullYear()
  const firstDay = Date.UTC(weekYear, 0, 1)
  const week = Math.floor((thursday.getTime() - firstDay) / DAY_MS / 7) + 1
  return { year: weekYear, week }
}

/** Bugunun tarihi, kullanicinin saat diliminde "YYYY-MM-DD". */
export function todayIso(now: Date = new Date()): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

const fromIso = (isoDate: string) => {
  const [year, month, day] = isoDate.split('-').map(Number)
  return Date.UTC(year, month - 1, day)
}

const toIso = (time: number) => new Date(time).toISOString().slice(0, 10)

/** Tarihe gun ekler ("2026-11-09", -7 -> "2026-11-02"). */
export function addDays(isoDate: string, days: number): string {
  return toIso(fromIso(isoDate) + days * DAY_MS)
}

/** ISO yilinda bu hafta var mi: 1-52 her yilda, 53 yalnizca 53 haftalik yillarda (28 Aralik'in haftasi). */
export function hasWeek(year: number, week: number): boolean {
  const weeksInYear = isoWeekOf(`${year}-12-28`)!.week
  return Number.isInteger(week) && week >= 1 && week <= weeksInYear
}

/** ISO yilinin verilen haftasinin pazartesisi (2026, 31 -> "2026-07-27"). */
export function mondayOf(year: number, week: number): string {
  const jan4 = fromIso(`${year}-01-04`)
  const weekday = new Date(jan4).getUTCDay() || 7
  return toIso(jan4 - (weekday - 1) * DAY_MS + (week - 1) * 7 * DAY_MS)
}

/**
 * Raporun anlattigi haftanin yili: hafta numarasinin rapor tarihine en yakin oldugu yil (tarihin ISO yili,
 * bir oncesi ve bir sonrasi arasindan; o yilda bu hafta yoksa aday degildir). Hicbir adayda yoksa tarihin yili.
 */
export function reportYearOf(week: number, reportDate: string): number | null {
  const dateWeek = isoWeekOf(reportDate)
  if (!dateWeek) return null
  const date = fromIso(reportDate)
  const candidates = [dateWeek.year, dateWeek.year - 1, dateWeek.year + 1].filter((year) => hasWeek(year, week))
  if (candidates.length === 0) return dateWeek.year
  const distance = (year: number) => Math.abs(fromIso(mondayOf(year, week)) - date)
  return candidates.reduce((best, year) => (distance(year) < distance(best) ? year : best))
}

/** Rapor, anlattigi haftanin pazartesisinden itibaren 14 gun icinde (o hafta ya da ertesi hafta) yazilir. */
export const REPORT_WINDOW_DAYS = 14

export type ReportWeekCheck =
  | { ok: true; year: number; monday: string }
  | { ok: false; field: 'weekNumber' | 'reportDate'; message: string }

const formatTr = (isoDate: string) => isoDate.split('-').reverse().join('.')

/** Hafta ile rapor tarihi birbirine uyuyor mu; uyuyorsa anlatilan haftanin yili, uymuyorsa alan ve mesaj. */
export function checkReportWeek(week: number, reportDate: string): ReportWeekCheck | null {
  const year = reportYearOf(week, reportDate)
  if (year === null) return null
  if (!hasWeek(year, week)) {
    return { ok: false, field: 'weekNumber', message: `Rapor tarihinin yılında ve komşu yıllarda ${week}. hafta yok.` }
  }
  const monday = mondayOf(year, week)
  if (reportDate < monday) {
    return {
      ok: false,
      field: 'reportDate',
      message: `Rapor tarihi, anlattığı haftadan (${year} / ${week}. hafta, ${formatTr(monday)}) önce olamaz.`,
    }
  }
  const lastDay = addDays(monday, REPORT_WINDOW_DAYS - 1)
  if (reportDate > lastDay) {
    return {
      ok: false,
      field: 'reportDate',
      message: `Rapor en geç anlattığı haftanın ertesi haftasında yazılabilir (${year} / ${week}. hafta için son gün ${formatTr(lastDay)}).`,
    }
  }
  return { ok: true, year, monday }
}
