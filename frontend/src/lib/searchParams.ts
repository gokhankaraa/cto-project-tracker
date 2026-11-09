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
