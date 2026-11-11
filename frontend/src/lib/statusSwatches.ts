import type { OverallStatus } from '../api/types'

/** Genel durum renkleri (rozetlerle ayni tonlar); logo, giris sayfasi ve hafta hucrelerinde kullanilir. */
export const STATUS_SWATCHES: Record<OverallStatus, string> = {
  YOLUNDA: 'var(--mantine-color-teal-5)',
  RISKLI: 'var(--mantine-color-yellow-5)',
  GECIKMELI: 'var(--mantine-color-orange-6)',
  BLOKE: 'var(--mantine-color-red-6)',
}
