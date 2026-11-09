import { AppShell, Container, Group, Text, Title } from '@mantine/core'
import { Link, Outlet } from 'react-router'

/** Tum sayfalarin ortak cercevesi. Gezinme menusu ekranlar eklendikce (Faz 1) buraya gelecek. */
export function AppLayout() {
  return (
    <AppShell header={{ height: 56 }} padding="md">
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Title order={3} renderRoot={(props) => <Link to="/" {...props} />} c="inherit" td="none">
            CTO Takip
          </Title>
          <Text size="sm" c="dimmed">
            Haftalık proje durum raporlama
          </Text>
        </Group>
      </AppShell.Header>

      <AppShell.Main>
        <Container size="lg">
          <Outlet />
        </Container>
      </AppShell.Main>
    </AppShell>
  )
}
