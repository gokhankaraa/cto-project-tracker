import { Button, Group, MultiSelect, Paper, Select, SimpleGrid, TextInput } from '@mantine/core'

import { useProjects } from '../../api/projects'
import { NO_REPORT_FILTERS, type ReportFilters } from '../../api/reports'
import { OVERALL_STATUSES, PROGRESS_STAGES, RISK_LEVELS } from '../../api/types'
import { useUsers } from '../../api/users'
import { toNumber, WEEK_OPTIONS, yearOptions } from '../../lib/filterOptions'
import {
  enumOptions,
  OVERALL_STATUS_LABELS,
  PROGRESS_STAGE_LABELS,
  RISK_LEVEL_LABELS,
} from '../../lib/labels'

interface ReportFilterBarProps {
  filters: ReportFilters
  onChange: (filters: ReportFilters) => void
}

/** Bos birakilan tarih kutusu '' verir; filtre icin null. */
const dateValue = (value: string) => (value === '' ? null : value)

/**
 * Rapor listesi filtreleri. Ters aralik secilemez: bitis haftasi listesinde baslangictan kucuk haftalar
 * pasiftir, tarih kutularinin min/max'i birbirine baglidir. Baslangic bitisi gecerse bitis temizlenir.
 */
export function ReportFilterBar({ filters, onChange }: ReportFilterBarProps) {
  const projects = useProjects()
  const owners = useUsers('PROJECT_MANAGER')
  const set = (patch: Partial<ReportFilters>) => onChange({ ...filters, ...patch })
  const active = JSON.stringify(filters) !== JSON.stringify(NO_REPORT_FILTERS)
  const noYear = filters.year === null

  const setWeekFrom = (weekFrom: number | null) =>
    set({ weekFrom, weekTo: weekFrom !== null && filters.weekTo !== null && filters.weekTo < weekFrom ? null : filters.weekTo })
  const setDateFrom = (dateFrom: string | null) =>
    set({ dateFrom, dateTo: dateFrom !== null && filters.dateTo !== null && filters.dateTo < dateFrom ? null : filters.dateTo })

  return (
    <Paper withBorder p="md">
      <SimpleGrid cols={{ base: 2, md: 4 }} spacing="sm">
        <Select
          label="Proje"
          placeholder="Tümü"
          data={(projects.data ?? []).map((project) => ({ value: String(project.id), label: project.name }))}
          value={filters.projectId === null ? null : String(filters.projectId)}
          onChange={(value) => set({ projectId: toNumber(value) })}
          searchable
          clearable
        />
        <Select
          label="Sorumlu"
          placeholder="Tümü"
          data={(owners.data ?? []).map((user) => ({ value: String(user.id), label: user.fullName }))}
          value={filters.ownerId === null ? null : String(filters.ownerId)}
          onChange={(value) => set({ ownerId: toNumber(value) })}
          clearable
        />
        <Select
          label="Yıl"
          placeholder="Tümü"
          data={yearOptions(filters.year)}
          value={filters.year === null ? null : String(filters.year)}
          // Yil kalkinca hafta araligi da kalkar (hafta yalnizca yil ile gecerli).
          onChange={(value) =>
            set(value === null ? { year: null, weekFrom: null, weekTo: null } : { year: toNumber(value) })
          }
          clearable
        />
        <Select
          label="İlk hafta"
          placeholder={noYear ? 'Önce yıl seçin' : 'Yılın başı'}
          data={WEEK_OPTIONS}
          value={filters.weekFrom === null ? null : String(filters.weekFrom)}
          onChange={(value) => setWeekFrom(toNumber(value))}
          disabled={noYear}
          searchable
          clearable
        />
        <Select
          label="Son hafta"
          placeholder={noYear ? 'Önce yıl seçin' : 'Yılın sonu'}
          data={WEEK_OPTIONS.map((option) => ({
            ...option,
            disabled: filters.weekFrom !== null && Number(option.value) < filters.weekFrom,
          }))}
          value={filters.weekTo === null ? null : String(filters.weekTo)}
          onChange={(value) => set({ weekTo: toNumber(value) })}
          disabled={noYear}
          searchable
          clearable
        />
        <TextInput
          type="date"
          label="Başlangıç tarihi"
          value={filters.dateFrom ?? ''}
          max={filters.dateTo ?? undefined}
          onChange={(event) => setDateFrom(dateValue(event.currentTarget.value))}
        />
        <TextInput
          type="date"
          label="Bitiş tarihi"
          value={filters.dateTo ?? ''}
          min={filters.dateFrom ?? undefined}
          onChange={(event) => set({ dateTo: dateValue(event.currentTarget.value) })}
        />
        <MultiSelect
          label="Aşama"
          placeholder={filters.progressStage.length === 0 ? 'Tümü' : undefined}
          data={enumOptions(PROGRESS_STAGES, PROGRESS_STAGE_LABELS)}
          value={filters.progressStage}
          onChange={(value) => set({ progressStage: value as ReportFilters['progressStage'] })}
          clearable
        />
        <MultiSelect
          label="Genel durum"
          placeholder={filters.overallStatus.length === 0 ? 'Tümü' : undefined}
          data={enumOptions(OVERALL_STATUSES, OVERALL_STATUS_LABELS)}
          value={filters.overallStatus}
          onChange={(value) => set({ overallStatus: value as ReportFilters['overallStatus'] })}
          clearable
        />
        <MultiSelect
          label="Risk"
          placeholder={filters.riskLevel.length === 0 ? 'Tümü' : undefined}
          data={enumOptions(RISK_LEVELS, RISK_LEVEL_LABELS)}
          value={filters.riskLevel}
          onChange={(value) => set({ riskLevel: value as ReportFilters['riskLevel'] })}
          clearable
        />
        <Group align="flex-end">
          <Button variant="subtle" onClick={() => onChange(NO_REPORT_FILTERS)} disabled={!active}>
            Filtreleri temizle
          </Button>
        </Group>
      </SimpleGrid>
    </Paper>
  )
}
