import { Box, Stack, Text, Title } from '@mantine/core'
import { useMemo } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { useDashboardSummary, type DashboardFilters } from '../../api/dashboard'
import { QueryState } from '../../components/QueryState'
import { DashboardFilterBar } from './DashboardFilterBar'
import { dashboardSearch, parseDashboardFilters } from './filters'
import { ProjectSummaryTable } from './ProjectSummaryTable'
import { SummaryCards } from './SummaryCards'

/** Kartlarin ve tablonun hangi rapor noktasina gore hesaplandigini soyler. */
function referenceText({ year, weekNumber }: DashboardFilters) {
  if (year === null) return 'Her proje için en son haftalık rapora göre.'
  if (weekNumber === null) return `Her proje için ${year} sonuna kadarki en son rapora göre.`
  return `Her proje için ${year} yılı ${weekNumber}. haftaya kadarki en son rapora göre.`
}

/**
 * CTO dashboard'u (on analiz H-06). Filtreler adreste tutulur (/?riskLevel=YUKSEK&year=2026):
 * sayfa yenilenince korunur ve adres paylasilabilir.
 */
export function DashboardPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const filters = useMemo(() => parseDashboardFilters(searchParams), [searchParams])
  const summary = useDashboardSummary(filters)

  // replace: her filtre secimi tarayici gecmisine ayri bir adim olarak eklenmez.
  const setFilters = (next: DashboardFilters) => navigate({ search: dashboardSearch(next) }, { replace: true })

  return (
    <Stack gap="md">
      <div>
        <Title order={2}>Portföy özeti</Title>
        <Text size="sm" c="dimmed">
          {referenceText(filters)}
        </Text>
      </div>

      <DashboardFilterBar filters={filters} onChange={setFilters} />

      <QueryState query={summary}>
        {(data) => (
          // Yeni filtrenin yaniti beklenirken eski veri soluk gosterilir.
          <Box style={{ opacity: summary.isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <Stack gap="md">
              <SummaryCards summary={data} />
              <ProjectSummaryTable projects={data.projects} />
            </Stack>
          </Box>
        )}
      </QueryState>
    </Stack>
  )
}
