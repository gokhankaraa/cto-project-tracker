/**
 * Backend yanit tipleri. Alan adlari backend'deki DTO'larla birebir aynidir
 * (backend/src/main/java/com/kolaysoft/ctotracker/...).
 */

/** Dogrulama hatasindaki tek bir alan (ApiError.fieldErrors). */
export interface FieldError {
  field: string
  message: string
}

/**
 * Backend'in tum hatalarda dondurdugu ortak govde (common/error/ApiError.java).
 * fieldErrors yalnizca VALIDATION_ERROR'da bulunur.
 */
export interface ApiError {
  timestamp: string
  status: number
  code: string
  message: string
  path: string
  fieldErrors?: FieldError[]
}

/** GET /api/health */
export interface HealthResponse {
  status: string
  application: string
  databaseStatus: string
  database: string
  timestamp: string
}
