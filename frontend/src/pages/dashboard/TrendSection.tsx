import { BarChart, type BarChartSeries } from '@mantine/charts'
import { Button, Group, Paper, SimpleGrid, Stack, Table, Text, Title } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'

import { useDashboardTrend, type DashboardFilters } from '../../api/dashboard'
import type { TrendWeek } from '../../api/types'
import { QueryState } from '../../components/QueryState'
import { formatWeek } from '../../lib/format'

/**
 * Grafik renkleri. Rozetlerle ayni ton aileleri, ama acik zeminde ve renk korlugunde birbirinden ayrilacak
 * adimlar secildi (dataviz dogrulayicisi: bitisik ciftler CVD ΔE >= 8.6, normal gorus ΔE >= 15).
 * Durum renkleri anlam tasir (iyi -> kritik); "Rapor yok" bir durum degil, verinin yoklugu: notr gri.
 */
const STATUS_SERIES = [
  { name: 'YOLUNDA', label: 'Yolunda', color: '#12b886' },
  { name: 'RISKLI', label: 'Riskli', color: '#f08c00' },
  { name: 'GECIKMELI', label: 'Gecikmeli', color: '#d9480f' },
  { name: 'BLOKE', label: 'Bloke', color: '#a61e4d' },
  { name: 'RAPOR_YOK', label: 'Rapor yok', color: '#ced4da' },
]

/**
 * Tur renkleri, tur rozetlerinin ton aileleri (risk turuncu, engel kirmizi, karar mor, aksiyon mavi). Acik kayitlar
 * da her hafta bir toplamin parcalari oldugu icin yigilmis cubuk: kucuk tam sayilarda cizgiler ust uste binip
 * birbirini gizlerdi.
 */
const TRACKING_SERIES = [
  { name: 'openRisks', label: 'Açık risk', color: '#e8590c' },
  { name: 'openBlockers', label: 'Açık engel', color: '#a61e4d' },
  { name: 'pendingDecisions', label: 'Bekleyen karar', color: '#cc5de8' },
  { name: 'openActions', label: 'Açık aksiyon', color: '#1864ab' },
]

function referenceText({ year, weekNumber }: DashboardFilters) {
  if (year === null) return 'Bu haftaya kadarki 12 hafta.'
  if (weekNumber === null) return `${year} sonuna kadarki 12 hafta.`
  return `${year} yılı ${weekNumber}. haftaya kadarki 12 hafta.`
}

/** Grafik verisi: eksende kisa hafta numarasi, ipucunda tam hafta ("2026 / 31. hafta"). */
function toChartRows(weeks: TrendWeek[]) {
  return weeks.map((week) => ({
    week: formatWeek(week.year, week.weekNumber),
    ...week.projectsByStatus,
    RAPOR_YOK: week.totalProjects - week.projectsWithReport,
    openRisks: week.openRisks,
    openBlockers: week.openBlockers,
    pendingDecisions: week.pendingDecisions,
    openActions: week.openActions,
  }))
}

const shortWeek = (label: string) => label.replace(/^\d+ \/ (\d+)\. hafta$/, '$1')

/** Iki grafigin ortak gorunumu: yigilmis cubuk, gosterge seri sirasinda, yalnizca yatay izgara cizgileri. */
function StackedWeeks({ rows, series, label }: { rows: object[]; series: BarChartSeries[]; label: string }) {
  // Recharts gostergeyi varsayilan olarak ada gore siralar; seri sirasi (yigin ve anlam sirasi) korunur.
  const order = (item: { dataKey?: unknown }) => series.findIndex((s) => s.name === item.dataKey)
  return (
    <BarChart
      h={240}
      data={rows as Record<string, unknown>[]}
      dataKey="week"
      type="stacked"
      series={series}
      withLegend
      legendProps={{ verticalAlign: 'bottom', itemSorter: order }}
      gridAxis="x"
      tickLine="none"
      xAxisProps={{ tickFormatter: shortWeek }}
      yAxisProps={{ allowDecimals: false, width: 30 }}
      // Yigilmis parcalar arasinda 2 px zemin rengi bosluk: komsu renkler birbirine karismaz.
      barProps={{ stroke: 'var(--mantine-color-body)', strokeWidth: 2 }}
      aria-label={label}
    />
  )
}

