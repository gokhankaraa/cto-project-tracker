/**
 * ISO 8601 haftasi: haftalar pazartesi baslar; yilin ilk haftasi, o yilin ilk persembesini iceren haftadir.
 * Backend raporun yilini ayni kuralla rapor tarihinden turetir (WeeklyReport.yearOf, IsoFields.WEEK_BASED_YEAR);
 * bu yuzden yil sinirinda takvim yilindan farkli olabilir: 2027-01-01 (cuma) 2026'nin 53. haftasidir.
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
