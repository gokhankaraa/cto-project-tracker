import { Anchor, Badge, Button, Group, Paper, Stack, Table, Text } from '@mantine/core'
import { Link } from 'react-router'

import type { TrackingItem } from '../../api/types'
import { EnumBadge } from '../../components/EnumBadge'
import { formatDate } from '../../lib/format'
import { RISK_LEVEL_LABELS, TRACKING_ITEM_TYPE_LABELS, trackingStatusLabels } from '../../lib/labels'

export interface TrackingRowActions {
  onEdit: (item: TrackingItem) => void
  onDelete: (item: TrackingItem) => void
}

interface TrackingItemTableProps {
  items: TrackingItem[]
  /** Projeler arasi listede proje sutunu gosterilir. */
  showProject?: boolean
  /** Yazma izni yoksa verilmez: satir islemleri ve sutunu hic cizilmez. */
  actions?: TrackingRowActions
  /** Satir yokken gosterilecek metin. */
  emptyText: string
}

/** Risk, engel, karar ve aksiyon tablosu (proje sayfasi ve projeler arasi liste). */
export function TrackingItemTable({ items, showProject = false, actions, emptyText }: TrackingItemTableProps) {
  const columnCount = 6 + (showProject ? 1 : 0) + (actions ? 1 : 0)
  return (
    <Paper withBorder>
      <Table.ScrollContainer minWidth={showProject ? 980 : 880}>
        <Table striped highlightOnHover verticalSpacing="sm">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Tür</Table.Th>
              <Table.Th>Başlık</Table.Th>
              {showProject && <Table.Th>Proje</Table.Th>}
              <Table.Th>Risk</Table.Th>
              <Table.Th>Sorumlu</Table.Th>
              <Table.Th>Takip tarihi</Table.Th>
              <Table.Th>Durum</Table.Th>
              {actions && <Table.Th />}
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {items.length === 0 ? (
              <Table.Tr>
                <Table.Td colSpan={columnCount}>
                  <Text c="dimmed" ta="center" py="md">
                    {emptyText}
                  </Text>
                </Table.Td>
              </Table.Tr>
            ) : (
              items.map((item) => (
                <Table.Tr key={item.id} c={item.status === 'KAPANDI' ? 'dimmed' : undefined}>
                  <Table.Td>
                    <EnumBadge value={item.type} labels={TRACKING_ITEM_TYPE_LABELS} />
                  </Table.Td>
                  <Table.Td maw={360}>
                    <TitleCell item={item} />
                  </Table.Td>
                  {showProject && (
                    <Table.Td style={{ whiteSpace: 'nowrap' }}>
                      <Anchor component={Link} to={`/projects/${item.projectId}`} size="sm">
                        {item.projectName}
                      </Anchor>
                    </Table.Td>
                  )}
                  <Table.Td>
                    <EnumBadge value={item.riskLevel} labels={RISK_LEVEL_LABELS} />
                  </Table.Td>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>{item.assignee ?? '–'}</Table.Td>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>
                    <FollowUpCell item={item} />
                  </Table.Td>
                  <Table.Td>
                    <EnumBadge value={item.status} labels={trackingStatusLabels(item.type)} />
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
              ))
            )}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  )
}

/** Baslik; etki ve aksiyon plani altinda (CTO kaydi acmadan ne oldugunu ve ne yapilacagini gorur). */
function TitleCell({ item }: { item: TrackingItem }) {
  return (
    <Stack gap={2}>
      <Text size="sm" fw={500}>
        {item.title}
      </Text>
      {item.impact && (
        <Text size="xs" c="dimmed" lineClamp={2}>
          Etki: {item.impact}
        </Text>
      )}
      {item.actionPlan && (
        <Text size="xs" c="dimmed" lineClamp={2}>
          Aksiyon: {item.actionPlan}
        </Text>
      )}
    </Stack>
  )
}

/** Takip tarihi; gecikmisse rozetle. Kapali kayitta kapanis tarihi gosterilir. */
function FollowUpCell({ item }: { item: TrackingItem }) {
  if (item.status === 'KAPANDI') {
    return <Text size="sm">{item.closedDate ? `Kapandı: ${formatDate(item.closedDate)}` : '–'}</Text>
  }
  return (
    <Group gap={6} wrap="nowrap">
      <Text size="sm">{formatDate(item.followUpDate)}</Text>
      {item.overdue && (
        <Badge color="orange" variant="filled" miw="max-content">
          Gecikmede
        </Badge>
      )}
    </Group>
  )
}
