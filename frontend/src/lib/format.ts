/**
 * Turkce arama icin metni sadelestirir: kucuk harf, Turkce ve aksanli harfler duz karsiliklarina
 * (I/ı/İ -> i, ş -> s, ğ -> g, ü -> u, ö -> o, ç -> c). "ik" yazan kullanici "Atlas IK"yi de,
 * "çağrı" yazan "Cagri"yi de bulur; klavye duzeni ya da buyuk/kucuk harf sonucu degistirmez.
 */
export function foldText(text: string): string {
  return text
    .toLocaleLowerCase('tr')
    .replaceAll('ı', 'i')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
}

/** Aranan metin, verilen alanlardan birinde geciyor mu (bos arama her seyi eslestirir). */
export function matchesSearch(query: string, ...fields: (string | null | undefined)[]): boolean {
  const needle = foldText(query.trim())
  return needle === '' || fields.some((field) => field != null && foldText(field).includes(needle))
}

/**
 * Backend'in ISO tarihini ("2026-07-20") "20.07.2026" olarak gosterir. Date nesnesine cevrilmez:
 * new Date('2026-07-20') UTC gece yarisidir ve UTC'nin batisindaki saat dilimlerinde bir onceki gune duser.
 */
export function formatDate(isoDate: string | null | undefined): string {
  if (!isoDate) return '–'
  const [year, month, day] = isoDate.split('-')
  return `${day}.${month}.${year}`
}

/** "2026 / 31. hafta" */
export function formatWeek(year: number | null, weekNumber: number | null): string {
  return year === null || weekNumber === null ? '–' : `${year} / ${weekNumber}. hafta`
}

/** Adres parametresindeki id'yi okur (/projects/:id); pozitif tam sayi degilse null. */
export function parseId(raw: string | undefined): number | null {
  if (raw === undefined || !/^\d+$/.test(raw)) return null
  const id = Number(raw)
  return id >= 1 && Number.isSafeInteger(id) ? id : null
}
