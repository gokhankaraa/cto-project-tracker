import { endSession, getSession } from '../auth/session'
import type { ApiError, FieldError } from './types'

/**
 * Backend adresi; .env.local'daki VITE_API_BASE_URL ile degistirilir (bkz. .env.example). Bos ise
 * istekler sayfanin geldigi adrese gider (Docker'da nginx /api'yi backend'e iletir).
 */
const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/+$/, '')

/** Sunucuya hic ulasilamadiginda (backend kapali, CORS reddi, ag hatasi) kullanilan kod. */
export const NETWORK_ERROR = 'NETWORK_ERROR'

type ParamValue = string | number | boolean | null | undefined | readonly (string | number)[]

/** Sorgu parametreleri; null, undefined, bos metin ve bos liste "filtre yok" sayilip gonderilmez. */
export type QueryParams = Record<string, ParamValue>

/**
 * Backend'den donen hata. Backend'in ApiError govdesindeki kod, mesaj ve alan hatalarini tasir;
 * ekranlar hata turunu {@link code} ile ayirt eder, formlar {@link fieldErrorMap} ile alanlara esler.
 */
export class ApiRequestError extends Error {
  /** HTTP durum kodu; sunucuya ulasilamadiysa 0. */
  readonly status: number
  /** Backend hata kodu (VALIDATION_ERROR, RESOURCE_NOT_FOUND...) ya da NETWORK_ERROR. */
  readonly code: string
  readonly fieldErrors: FieldError[]

  constructor(status: number, code: string, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }

  /** Alan hatalarini { alan: mesaj } bicimine cevirir; ayni alanda birden cok hata varsa ilki kalir. */
  fieldErrorMap(): Record<string, string> {
    const map: Record<string, string> = {}
    for (const { field, message } of this.fieldErrors) {
      map[field] ??= message
    }
    return map
  }
}

/** Ekranda gosterilecek hata metni: backend'in mesaji ya da genel bir metin. */
export function errorMessage(error: unknown): string {
  return error instanceof ApiRequestError ? error.message : 'Beklenmeyen bir hata oluştu.'
}

/**
 * Tam istek adresini kurar. Cok degerli parametreler backend'in bekledigi gibi virgulle birlesir
 * (riskLevel=ORTA,YUKSEK).
 */
export function buildUrl(path: string, params?: QueryParams): string {
  // BASE_URL bossa yol goreli kalir; sayfanin adresiyle tamamlanir.
  const url = new URL(BASE_URL + path, globalThis.location?.origin)
  for (const [key, value] of Object.entries(params ?? {})) {
    if (value === null || value === undefined || value === '') continue
    if (Array.isArray(value)) {
      if (value.length > 0) url.searchParams.set(key, value.join(','))
    } else {
      url.searchParams.set(key, String(value))
    }
  }
  return url.toString()
}

interface RequestOptions {
  params?: QueryParams
  body?: unknown
  signal?: AbortSignal
  /** Beklenen yanit turu; varsayilan JSON. Dosya indirmede her tur kabul edilir. */
  accept?: string
}

/**
 * Tum backend istekleri buradan gecer: token eklenir, hata govdesi tek yerde {@link ApiRequestError}'a cevrilir,
 * token'la gidip 401 donen istek oturumu kapatir. Basarili yanit (2xx) oldugu gibi doner.
 */
async function send(method: string, path: string, options: RequestOptions = {}): Promise<Response> {
  const { params, body, signal, accept = 'application/json' } = options
  const headers: Record<string, string> = { Accept: accept }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = getSession()?.token
  if (token) headers.Authorization = `Bearer ${token}`

  let response: Response
  try {
    response = await fetch(buildUrl(path, params), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (error) {
    // Iptal edilen istek (sayfadan cikildi, sorgu yenilendi) hata olarak gosterilmez.
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiRequestError(0, NETWORK_ERROR, 'Sunucuya ulaşılamadı. Backend çalışıyor mu?')
  }

  if (!response.ok) {
    // Token'la gidip 401 donen istek: token'in suresi dolmus ya da gecersiz (or. sunucu anahtari degisti).
    // Oturum kapanir, sayfa korumasi /login'e yonlendirir. Token'siz istegin 401'i (yanlis parola)
    // oturumu ilgilendirmez; giris formu kendisi gosterir.
    if (response.status === 401 && token) endSession('expired')
    throw await toApiRequestError(response)
  }
  return response
}

/** JSON istekleri. */
export async function apiRequest<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const response = await send(method, path, options)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

/** Indirilen dosya ve backend'in onerdigi adi. */
export interface DownloadedFile {
  blob: Blob
  fileName: string
}

/**
 * Dosya indirir (or. Excel ciktisi). Duz bir baglanti token tasiyamadigi icin istek buradan, token'la yapilir.
 * Hata JSON istekleriyle ayni yoldan {@link ApiRequestError} olur.
 *
 * @param fallbackName backend dosya adi vermezse kullanilir
 */
export async function apiDownload(path: string, params: QueryParams, fallbackName: string): Promise<DownloadedFile> {
  const response = await send('GET', path, { params, accept: '*/*' })
  return {
    blob: await response.blob(),
    fileName: fileNameFrom(response.headers.get('Content-Disposition')) ?? fallbackName,
  }
}

/**
 * Content-Disposition basligindaki dosya adi. UTF-8 bicimi (filename*=UTF-8''...) Turkce harfleri tasidigi icin
 * once o okunur, yoksa duz filename.
 */
export function fileNameFrom(header: string | null): string | null {
  if (!header) return null
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(header)
  if (encoded) {
    try {
      return decodeURIComponent(encoded[1].trim())
    } catch {
      // Bozuk kodlama: duz ada dusulur.
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(header)
  return plain ? plain[1].trim() : null
}

async function toApiRequestError(response: Response): Promise<ApiRequestError> {
  try {
    const body = (await response.json()) as Partial<ApiError>
    if (typeof body?.code === 'string') {
      return new ApiRequestError(
        response.status,
        body.code,
        body.message ?? 'İstek işlenemedi.',
        body.fieldErrors ?? [],
      )
    }
  } catch {
    // Govde JSON degil (or. bir proxy'nin HTML hata sayfasi); asagidaki genel hata kullanilir.
  }
  return new ApiRequestError(response.status, 'UNKNOWN_ERROR', `Beklenmeyen yanıt (HTTP ${response.status}).`)
}

export const api = {
  get: <T>(path: string, params?: QueryParams, signal?: AbortSignal) =>
    apiRequest<T>('GET', path, { params, signal }),
  post: <T>(path: string, body: unknown) => apiRequest<T>('POST', path, { body }),
  put: <T>(path: string, body: unknown) => apiRequest<T>('PUT', path, { body }),
  delete: (path: string) => apiRequest<void>('DELETE', path),
}
