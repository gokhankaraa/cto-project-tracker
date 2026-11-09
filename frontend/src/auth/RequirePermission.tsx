import { Anchor, Stack, Text, Title } from '@mantine/core'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

import { usePermissions, type Permission } from './permissions'

/**
 * Yazma formlarinin korumasi. Butonlar yetkisi olmayana zaten gorunmez; bu, adresi elle yazana
 * (or. CTO'nun /projects/new acmasi) bos bir form yerine aciklama gosterir.
 */
export function RequirePermission({ permission, children }: { permission: Permission; children: ReactNode }) {
  const permissions = usePermissions()
  if (permissions[permission]) return children

  return (
    <Stack gap="sm">
      <Title order={2}>Yetkiniz yok</Title>
      <Text c="dimmed">Bu işlem rolünüz için kapalı. Kayıtları görüntüleyebilirsiniz.</Text>
      <Anchor component={Link} to="/">
        Ana sayfaya dön
      </Anchor>
    </Stack>
  )
}
