import { Anchor, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <Stack gap="sm">
      <Title order={2}>Sayfa bulunamadı</Title>
      <Text c="dimmed">Aradığınız sayfa yok ya da taşınmış olabilir.</Text>
      <Anchor component={Link} to="/">
        Ana sayfaya dön
      </Anchor>
    </Stack>
  )
}
