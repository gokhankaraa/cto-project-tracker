import { AppShell, Burger, Group, NavLink, Text, Title } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { Link, Outlet, useLocation } from 'react-router'

interface NavItem {
  to: string
  label: string
  /** Ekran henuz hazir degilse menude pasif gorunur (Faz 1 adim adim tamamlaniyor). */
  ready: boolean
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard', ready: true },
  { to: '/projects', label: 'Projeler', ready: false },
  { to: '/reports', label: 'Raporlar', ready: false },
  { to: '/work-items', label: 'İş kalemleri', ready: false },
]

/** Menu ogesi, adresi kendisiyle ya da alt sayfasiyla (/projects/3) eslesince aktif gorunur. */
function isActive(pathname: string, to: string) {
  return to === '/' ? pathname === '/' : pathname === to || pathname.startsWith(`${to}/`)
}

/** Tum sayfalarin ortak cercevesi: ust bar ve sol menu (dar ekranda acilir menu). */
export function AppLayout() {
  const [opened, { toggle, close }] = useDisclosure()
  const { pathname } = useLocation()

  return (
    <AppShell
      header={{ height: 56 }}
      navbar={{ width: 220, breakpoint: 'sm', collapsed: { mobile: !opened } }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group gap="sm">
            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" aria-label="Menüyü aç/kapat" />
            <Title order={3} renderRoot={(props) => <Link to="/" {...props} />} c="inherit" td="none">
              CTO Takip
            </Title>
          </Group>
          <Text size="sm" c="dimmed" visibleFrom="sm">
            Haftalık proje durum raporlama
          </Text>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="xs" aria-label="Ana menü">
        {NAV_ITEMS.map((item) =>
          item.ready ? (
            <NavLink
              key={item.to}
              component={Link}
              to={item.to}
              label={item.label}
              active={isActive(pathname, item.to)}
              onClick={close}
            />
          ) : (
            <NavLink key={item.to} label={item.label} description="Yakında" disabled />
          ),
        )}
      </AppShell.Navbar>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  )
}
