import { Alert, Badge, Button, Card, Group, Loader, Stack, Text, Title } from '@mantine/core'

import { ApiRequestError } from '../api/client'
import { useHealth } from '../api/health'

const dateTime = new Intl.DateTimeFormat('tr-TR', { dateStyle: 'medium', timeStyle: 'medium' })

/** Gecici ana sayfa: frontend ile backend arasindaki baglantiyi uctan uca gosterir (Faz 0). */
export function HomePage() {
  return (
    <Stack gap="lg">
      <Title order={2}>Genel bakış</Title>
      <BackendStatusCard />
    </Stack>
  )
}

function BackendStatusCard() {
  const health = useHealth()

  return (
    <Card withBorder padding="lg" maw={520}>
      <Group justify="space-between" mb="sm">
        <Text fw={600}>Backend bağlantısı</Text>
        {health.isSuccess && (
          <Badge color={health.data.status === 'UP' ? 'teal' : 'red'}>{health.data.status}</Badge>
        )}
      </Group>

      {health.isPending && (
        <Group gap="xs">
          <Loader size="sm" />
          <Text size="sm">Kontrol ediliyor…</Text>
        </Group>
      )}

      {health.isError && (
        <Alert color="red" title="Backend'e ulaşılamadı">
          <Stack gap="xs" align="flex-start">
            <Text size="sm">
              {health.error instanceof ApiRequestError ? health.error.message : 'Beklenmeyen bir hata oluştu.'}
            </Text>
            <Button size="xs" variant="light" color="red" onClick={() => health.refetch()} loading={health.isFetching}>
              Tekrar dene
            </Button>
          </Stack>
        </Alert>
      )}

      {health.isSuccess && (
        <Stack gap={4}>
          <Text size="sm">
            Veritabanı: {health.data.database} ({health.data.databaseStatus})
          </Text>
          <Text size="sm" c="dimmed">
            Son kontrol: {dateTime.format(new Date(health.data.timestamp))}
          </Text>
        </Stack>
      )}
    </Card>
  )
}
