import { Alert, Button, Group, Paper, Select, Stack, Text, Timeline, Title, VisuallyHidden } from '@mantine/core'
import { useState } from 'react'

import { errorMessage } from '../../../api/client'
import { useProjectHistory } from '../../../api/history'
import { AUDIT_ENTITY_TYPES, type AuditChange, type AuditEntityType, type AuditEvent } from '../../../api/types'
import { EnumBadge } from '../../../components/EnumBadge'
import { QueryState } from '../../../components/QueryState'
import { formatDateTime } from '../../../lib/format'
import { AUDIT_ACTION_LABELS, AUDIT_ENTITY_TYPE_LABELS, enumOptions } from '../../../lib/labels'
import { fieldLabel, formatValue } from './historyFormat'

const TYPE_OPTIONS = enumOptions(AUDIT_ENTITY_TYPES, AUDIT_ENTITY_TYPE_LABELS)

/**
 * Proje sayfasindaki degisiklik gecmisi: projeye, raporlarina, is kalemlerine ve risk / aksiyon kayitlarina kim,
 * ne zaman, ne yapti. En yeni ustte; "Daha fazla goster" sonraki sayfayi ekler. Projeyi goren herkes gorur.
 */
export function ProjectHistorySection({ projectId }: { projectId: number }) {
  const [entityType, setEntityType] = useState<AuditEntityType | null>(null)
  const history = useProjectHistory(projectId, entityType)

  return (
    <Stack gap="sm">
      <Group justify="space-between" align="flex-end" wrap="wrap">
        <div>
          <Title order={3}>Değişiklik geçmişi</Title>
          <Text size="sm" c="dimmed">
            Kim, neyi, ne zaman değiştirdi. En yeni değişiklik üstte.
          </Text>
        </div>
        <Select
          label="Kayıt türü"
          placeholder="Tümü"
          data={TYPE_OPTIONS}
          value={entityType}
          onChange={(value) => setEntityType(value as AuditEntityType | null)}
          clearable
          w={200}
        />
      </Group>

      <QueryState
        query={history}
        isEmpty={(data) => data.pages[0].totalElements === 0}
        empty={
          entityType === null
            ? 'Bu projede henüz değişiklik kaydı yok. Proje, raporları, iş kalemleri ya da riskleri ve aksiyonları ' +
              'değiştirildiğinde burada görünür. Demo verisi geçmişe yazılmaz.'
            : `Bu projede "${AUDIT_ENTITY_TYPE_LABELS[entityType].label}" türünde değişiklik yok.`
        }
      >
        {(data) => {
          const events = data.pages.flatMap((page) => page.content)
          const total = data.pages[0].totalElements
          return (
            <Stack gap="sm">
              <Paper withBorder p="md">
                {/* Timeline div'lerden olusur; ekran okuyucu gecmisi liste olarak duysun. */}
                <Timeline bulletSize={12} lineWidth={2} role="list" aria-label="Değişiklikler">
                  {events.map((event) => (
                    <Timeline.Item key={event.id} role="listitem" title={<EventTitle event={event} />}>
                      <EventBody event={event} />
                    </Timeline.Item>
                  ))}
                </Timeline>
              </Paper>

              {history.isFetchNextPageError && (
                <Alert color="red" title="Sonraki değişiklikler yüklenemedi">
                  {errorMessage(history.error)}
                </Alert>
              )}
              <Group justify="space-between">
                <Text size="sm" c="dimmed">
                  {events.length} / {total} değişiklik gösteriliyor
                </Text>
                {history.hasNextPage && (
                  <Button
                    variant="default"
                    onClick={() => void history.fetchNextPage()}
                    loading={history.isFetchingNextPage}
                  >
                    Daha fazla göster
                  </Button>
                )}
              </Group>
            </Stack>
          )
        }}
      </QueryState>
    </Stack>
  )
}

function EventTitle({ event }: { event: AuditEvent }) {
  return (
    <Group gap="xs" wrap="nowrap" align="flex-start">
      <EnumBadge value={event.action} labels={AUDIT_ACTION_LABELS} size="sm" />
      <Text size="sm" fw={600}>
        {AUDIT_ENTITY_TYPE_LABELS[event.entityType].label}: {event.summary}
      </Text>
    </Group>
  )
}

function EventBody({ event }: { event: AuditEvent }) {
  return (
    <Stack gap={4} mt={4}>
      <Text size="xs" c="dimmed">
        {event.actorName}, <time dateTime={event.occurredAt}>{formatDateTime(event.occurredAt)}</time>
      </Text>
      {event.changes.map((change) => (
        <ChangeRow
          key={change.field}
          entityType={event.entityType}
          change={change}
          showOld={event.action === 'GUNCELLENDI'}
        />
      ))}
    </Stack>
  )
}

/**
 * "Risk seviyesi: Orta -> Yuksek". Eklemede yalnizca yeni deger gorunur. Bosaltilan alan "boş" yazar. Uzun metin
 * uc satirda kesilir (tam metin kaydin kendisinde).
 */
function ChangeRow({ entityType, change, showOld }: { entityType: AuditEntityType; change: AuditChange; showOld: boolean }) {
  const oldValue = formatValue(entityType, change.field, change.oldValue)
  const newValue = formatValue(entityType, change.field, change.newValue)
  return (
    <Text size="sm" lineClamp={3}>
      <Text span c="dimmed">
        {fieldLabel(entityType, change.field)}:
      </Text>{' '}
      {showOld && (
        <>
          <Text span c="dimmed" td="line-through">
            {oldValue ?? 'boş'}
          </Text>
          <span aria-hidden="true"> → </span>
          <VisuallyHidden>, yeni değer: </VisuallyHidden>
        </>
      )}
      <Text span fw={500}>
        {newValue ?? 'boş'}
      </Text>
    </Text>
  )
}
