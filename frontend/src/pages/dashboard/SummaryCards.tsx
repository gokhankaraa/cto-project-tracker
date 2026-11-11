import {
  getThemeColor,
  Group,
  Paper,
  SimpleGrid,
  Stack,
  Text,
  Title,
  UnstyledButton,
  useMantineTheme,
  type MantineColor,
} from '@mantine/core'
import { Link } from 'react-router'

import type { DashboardSummary } from '../../api/types'
import classes from './SummaryCards.module.css'

interface Counter {
  label: string
  value: number
  /** Sifirdan buyukse sayi bu renkte gosterilir (dikkat isteyen sayaclar). */
  alertColor?: MantineColor
  /** Verilirse kart, sayilan kayitlarin listesine gider. */
  to?: string
}

function CounterCard({ label, value, alertColor, to }: Counter) {
  const theme = useMantineTheme()
  const color = value > 0 && alertColor ? getThemeColor(alertColor, theme) : undefined
  const content = (
    <Paper
      withBorder
      p="md"
      h="100%"
      role="group"
      aria-label={label}
      className={classes.card}
      style={{ '--counter-accent': color }}
    >
      <Text size="sm" c="dimmed" fw={500}>
        {label}
      </Text>
      <Group justify="space-between" align="flex-end" gap="xs" mt={4}>
        <Text fz={30} fw={700} lh={1.1} c={color}>
          {value}
        </Text>
        {to && (
          <Text size="xs" c="indigo" fw={500} className={classes.hint}>
            Listeyi aç
          </Text>
        )}
      </Group>
    </Paper>
  )
  if (!to) return content
  return (
    <UnstyledButton
      component={Link}
      to={to}
      className={classes.link}
      aria-label={`${label}: ${value}, listeyi aç`}
    >
      {content}
    </UnstyledButton>
  )
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
      {counters.map((counter) => (
        <CounterCard key={counter.label} {...counter} />
      ))}
    </SimpleGrid>
  )
}

interface TrackingCardsProps {
  summary: DashboardSummary
  /** Yil / hafta secili mi: takip sayaclari bu secimden etkilenmez, bu acikca soylenir. */
  hasWeekReference: boolean
}

/**
 * Risk, engel, karar ve aksiyon sayaclari. Rapor sayaclarindan (ustteki "Yuksek riskli" raporun risk seviyesidir)
 * ayri bir baslik altinda durur. Her kart sayilan kayitlarin listesine gider.
 */
export function TrackingCards({ summary, hasWeekReference }: TrackingCardsProps) {
  const list = (query: string) => `/tracking-items?${query}`
  const counters: Counter[] = [
    { label: 'Açık risk', value: summary.openRisks, alertColor: 'red', to: list('type=RISK') },
    { label: 'Açık engel', value: summary.openBlockers, alertColor: 'red', to: list('type=ENGEL') },
    { label: 'Bekleyen karar', value: summary.pendingDecisions, alertColor: 'grape', to: list('type=KARAR') },
    { label: 'Gecikmiş takip', value: summary.overdueTrackingItems, alertColor: 'orange', to: list('overdue=true') },
  ]

  return (
    <Stack gap="xs">
      <Group gap="sm" align="baseline">
        <Title order={4}>Riskler ve aksiyonlar</Title>
        <Text size="sm" c="dimmed">
          {hasWeekReference ? 'Bugünkü durum; seçilen yıl ve hafta bu sayıları değiştirmez.' : 'Bugünkü durum.'}
        </Text>
      </Group>
      <SimpleGrid cols={{ base: 2, lg: 4 }} spacing="sm">
        {counters.map((counter) => (
          <CounterCard key={counter.label} {...counter} />
        ))}
      </SimpleGrid>
    </Stack>
  )
}
