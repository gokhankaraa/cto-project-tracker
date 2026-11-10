import { Button, Group, MultiSelect, Paper, Select, SimpleGrid } from '@mantine/core'

import { useProjects } from '../../api/projects'
import { RISK_LEVELS, TRACKING_ITEM_TYPES } from '../../api/types'
import { AssigneeInput } from '../../components/AssigneeInput'
import { booleanToValue, DELAYED_OPTIONS, toNumber, valueToBoolean } from '../../lib/filterOptions'
import { enumOptions, RISK_LEVEL_LABELS, TRACKING_ITEM_TYPE_LABELS } from '../../lib/labels'
import { NO_TRACKING_FILTERS, type TrackingListFilters, type TrackingScope } from './filters'

const SCOPE_OPTIONS: { value: TrackingScope; label: string }[] = [
  { value: 'open', label: 'Kapanmamışlar' },
  { value: 'closed', label: 'Kapananlar' },
  { value: 'all', label: 'Tümü' },
]

interface TrackingItemFilterBarProps {
  filters: TrackingListFilters
  onChange: (filters: TrackingListFilters) => void
}

export function TrackingItemFilterBar({ filters, onChange }: TrackingItemFilterBarProps) {
  const projects = useProjects()
  const set = (patch: Partial<TrackingListFilters>) => onChange({ ...filters, ...patch })
  const active = JSON.stringify(filters) !== JSON.stringify(NO_TRACKING_FILTERS)

  return (
    <Paper withBorder p="md">
      <SimpleGrid cols={{ base: 2, md: 4 }} spacing="sm">
        <Select
          label="Kayıtlar"
          data={SCOPE_OPTIONS}
          value={filters.scope}
          onChange={(value) => set({ scope: (value as TrackingScope | null) ?? 'open' })}
          allowDeselect={false}
        />
        <Select
          label="Proje"
          placeholder="Tümü"
          data={(projects.data ?? []).map((project) => ({ value: String(project.id), label: project.name }))}
          value={filters.projectId === null ? null : String(filters.projectId)}
          onChange={(value) => set({ projectId: toNumber(value) })}
          disabled={projects.isError}
          searchable
          clearable
        />
        <MultiSelect
          label="Tür"
          placeholder={filters.type.length === 0 ? 'Tümü' : undefined}
          data={enumOptions(TRACKING_ITEM_TYPES, TRACKING_ITEM_TYPE_LABELS)}
          value={filters.type}
          onChange={(value) => set({ type: value as TrackingListFilters['type'] })}
          clearable
        />
        <MultiSelect
          label="Risk seviyesi"
          placeholder={filters.riskLevel.length === 0 ? 'Tümü' : undefined}
          data={enumOptions(RISK_LEVELS, RISK_LEVEL_LABELS)}
          value={filters.riskLevel}
          onChange={(value) => set({ riskLevel: value as TrackingListFilters['riskLevel'] })}
          clearable
        />
        <Select
          label="Gecikme"
          placeholder="Tümü"
          data={DELAYED_OPTIONS}
          value={booleanToValue(filters.overdue)}
          onChange={(value) => set({ overdue: valueToBoolean(value) })}
          clearable
        />
        <AssigneeInput value={filters.assignee} onChange={(assignee) => set({ assignee })} />
        <Group align="flex-end">
          <Button variant="subtle" onClick={() => onChange(NO_TRACKING_FILTERS)} disabled={!active}>
            Filtreleri temizle
          </Button>
        </Group>
      </SimpleGrid>
    </Paper>
  )
}
