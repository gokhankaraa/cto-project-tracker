import { Button, Group, MultiSelect, Paper, Select, SimpleGrid } from '@mantine/core'

import { NO_DASHBOARD_FILTERS, type DashboardFilters } from '../../api/dashboard'
import { OVERALL_STATUSES, PROJECT_STATUSES, RISK_LEVELS } from '../../api/types'
import { useUsers } from '../../api/users'
import { enumOptions, OVERALL_STATUS_LABELS, PROJECT_STATUS_LABELS, RISK_LEVEL_LABELS } from '../../lib/labels'
import { hasActiveFilters } from './filters'

interface DashboardFilterBarProps {
  filters: DashboardFilters
  onChange: (filters: DashboardFilters) => void
}

const WEEK_OPTIONS = Array.from({ length: 53 }, (_, i) => ({ value: String(i + 1), label: `${i + 1}. hafta` }))

const DELAYED_OPTIONS = [
  { value: 'true', label: 'Yalnızca gecikenler' },
  { value: 'false', label: 'Gecikmeyenler' },
]

/** Son bes yil; adreste daha eski bir yil varsa o da listeye eklenir ki secili gorunsun. */
function yearOptions(selected: number | null) {
  const current = new Date().getFullYear()
  const years = Array.from({ length: 5 }, (_, i) => current - i)
  if (selected !== null && !years.includes(selected)) years.push(selected)
  return years.sort((a, b) => b - a).map(String)
}

const toNumber = (value: string | null) => (value === null ? null : Number(value))

/**
 * Dashboard filtreleri. Yil ve hafta serbest metin yerine listeden secilir: yazarken olusan "2", "20"
 * gibi ara degerler backend'e gidip 400 dondurmez. Hafta, yil secilmeden pasiftir (backend kurali).
 */
export function DashboardFilterBar({ filters, onChange }: DashboardFilterBarProps) {
  const owners = useUsers('PROJECT_MANAGER')
  const set = (patch: Partial<DashboardFilters>) => onChange({ ...filters, ...patch })

  return (
    <Paper withBorder p="md">
      <SimpleGrid cols={{ base: 2, md: 4 }} spacing="sm">
        <Select
          label="Yıl"
          placeholder="En son rapor"
          data={yearOptions(filters.year)}
          value={filters.year === null ? null : String(filters.year)}
          // Yil kaldirilinca hafta da kalkar; yilsiz hafta gecersizdir.
          onChange={(value) => set({ year: toNumber(value), weekNumber: value === null ? null : filters.weekNumber })}
          clearable
        />
        <Select
          label="Hafta"
          placeholder={filters.year === null ? 'Önce yıl seçin' : 'Yılın sonuna kadar'}
          data={WEEK_OPTIONS}
          value={filters.weekNumber === null ? null : String(filters.weekNumber)}
          onChange={(value) => set({ weekNumber: toNumber(value) })}
          disabled={filters.year === null}
          searchable
          clearable
        />
        <Select
          label="Sorumlu"
          placeholder="Tümü"
          data={(owners.data ?? []).map((user) => ({ value: String(user.id), label: user.fullName }))}
          value={filters.ownerId === null ? null : String(filters.ownerId)}
          onChange={(value) => set({ ownerId: toNumber(value) })}
          disabled={owners.isError}
          clearable
        />
        <MultiSelect
          label="Proje durumu"
          placeholder={filters.projectStatus.length === 0 ? 'Tümü' : undefined}
          data={enumOptions(PROJECT_STATUSES, PROJECT_STATUS_LABELS)}
          value={filters.projectStatus}
          onChange={(value) => set({ projectStatus: value as DashboardFilters['projectStatus'] })}
          clearable
        />
        <MultiSelect
          label="Genel durum"
          placeholder={filters.overallStatus.length === 0 ? 'Tümü' : undefined}
          data={enumOptions(OVERALL_STATUSES, OVERALL_STATUS_LABELS)}
          value={filters.overallStatus}
          onChange={(value) => set({ overallStatus: value as DashboardFilters['overallStatus'] })}
          clearable
        />
        <MultiSelect
          label="Risk"
          placeholder={filters.riskLevel.length === 0 ? 'Tümü' : undefined}
          data={enumOptions(RISK_LEVELS, RISK_LEVEL_LABELS)}
          value={filters.riskLevel}
          onChange={(value) => set({ riskLevel: value as DashboardFilters['riskLevel'] })}
          clearable
        />
        <Select
          label="Gecikme"
          placeholder="Tümü"
          data={DELAYED_OPTIONS}
          value={filters.delayed === null ? null : String(filters.delayed)}
          onChange={(value) => set({ delayed: value === null ? null : value === 'true' })}
          clearable
        />
        <Group align="flex-end">
          <Button
            variant="subtle"
            onClick={() => onChange(NO_DASHBOARD_FILTERS)}
            disabled={!hasActiveFilters(filters)}
          >
            Filtreleri temizle
          </Button>
        </Group>
      </SimpleGrid>
    </Paper>
  )
}
