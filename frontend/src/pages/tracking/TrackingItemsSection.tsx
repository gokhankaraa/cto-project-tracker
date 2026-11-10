import { Alert, Button, Group, Modal, Stack, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'

import { errorMessage } from '../../api/client'
import {
  useCreateTrackingItem,
  useDeleteTrackingItem,
  useProjectTrackingItems,
  useUpdateTrackingItem,
  type TrackingItemRequest,
} from '../../api/trackingItems'
import type { TrackingItem } from '../../api/types'
import { usePermissions } from '../../auth/permissions'
import { QueryState } from '../../components/QueryState'
import { TrackingItemFormModal } from './TrackingItemFormModal'
import { TrackingItemTable } from './TrackingItemTable'

/** Pencerelerin durumu: kapali, yeni kayit, duzenleme ya da silme onayi. */
type Dialog =
  | { kind: 'closed' }
  | { kind: 'create' }
  | { kind: 'edit'; item: TrackingItem }
  | { kind: 'delete'; item: TrackingItem }

/** Proje sayfasindaki risk, engel, karar ve aksiyon kayitlari: tablo, ekleme/duzenleme penceresi, silme onayi. */
export function TrackingItemsSection({ projectId }: { projectId: number }) {
  const items = useProjectTrackingItems(projectId)
  const create = useCreateTrackingItem(projectId)
  const update = useUpdateTrackingItem(projectId)
  const [dialog, setDialog] = useState<Dialog>({ kind: 'closed' })
  const canWrite = usePermissions().writeReports
  const close = () => setDialog({ kind: 'closed' })

  const save = async (request: TrackingItemRequest) => {
    if (dialog.kind === 'edit') await update.mutateAsync({ id: dialog.item.id, request })
    else await create.mutateAsync(request)
    notifications.show({ color: 'teal', message: dialog.kind === 'edit' ? 'Kayıt güncellendi.' : 'Kayıt eklendi.' })
    close()
  }

  return (
    <Stack gap="sm">
      <Group justify="space-between">
        <Title order={3}>Riskler ve aksiyonlar</Title>
        {canWrite && <Button onClick={() => setDialog({ kind: 'create' })}>Kayıt ekle</Button>}
      </Group>

      <QueryState query={items}>
        {(list) => (
          <TrackingItemTable
            items={list}
            emptyText={
              canWrite
                ? 'Bu projede risk, engel, karar ya da aksiyon kaydı yok. Eklemek için "Kayıt ekle"yi kullanın.'
                : 'Bu projede risk, engel, karar ya da aksiyon kaydı yok.'
            }
            actions={
              canWrite
                ? {
                    onEdit: (item) => setDialog({ kind: 'edit', item }),
                    onDelete: (item) => setDialog({ kind: 'delete', item }),
                  }
                : undefined
            }
          />
        )}
      </QueryState>

      <TrackingItemFormModal
        opened={dialog.kind === 'create' || dialog.kind === 'edit'}
        item={dialog.kind === 'edit' ? dialog.item : undefined}
        onClose={close}
        onSubmit={save}
      />
      <DeleteTrackingItemModal
        projectId={projectId}
        item={dialog.kind === 'delete' ? dialog.item : null}
        onClose={close}
      />
    </Stack>
  )
}

function DeleteTrackingItemModal({
  projectId,
  item,
  onClose,
}: {
  projectId: number
  item: TrackingItem | null
  onClose: () => void
}) {
  const deletion = useDeleteTrackingItem(projectId)

  const closeModal = () => {
    onClose()
    deletion.reset()
  }

  const confirm = () =>
    item &&
    deletion.mutate(item.id, {
      onSuccess: () => {
        notifications.show({ color: 'teal', message: 'Kayıt silindi.' })
        closeModal()
      },
    })

  return (
    <Modal opened={item !== null} onClose={closeModal} title="Kaydı sil" centered>
      <Stack gap="md">
        <Text size="sm">
          <b>{item?.title}</b> silinecek. Bu işlem geri alınamaz; kaydı yalnızca kapatmak için "Düzenle"den durumunu
          değiştirin.
        </Text>
        {deletion.isError && (
          <Alert color="red" title="Kayıt silinemedi">
            {errorMessage(deletion.error)}
          </Alert>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={closeModal}>
            Vazgeç
          </Button>
          <Button color="red" onClick={confirm} loading={deletion.isPending} disabled={deletion.isError}>
            Sil
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
