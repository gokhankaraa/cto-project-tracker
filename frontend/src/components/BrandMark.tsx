import { STATUS_SWATCHES } from '../lib/statusSwatches'

/**
 * Uygulamanin logosu: indigo zemin uzerinde dort durum rengi, dashboard'daki hafta hucreleri gibi.
 * public/favicon.svg ayni sekildir; birini degistirirken digerini de guncelleyin.
 */
export function BrandMark({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true" focusable="false" style={{ flex: 'none' }}>
      <rect width="32" height="32" rx="8" fill="var(--mantine-color-indigo-9)" />
      <rect x="6" y="6" width="9" height="9" rx="2.5" fill={STATUS_SWATCHES.YOLUNDA} />
      <rect x="17" y="6" width="9" height="9" rx="2.5" fill={STATUS_SWATCHES.RISKLI} />
      <rect x="6" y="17" width="9" height="9" rx="2.5" fill={STATUS_SWATCHES.GECIKMELI} />
      <rect x="17" y="17" width="9" height="9" rx="2.5" fill={STATUS_SWATCHES.BLOKE} />
    </svg>
  )
}
