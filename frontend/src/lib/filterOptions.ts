/** Filtre cubuklarinin ortak secenekleri (dashboard, is kalemleri, raporlar). */

export const WEEK_OPTIONS = Array.from({ length: 53 }, (_, i) => ({ value: String(i + 1), label: `${i + 1}. hafta` }))

export const DELAYED_OPTIONS = [
  { value: 'true', label: 'Yalnızca gecikenler' },
  { value: 'false', label: 'Gecikmeyenler' },
]

/** Son bes yil; adreste daha eski bir yil varsa o da listeye eklenir ki secili gorunsun. */
export function yearOptions(selected: number | null, now: Date = new Date()): string[] {
  const current = now.getFullYear()
  const years = Array.from({ length: 5 }, (_, i) => current - i)
  if (selected !== null && !years.includes(selected)) years.push(selected)
  return years.sort((a, b) => b - a).map(String)
}

/** Select degeri (metin ya da null) -> sayi ya da null. */
export const toNumber = (value: string | null) => (value === null ? null : Number(value))

/** Boolean filtre -> Select degeri ve geri. */
export const booleanToValue = (value: boolean | null) => (value === null ? null : String(value))
export const valueToBoolean = (value: string | null) => (value === null ? null : value === 'true')
