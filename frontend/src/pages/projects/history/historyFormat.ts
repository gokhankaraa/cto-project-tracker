import type { AuditEntityType } from '../../../api/types'
import { formatDate } from '../../../lib/format'
import {
  OVERALL_STATUS_LABELS,
  PROGRESS_STAGE_LABELS,
  PROJECT_STATUS_LABELS,
  RISK_LEVEL_LABELS,
  TRACKING_ITEM_STATUS_LABELS,
  TRACKING_ITEM_TYPE_LABELS,
  WORK_ITEM_STATUS_LABELS,
  type EnumLabels,
} from '../../../lib/labels'

/**
 * Gecmisteki alan adlari (API'deki adlar) -> formlardaki etiketler. Ayni ad turden ture farkli anlama gelebilir
 * (projede "name" proje adi), bu yuzden once ture ozel etiket aranir.
 */
const FIELD_LABELS: Record<string, string> = {
  title: 'Başlık',
  description: 'Açıklama',
  status: 'Durum',
  assignee: 'Sorumlu',
  riskLevel: 'Risk seviyesi',
  year: 'Yıl',
  weekNumber: 'Hafta',
  reportDate: 'Rapor tarihi',
  progressStage: 'İlerleme aşaması',
  overallStatus: 'Genel durum',
  done: 'Bu hafta yapılanlar',
  planned: 'Gelecek hafta planlananlar',
  risks: 'Riskler / engeller',
  note: 'Genel not',
  plannedDate: 'Planlanan tarih',
  completedDate: 'Tamamlanma tarihi',
  type: 'Tür',
  impact: 'Etki',
  actionPlan: 'Aksiyon planı',
  followUpDate: 'Takip tarihi',
  closedDate: 'Kapanış tarihi',
}

const FIELD_LABELS_BY_TYPE: Partial<Record<AuditEntityType, Record<string, string>>> = {
  PROJE: { name: 'Proje adı', customer: 'Müşteri', owner: 'Sorumlu proje yöneticisi' },
  RAPOR: { riskLevel: 'Risk' },
}

/** Bilinmeyen alan (backend'e yeni eklenmis) adiyla gorunur; ekran bozulmaz. */
export function fieldLabel(entityType: AuditEntityType, field: string): string {
  return FIELD_LABELS_BY_TYPE[entityType]?.[field] ?? FIELD_LABELS[field] ?? field
}

/** Durum alani kaydin turune gore farkli enum'dur. */
const STATUS_LABELS: Record<AuditEntityType, EnumLabels<string>> = {
  PROJE: PROJECT_STATUS_LABELS,
  RAPOR: OVERALL_STATUS_LABELS,
  IS_KALEMI: WORK_ITEM_STATUS_LABELS,
  TAKIP_KAYDI: TRACKING_ITEM_STATUS_LABELS,
}

const ENUM_FIELDS: Record<string, EnumLabels<string>> = {
  riskLevel: RISK_LEVEL_LABELS,
  progressStage: PROGRESS_STAGE_LABELS,
  overallStatus: OVERALL_STATUS_LABELS,
  type: TRACKING_ITEM_TYPE_LABELS,
}

const DATE_FIELDS = new Set(['reportDate', 'plannedDate', 'completedDate', 'followUpDate', 'closedDate'])

/**
 * Ham degeri ekrandaki haline cevirir: enum -> Turkce etiket, ISO tarih -> 20.07.2026. Bos deger null doner
 * (cagiran "bos" yazar). Etiketi olmayan enum degeri ham haliyle gorunur.
 */
export function formatValue(entityType: AuditEntityType, field: string, value: string | null): string | null {
  if (value === null) return null
  const labels = field === 'status' ? STATUS_LABELS[entityType] : ENUM_FIELDS[field]
  if (labels) return labels[value]?.label ?? value
  if (DATE_FIELDS.has(field)) return formatDate(value)
  return value
}
