import { Alert, Badge, Button, Group, Modal, Paper, Stack, Table, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'

import { errorMessage } from '../../api/client'
import type { WeeklyReport, WorkItem } from '../../api/types'
import {
  useCreateWorkItem,
  useDeleteWorkItem,
  useReportWorkItems,
  useUpdateWorkItem,
  type WorkItemRequest,
} from '../../api/workItems'
import { usePermissions } from '../../auth/permissions'
import { EnumBadge } from '../../components/EnumBadge'
import { QueryState } from '../../components/QueryState'
import { formatDate } from '../../lib/format'
import { WORK_ITEM_STATUS_LABELS } from '../../lib/labels'
import { WorkItemFormModal } from './WorkItemFormModal'

/** Pencerelerin durumu: kapali, yeni kayit, duzenleme ya da silme onayi. */
type Dialog = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; item: WorkItem } | { kind: 'delete'; item: WorkItem }

/** Rapor sayfasindaki is kalemleri: tablo, ekleme/duzenleme penceresi ve silme onayi. */
export function WorkItemsSection({ report }: { report: WeeklyReport }) {
  const workItems = useReportWorkItems(report.id)
  const create = useCreateWorkItem(report.id, report.projectId)
  const update = useUpdateWorkItem(report.id, report.projectId)
  const [dialog, setDialog] = useState<Dialog>({ kind: 'closed' })
  const canWrite = usePermissions().writeReports
  const close = () => setDialog({ kind: 'closed' })

  const save = async (request: WorkItemRequest) => {
    if (dialog.kind === 'edit') await update.mutateAsync({ id: dialog.item.id, request })
    else await create.mutateAsync(request)
    notifications.show({ color: 'teal', message: dialog.kind === 'edit' ? 'İş kalemi güncellendi.' : 'İş kalemi eklendi.' })
    close()
  }

  return (
    <Stack gap="sm">
      <Group justify="space-between">
        <Title order={3}>İş kalemleri</Title>
        {canWrite && <Button onClick={() => setDialog({ kind: 'create' })}>İş kalemi ekle</Button>}
      </Group>

      <QueryState query={workItems} isEmpty={(items) => items.length === 0} empty="Bu raporda iş kalemi yok.">
        {(items) => (
          <WorkItemTable
            items={items}
            // Yazma izni yoksa satir islemleri ve onlarin sutunu hic cizilmez.
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

      <WorkItemFormModal
        opened={dialog.kind === 'create' || dialog.kind === 'edit'}
        workItem={dialog.kind === 'edit' ? dialog.item : undefined}
        onClose={close}
        onSubmit={save}
      />
      <DeleteWorkItemModal report={report} item={dialog.kind === 'delete' ? dialog.item : null} onClose={close} />
    </Stack>
  )
}

interface RowActions {
  onEdit: (item: WorkItem) => void
  onDelete: (item: WorkItem) => void
}

function WorkItemTable({ items, actions }: { items: WorkItem[]; actions?: RowActions }) {
  return (
    <Paper withBorder>
      <Table.ScrollContainer minWidth={820}>
        <Table striped highlightOnHover verticalSpacing="sm">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Başlık</Table.Th>
              <Table.Th>Sorumlu</Table.Th>
              <Table.Th>Durum</Table.Th>
              <Table.Th>Planlanan</Table.Th>
              <Table.Th>Tamamlanan</Table.Th>
              <Table.Th>Gecikme</Table.Th>
              {actions && <Table.Th />}
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {items.map((item) => (
              <Table.Tr key={item.id}>
                <Table.Td maw={320}>
                  <Text size="sm" fw={500}>
                    {item.title}
                  </Text>
                  {item.description && (
                    <Text size="xs" c="dimmed" lineClamp={2}>
                      {item.description}
                    </Text>
                  )}
                </Table.Td>
                <Table.Td style={{ whiteSpace: 'nowrap' }}>{item.assignee ?? '–'}</Table.Td>
                <Table.Td>
                  <EnumBadge value={item.status} labels={WORK_ITEM_STATUS_LABELS} />
                </Table.Td>
                <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDate(item.plannedDate)}</Table.Td>
                <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDate(item.completedDate)}</Table.Td>
                <Table.Td>
                  {item.delayed ? (
                    <Badge color="orange" variant="filled" miw="max-content">
                      Gecikmede
                    </Badge>
                  ) : (
                    '–'
                  )}
                </Table.Td>
                {actions && (
                  <Table.Td>
                    <Group gap={4} justify="flex-end" wrap="nowrap">
                      <Button
                        size="compact-sm"
                        variant="subtle"
                        onClick={() => actions.onEdit(item)}
                        aria-label={`${item.title} düzenle`}
                      >
                        Düzenle
                      </Button>
                      <Button
                        size="compact-sm"
                        variant="subtle"
                        color="red"
                        onClick={() => actions.onDelete(item)}
                        aria-label={`${item.title} sil`}
                      >
                        Sil
                      </Button>
                    </Group>
                  </Table.Td>
                )}
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  )
}

function DeleteWorkItemModal({
  report,
  item,
  onClose,
}: {
  report: WeeklyReport
  item: WorkItem | null
  onClose: () => void
}) {
  const deletion = useDeleteWorkItem(report.id, report.projectId)

  const closeModal = () => {
    onClose()
    deletion.reset()
  }

  const confirm = () =>
    item &&
    deletion.mutate(item.id, {
      onSuccess: () => {
        notifications.show({ color: 'teal', message: 'İş kalemi silindi.' })
        closeModal()
      },
    })

  return (
    <Modal opened={item !== null} onClose={closeModal} title="İş kalemini sil" centered>
      <Stack gap="md">
        <Text size="sm">
          <b>{item?.title}</b> silinecek. Bu işlem geri alınamaz.
        </Text>
        {deletion.isError && (
          <Alert color="red" title="İş kalemi silinemedi">
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