/** Dashboard'un "Son 12 hafta" bolumu: proje durumlari ve acik kayitlar, haftadan haftaya. */
export function TrendSection({ filters }: { filters: DashboardFilters }) {
  const trend = useDashboardTrend(filters)
  const [tableOpen, { toggle }] = useDisclosure(false)

  return (
    <Stack gap="xs">
      <Group justify="space-between" align="flex-start" wrap="nowrap">
        <div>
          <Title order={4}>Son 12 hafta</Title>
          <Text size="sm" c="dimmed">
            {referenceText(filters)} Her proje o haftaya kadarki en son raporuyla sayılır; riskler ve aksiyonlar hafta
            sonunda açık olanlardır.
          </Text>
        </div>
        <Button variant="subtle" size="compact-sm" onClick={toggle} aria-expanded={tableOpen}>
          {tableOpen ? 'Tabloyu gizle' : 'Tablo olarak göster'}
        </Button>
      </Group>

      <QueryState query={trend}>
        {(data) => {
          const rows = toChartRows(data.weeks)
          return (
            <Stack gap="sm">
              <SimpleGrid cols={{ base: 1, lg: 2 }} spacing="sm">
                <Paper withBorder p="md">
                  <Text fw={600} size="sm" mb="xs">
                    Projelerin durumu
                  </Text>
                  <StackedWeeks rows={rows} series={STATUS_SERIES} label="Haftalara göre proje durumları" />
                </Paper>
                <Paper withBorder p="md">
                  <Text fw={600} size="sm" mb="xs">
                    Açık riskler ve aksiyonlar
                  </Text>
                  <StackedWeeks
                    rows={rows}
                    series={TRACKING_SERIES}
                    label="Haftalara göre açık riskler, engeller, kararlar ve aksiyonlar"
                  />
                </Paper>
              </SimpleGrid>
              {tableOpen && <TrendTable weeks={data.weeks} />}
            </Stack>
          )
        }}
      </QueryState>
    </Stack>
  )
}

/** Grafiklerin sayilari tablo olarak (renk gormeden okunabilir; ekran okuyucu icin de). */
function TrendTable({ weeks }: { weeks: TrendWeek[] }) {
  const headers = [
    'Hafta',
    'Raporlu',
    'Yolunda',
    'Riskli',
    'Gecikmeli',
    'Bloke',
    'Rapor yok',
    'Açık risk',
    'Açık engel',
    'Bekleyen karar',
    'Açık aksiyon',
  ]
  return (
    <Paper withBorder>
      <Table.ScrollContainer minWidth={900}>
        <Table striped verticalSpacing="xs" aria-label="Son 12 haftanın sayıları">
          <Table.Thead>
            <Table.Tr>
              {headers.map((header, i) => (
                <Table.Th key={header} ta={i === 0 ? undefined : 'right'}>
                  {header}
                </Table.Th>
              ))}
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {weeks.map((week) => (
              <Table.Tr key={week.weekStart}>
                <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatWeek(week.year, week.weekNumber)}</Table.Td>
                {[
                  week.projectsWithReport,
                  week.projectsByStatus.YOLUNDA,
                  week.projectsByStatus.RISKLI,
                  week.projectsByStatus.GECIKMELI,
                  week.projectsByStatus.BLOKE,
                  week.totalProjects - week.projectsWithReport,
                  week.openRisks,
                  week.openBlockers,
                  week.pendingDecisions,
                  week.openActions,
                ].map((value, i) => (
                  <Table.Td key={i} ta="right">
                    {value}
                  </Table.Td>
                ))}
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  )
}
