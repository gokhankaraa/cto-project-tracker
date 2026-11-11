import { Badge, Tooltip } from '@mantine/core'

import { useHealth } from '../api/health'

/**
 * Ust bardaki baglanti gostergesi (Faz 0'daki saglik kartinin yerini aldi). Backend ya da veritabani
 * dusunce ekranlardaki hatanin nedeni buradan da gorulur.
 */
export function BackendStatus() {
  const health = useHealth()

  if (health.isPending) return null

  const up = health.isSuccess && health.data.status === 'UP'
  const detail = health.isSuccess
    ? `Veritabanı: ${health.data.database} (${health.data.databaseStatus})`
    : "Backend'e ulaşılamadı"

  return (
    <Tooltip label={detail}>
      <Badge color={up ? 'teal' : 'red'} variant="dot" size="md" tt="none" fw={500}>
        {up ? 'Backend bağlı' : 'Bağlantı yok'}
      </Badge>
    </Tooltip>
  )
}
