import { AppShell, Box, Burger, Button, Group, NavLink, Text, Title } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { useQueryClient } from '@tanstack/react-query'
import { Link, Outlet, useLocation } from 'react-router'

import { endSession } from '../auth/session'
import { useSession } from '../auth/useSession'
import { BrandMark } from '../components/BrandMark'
import { EnumBadge } from '../components/EnumBadge'
import { ROLE_LABELS } from '../lib/labels'
import { BackendStatus } from './BackendStatus'

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard' },
  { to: '/projects', label: 'Projeler' },
  { to: '/reports', label: 'Raporlar' },
  { to: '/work-items', label: 'İş kalemleri' },
  { to: '/tracking-items', label: 'Riskler ve aksiyonlar' },
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
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" aria-label="Menüyü aç/kapat" />
            <Group
              gap={10}
              wrap="nowrap"
              renderRoot={(props) => <Link to="/" {...props} />}
              c="inherit"
              td="none"
            >
              <BrandMark size={28} />
              <Title order={3} fz={18} component="span">
                CTO Takip Sistemi
              </Title>
            </Group>
          </Group>
          <Group gap="md" wrap="nowrap">
            {/* Dar ekranda ust bar tek satirda kalsin diye baglanti gostergesi gizlenir. */}
            <Box visibleFrom="sm">
              <BackendStatus />
            </Box>
            <UserMenu />
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="sm" aria-label="Ana menü">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            component={Link}
            to={item.to}
            label={item.label}
            active={isActive(pathname, item.to)}
            onClick={close}
          />
        ))}
      </AppShell.Navbar>

      {/* Zemin rengi temada (dark.9): kartlar (Paper) koyu zeminden bir ton acik durur. */}
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  )
}

/** Ust bardaki oturum bilgisi: kullanicinin adi, rolu ve cikis. */
function UserMenu() {
  const session = useSession()
  const queryClient = useQueryClient()
  if (!session) return null

  const logout = () => {
    // Once oturum kapanir (sonraki istekler token'siz gider), sonra bu kullanicinin verisi onbellekten silinir.
    endSession('logout')
    queryClient.clear()
  }

  return (
    <Group gap="xs" wrap="nowrap">
      <Text size="sm" fw={500} visibleFrom="xs">
        {session.user.fullName}
      </Text>
      <EnumBadge value={session.user.role} labels={ROLE_LABELS} visibleFrom="xs" />
      <Button variant="subtle" size="compact-sm" onClick={logout}>
        Çıkış
      </Button>
    </Group>
  )
}
