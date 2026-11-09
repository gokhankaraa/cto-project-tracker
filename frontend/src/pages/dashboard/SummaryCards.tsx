import { Paper, SimpleGrid, Text, type MantineColor } from '@mantine/core'

import type { DashboardSummary } from '../../api/types'

interface Counter {
  label: string
  value: number
  /** Sifirdan buyukse sayi bu renkte gosterilir (dikkat isteyen sayaclar). */
  alertColor?: MantineColor
}

/** Ozet sayaclari. Backend sayaclari filtrelenmis kumeden hesaplar; kartlar tabloyla hep tutarlidir. */
export function SummaryCards({ summary }: { summary: DashboardSummary }) {
  const counters: Counter[] = [
    { label: 'Toplam proje', value: summary.totalProjects },
    { label: 'Yüksek riskli', value: summary.highRiskProjects, alertColor: 'red' },
    { label: 'Geciken', value: summary.delayedProjects, alertColor: 'orange' },
    { label: 'Raporu olmayan', value: summary.projectsWithoutReport, alertColor: 'yellow.7' },
    { label: 'Canlı task', value: summary.totalLiveTasks },
  ]

  return (
    <SimpleGrid cols={{ base: 2, sm: 3, lg: 5 }} spacing="sm">
      {counters.map(({ label, value, alertColor }) => (
        <Paper key={label} withBorder p="md" role="group" aria-label={label}>
          <Text size="xs" c="dimmed" tt="uppercase" fw={600}>
            {label}
          </Text>
          <Text fz={28} fw={700} lh={1.2} c={value > 0 ? alertColor : undefined}>
            {value}
          </Text>
        </Paper>
      ))}
    </SimpleGrid>
  )
}
