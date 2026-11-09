import { Button, Group, MultiSelect, Paper, Select, SimpleGrid, TextInput } from '@mantine/core'
import { useDebouncedCallback } from '@mantine/hooks'
import { useState } from 'react'

import { useProjects } from '../../api/projects'
import { WORK_ITEM_STATUSES } from '../../api/types'
import { NO_WORK_ITEM_FILTERS, type WorkItemFilters } from '../../api/workItems'
import {
  booleanToValue,
  DELAYED_OPTIONS,
  toNumber,
  valueToBoolean,
  WEEK_OPTIONS,
  yearOptions,
} from '../../lib/filterOptions'
import { enumOptions, WORK_ITEM_STATUS_LABELS } from '../../lib/labels'

interface WorkItemFilterBarProps {
  filters: WorkItemFilters
  onChange: (filters: WorkItemFilters) => void
}

/**
 * Sorumlu aramasi: kutu yazildikca guncellenir, filtre yazmayi birakinca (300 ms) uygulanir; her tusta
 * istek gitmez. Adresteki deger disaridan degisirse (Filtreleri temizle, geri tusu) kutu ona uyar.
 */
function AssigneeInput({ value, onChange }: { value: string | null; onChange: (value: string | null) => void }) {
  const [text, setText] = useState(value ?? '')
  const [lastValue, setLastValue] = useState(value)
  if (value !== lastValue) {
    // Render sirasinda esitleme (effect yerine): React'in "onceki prop'a gore state ayarlama" kalibi.
    setLastValue(value)
    if ((value ?? '') !== text.trim()) setText(value ?? '')
  }
  const apply = useDebouncedCallback((next: string) => onChange(next.trim() === '' ? null : next.trim()), 300)

  return (
    <TextInput
      label="Sorumlu"
      placeholder="Ad ile ara"
      value={text}
      onChange={(event) => {
        setText(event.currentTarget.value)
        apply(event.currentTarget.value)
      }}
    />
  )
}

export function WorkItemFilterBar({ filters, onChange }: WorkItemFilterBarProps) {
  const projects = useProjects()
  const set = (patch: Partial<WorkItemFilters>) => onChange({ ...filters, ...patch })
  const active = JSON.stringify(filters) !== JSON.stringify(NO_WORK_ITEM_FILTERS)

  return (
    <Paper withBorder p="md">
      <SimpleGrid cols={{ base: 2, md: 4 }} spacing="sm">
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
        <Select
          label="Yıl"
          placeholder="En son rapor"
          data={yearOptions(filters.year)}
          value={filters.year === null ? null : String(filters.year)}
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
        <AssigneeInput value={filters.assignee} onChange={(assignee) => set({ assignee })} />
        <MultiSelect
          label="Durum"
          placeholder={filters.status.length === 0 ? 'Tümü' : undefined}
          data={enumOptions(WORK_ITEM_STATUSES, WORK_ITEM_STATUS_LABELS)}
          value={filters.status}
          onChange={(value) => set({ status: value as WorkItemFilters['status'] })}
          clearable
        />
        <Select
          label="Gecikme"
          placeholder="Tümü"
          data={DELAYED_OPTIONS}
          value={booleanToValue(filters.delayed)}
          onChange={(value) => set({ delayed: valueToBoolean(value) })}
          clearable
        />
        <Group align="flex-end">
          <Button variant="subtle" onClick={() => onChange(NO_WORK_ITEM_FILTERS)} disabled={!active}>
            Filtreleri temizle
          </Button>
        </Group>
      </SimpleGrid>
    </Paper>
  )
}
