import { Box, Button, Center, Loader, Stack } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useMutation } from '@tanstack/react-query'
import { lazy, Suspense, useMemo } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { errorMessage } from '../../api/client'
import { downloadDashboardExport, useDashboardSummary, type DashboardFilters } from '../../api/dashboard'
import { PageHeader } from '../../components/PageHeader'
import { QueryState } from '../../components/QueryState'
import { saveFile } from '../../lib/download'
import { DashboardFilterBar } from './DashboardFilterBar'
import { dashboardSearch, parseDashboardFilters } from './filters'
import { ProjectSummaryTable } from './ProjectSummaryTable'
import { SummaryCards, TrackingCards } from './SummaryCards'

/**
 * Grafikler ayri parca: grafik kutuphanesi (recharts) buyuk, dashboard ise giristen sonraki ilk sayfa. Kartlar ve
 * tablo hemen gorunur, grafikler kendi parcasi inince gelir.
 */
const TrendSection = lazy(() => import('./TrendSection').then((m) => ({ default: m.TrendSection })))

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
  // Ekrandakinin dosya hali: ayni filtrelerle. Indirme bitince dosya tarayiciya kaydedilir.
  const exportFile = useMutation({
    mutationFn: () => downloadDashboardExport(filters),
    onSuccess: ({ blob, fileName }) => saveFile(blob, fileName),
    onError: (error) =>
      notifications.show({ color: 'red', title: 'Excel dosyası indirilemedi', message: errorMessage(error) }),
  })

  // replace: her filtre secimi tarayici gecmisine ayri bir adim olarak eklenmez.
  const setFilters = (next: DashboardFilters) => navigate({ search: dashboardSearch(next) }, { replace: true })

  return (
    <Stack gap="md">
      <PageHeader
        title="Portföy özeti"
        description={referenceText(filters)}
        actions={
          <Button variant="default" onClick={() => exportFile.mutate()} loading={exportFile.isPending}>
            Excel'e aktar
          </Button>
        }
      />

      <DashboardFilterBar filters={filters} onChange={setFilters} />

      <QueryState query={summary}>
        {(data) => (
          // Yeni filtrenin yaniti beklenirken eski veri soluk gosterilir.
          <Box style={{ opacity: summary.isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <Stack gap="md">
              <SummaryCards summary={data} />
              <TrackingCards
                summary={data}
                hasWeekReference={filters.year !== null}
              />
              <ProjectSummaryTable projects={data.projects} />
            </Stack>
          </Box>
        )}
      </QueryState>

      {/* Kendi sorgusu var; ozet yuklenirken ya da hata verirken de bagimsiz gorunur. */}
      <Suspense
        fallback={
          <Center h={120}>
            <Loader size="sm" aria-label="Grafikler yükleniyor" />
          </Center>
        }
      >
        <TrendSection filters={filters} />
      </Suspense>
    </Stack>
  )
}
