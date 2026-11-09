/**
 * URL sorgu parametreleri ile ekran filtreleri arasindaki ceviri. Filtreler URL'de tutulur; boylece
 * sayfa yenilenince kaybolmaz, adres paylasilabilir ve tarayicinin geri tusu calisir.
 *
 * Okuma toleranslidir: elle bozulmus bir adres (riskLevel=FOO, year=abc) hata ekrani yerine
 * "o filtre yok" olarak yorumlanir.
 */

/** Virgulle ayrilmis enum listesini okur; tanimsiz degerler atilir, sira enum dizisindeki siradir. */
export function parseEnumList<E extends string>(raw: string | null, allowed: readonly E[]): E[] {
  if (!raw) return []
  const given = new Set(raw.split(',').map((part) => part.trim()))
  return allowed.filter((value) => given.has(value))
}

/** Tam sayiyi okur; sayi degilse ya da [min, max] disindaysa null. */
export function parseIntInRange(raw: string | null, min: number, max: number): number | null {
  if (raw === null || !/^\d+$/.test(raw)) return null
  const value = Number(raw)
  return value >= min && value <= max ? value : null
}

/** Kullaniciya gorunen sayfa numarasi (1'den baslar); gecersizse 1. */
export function parsePage(raw: string | null): number {
  return parseIntInRange(raw, 1, Number.MAX_SAFE_INTEGER) ?? 1
}

/** Izin verilen siralamalardan biri; degilse varsayilan. */
export function parseSort<S extends string>(raw: string | null, allowed: readonly S[], fallback: S): S {
  return allowed.find((value) => value === raw) ?? fallback
}

/** "YYYY-MM-DD" biciminde gercek bir tarih mi (2026-02-30 degil). */
export function parseIsoDate(raw: string | null): string | null {
  if (raw === null || !/^\d{4}-\d{2}-\d{2}$/.test(raw)) return null
  const [year, month, day] = raw.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day ? raw : null
}

/** "true" / "false" disindaki her deger null (filtre yok). */
export function parseBoolean(raw: string | null): boolean | null {
  if (raw === 'true') return true
  if (raw === 'false') return false
  return null
}

type FilterValue = string | number | boolean | null | undefined | readonly string[]

const encode = (value: string) => encodeURIComponent(value).replaceAll('%2C', ',')

/**
 * Filtre nesnesini "?a=1&b=X,Y" bicimine cevirir. Bos degerler yazilmaz. Virgul kodlanmadan birakilir
 * (riskLevel=ORTA,YUKSEK, sort=title,asc): sorgu metninde gecerli bir karakterdir, adres okunur kalir ve
 * backend'in bicimiyle aynidir.
 */
export function toSearchString(filters: Record<string, FilterValue>): string {
  const parts: string[] = []
  for (const [key, value] of Object.entries(filters)) {
    if (value === null || value === undefined || value === '') continue
    if (Array.isArray(value)) {
      if (value.length > 0) parts.push(`${key}=${value.map(encode).join(',')}`)
    } else {
      parts.push(`${key}=${encode(String(value))}`)
    }
  }
  return parts.length > 0 ? `?${parts.join('&')}` : ''
}
