import { Anchor, Group, Stack, Text, Title } from '@mantine/core'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

interface PageHeaderProps {
  title: ReactNode
  description?: ReactNode
  /** Ust seviye sayfaya donus baglantisi (or. detaydan listeye). */
  back?: { to: string; label: string }
  /** Sagdaki dugmeler; dar ekranda basligin altina iner. */
  actions?: ReactNode
}

/** Tum sayfalarda ayni baslik duzeni: geri baglantisi, baslik, aciklama ve eylemler. */
export function PageHeader({ title, description, back, actions }: PageHeaderProps) {
  return (
    <Stack gap={4}>
      {back && (
        <Anchor component={Link} to={back.to} size="sm">
          ← {back.label}
        </Anchor>
      )}
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>{title}</Title>
          {description && (
            <Text size="sm" c="dimmed">
              {description}
            </Text>
          )}
        </div>
        {actions && <Group gap="xs">{actions}</Group>}
      </Group>
    </Stack>
  )
}
