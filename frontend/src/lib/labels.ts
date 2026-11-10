import type { MantineColor } from '@mantine/core'

import type {
  OverallStatus,
  ProgressStage,
  ProjectStatus,
  RiskLevel,
  Role,
  TrackingItemStatus,
  TrackingItemType,
  WorkItemStatus,
} from '../api/types'

/** Bir enum degerinin ekranda gorunen adi ve rozet rengi. */
export interface EnumLabel {
  label: string
  color: MantineColor
}

/**
 * Enum degeri -> etiket eslemesi. Record<Enum, ...> tipi sayesinde backend'e yeni bir deger eklenip
 * types.ts guncellendiginde burada eksik kalan etiket derleme hatasi verir.
 */
export type EnumLabels<E extends string> = Record<E, EnumLabel>

export const PROJECT_STATUS_LABELS: EnumLabels<ProjectStatus> = {
  PLANLANDI: { label: 'Planlandı', color: 'gray' },
  DEVAM_EDIYOR: { label: 'Devam ediyor', color: 'blue' },
  TAMAMLANDI: { label: 'Tamamlandı', color: 'teal' },
  RISKLI: { label: 'Riskli', color: 'yellow' },
  BLOKE: { label: 'Bloke', color: 'red' },
}

export const OVERALL_STATUS_LABELS: EnumLabels<OverallStatus> = {
  YOLUNDA: { label: 'Yolunda', color: 'teal' },
  RISKLI: { label: 'Riskli', color: 'yellow' },
  GECIKMELI: { label: 'Gecikmeli', color: 'orange' },
  BLOKE: { label: 'Bloke', color: 'red' },
}

export const RISK_LEVEL_LABELS: EnumLabels<RiskLevel> = {
  DUSUK: { label: 'Düşük', color: 'teal' },
  ORTA: { label: 'Orta', color: 'yellow' },
  YUKSEK: { label: 'Yüksek', color: 'red' },
}

export const PROGRESS_STAGE_LABELS: EnumLabels<ProgressStage> = {
  ANALIZ: { label: 'Analiz', color: 'indigo' },
  GELISTIRME: { label: 'Geliştirme', color: 'indigo' },
  TEST: { label: 'Test', color: 'indigo' },
  TAMAMLANDI: { label: 'Tamamlandı', color: 'teal' },
}

export const WORK_ITEM_STATUS_LABELS: EnumLabels<WorkItemStatus> = {
  PLANLANDI: { label: 'Planlandı', color: 'gray' },
  DEVAM_EDIYOR: { label: 'Devam ediyor', color: 'blue' },
  TESTTE: { label: 'Testte', color: 'violet' },
  TAMAMLANDI: { label: 'Tamamlandı', color: 'teal' },
  GECIKTI: { label: 'Gecikti', color: 'orange' },
  RISKLI: { label: 'Riskli', color: 'yellow' },
  BLOKE: { label: 'Bloke', color: 'red' },
}

export const TRACKING_ITEM_TYPE_LABELS: EnumLabels<TrackingItemType> = {
  RISK: { label: 'Risk', color: 'orange' },
  ENGEL: { label: 'Engel', color: 'red' },
  KARAR: { label: 'Karar', color: 'grape' },
  AKSIYON: { label: 'Aksiyon', color: 'blue' },
}

export const TRACKING_ITEM_STATUS_LABELS: EnumLabels<TrackingItemStatus> = {
  ACIK: { label: 'Açık', color: 'gray' },
  DEVAM_EDIYOR: { label: 'Devam ediyor', color: 'blue' },
  KAPANDI: { label: 'Kapandı', color: 'teal' },
}

/** Karar kaydinda durumlar ne oldugunu soyler: acik karar "bekleniyor", kapali karar "verildi". */
export const DECISION_STATUS_LABELS: EnumLabels<TrackingItemStatus> = {
  ACIK: { label: 'Karar bekleniyor', color: 'gray' },
  DEVAM_EDIYOR: { label: 'Görüşülüyor', color: 'blue' },
  KAPANDI: { label: 'Karar verildi', color: 'teal' },
}

/** Kaydin turune gore durum etiketleri. */
export const trackingStatusLabels = (type: TrackingItemType) =>
  type === 'KARAR' ? DECISION_STATUS_LABELS : TRACKING_ITEM_STATUS_LABELS

export const ROLE_LABELS: EnumLabels<Role> = {
  PROJECT_MANAGER: { label: 'Proje yöneticisi', color: 'blue' },
  CTO: { label: 'CTO', color: 'grape' },
  ADMIN: { label: 'Yönetici', color: 'gray' },
}

/** Select / MultiSelect icin { value, label } secenekleri; sira enum dizisindeki siradir. */
export function enumOptions<E extends string>(values: readonly E[], labels: EnumLabels<E>) {
  return values.map((value) => ({ value, label: labels[value].label }))
}
