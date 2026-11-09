import type { ApiError, FieldError } from './types'

/** Backend adresi; .env.local'daki VITE_API_BASE_URL ile degistirilir (bkz. .env.example). */
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
  const url = new URL(BASE_URL + path)
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
}

/** Tum backend istekleri buradan gecer: hata govdesi tek yerde {@link ApiRequestError}'a cevrilir. */
export async function apiRequest<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const { params, body, signal } = options
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'

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

  if (!response.ok) throw await toApiRequestError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
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
