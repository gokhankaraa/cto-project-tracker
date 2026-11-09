import { Alert, Button, Group, Modal, Select, SimpleGrid, Stack, Textarea, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useState } from 'react'

import { ApiRequestError, errorMessage } from '../../api/client'
import { WORK_ITEM_STATUSES, type WorkItem, type WorkItemStatus } from '../../api/types'
import { WORK_ITEM_LIMITS, type WorkItemRequest } from '../../api/workItems'
import { isoWeekOf } from '../../lib/isoWeek'
import { enumOptions, WORK_ITEM_STATUS_LABELS } from '../../lib/labels'

interface FormValues {
  title: string
  assignee: string
  status: WorkItemStatus | null
  plannedDate: string
  completedDate: string
  description: string
}

const maxLength = (limit: number, label: string) => (value: string) =>
  value.trim().length > limit ? `${label} en fazla ${limit} karakter olabilir.` : null

const validDate = (value: string) => (value === '' || isoWeekOf(value) !== null ? null : 'Geçerli bir tarih girin.')

function toRequest(values: FormValues): WorkItemRequest {
  const optional = (value: string) => (value.trim() === '' ? null : value.trim())
  return {
    title: values.title.trim(),
    description: optional(values.description),
    assignee: optional(values.assignee),
    status: values.status!,
    plannedDate: optional(values.plannedDate),
    completedDate: optional(values.completedDate),
  }
}

interface WorkItemFormModalProps {
  opened: boolean
  onClose: () => void
  /** Verilirse duzenleme. */
  workItem?: WorkItem
  /** Kaydi gonderir; hata olursa reddedilen Promise doner (alan hatalari forma yazilir). */
  onSubmit: (request: WorkItemRequest) => Promise<unknown>
}

/**
 * Is kalemi formu (pencere icinde). Rapor sayfasindan ayrilmadan ekleme/duzenleme yapilir.
 * Pencere her acilista yeniden olusur (key), boylece onceki kaydin degerleri ve hatalari kalmaz.
 */
export function WorkItemFormModal({ opened, onClose, workItem, onSubmit }: WorkItemFormModalProps) {
  return (
    <Modal opened={opened} onClose={onClose} title={workItem ? 'İş kalemini düzenle' : 'Yeni iş kalemi'} size="lg" centered>
      {opened && <WorkItemForm key={workItem?.id ?? 'new'} workItem={workItem} onSubmit={onSubmit} onCancel={onClose} />}
    </Modal>
  )
}

function WorkItemForm({
  workItem,
  onSubmit,
  onCancel,
}: {
  workItem?: WorkItem
  onSubmit: (request: WorkItemRequest) => Promise<unknown>
  onCancel: () => void
}) {
  const form = useForm<FormValues>({
    initialValues: {
      title: workItem?.title ?? '',
      assignee: workItem?.assignee ?? '',
      status: workItem?.status ?? 'PLANLANDI',
      plannedDate: workItem?.plannedDate ?? '',
      completedDate: workItem?.completedDate ?? '',
      description: workItem?.description ?? '',
    },
    validate: {
      title: (value) =>
        value.trim() === '' ? 'Başlık zorunludur.' : maxLength(WORK_ITEM_LIMITS.title, 'Başlık')(value),
      assignee: maxLength(WORK_ITEM_LIMITS.assignee, 'Sorumlu'),
      status: (value) => (value === null ? 'Durum zorunludur.' : null),
      plannedDate: validDate,
      completedDate: validDate,
      description: maxLength(WORK_ITEM_LIMITS.description, 'Açıklama'),
    },
  })

  // Alan hatasi tasimayan hata (or. rapor silinmis: 404) formun ustunde gosterilir.
  const [generalError, setGeneralError] = useState<string | null>(null)

  const submit = form.onSubmit(async (values) => {
    setGeneralError(null)
    try {
      await onSubmit(toRequest(values))
    } catch (error) {
      if (error instanceof ApiRequestError && error.fieldErrors.length > 0) form.setErrors(error.fieldErrorMap())
      else setGeneralError(errorMessage(error))
    }
  })

  return (
    <form onSubmit={submit} noValidate>
      <Stack gap="md">
        {generalError && (
          <Alert color="red" title="İş kalemi kaydedilemedi">
            {generalError}
          </Alert>
        )}
        <TextInput label="Başlık" withAsterisk data-autofocus {...form.getInputProps('title')} />
        <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
          <TextInput label="Sorumlu" placeholder="Ad soyad" {...form.getInputProps('assignee')} />
          <Select
            label="Durum"
            withAsterisk
            data={enumOptions(WORK_ITEM_STATUSES, WORK_ITEM_STATUS_LABELS)}
            allowDeselect={false}
            {...form.getInputProps('status')}
          />
          <TextInput type="date" label="Planlanan tarih" {...form.getInputProps('plannedDate')} />
          <TextInput type="date" label="Tamamlanma tarihi" {...form.getInputProps('completedDate')} />
        </SimpleGrid>
        <Textarea label="Açıklama" autosize minRows={2} maxRows={8} {...form.getInputProps('description')} />
        <Group justify="flex-end">
          <Button variant="default" onClick={onCancel}>
            Vazgeç
          </Button>
          <Button type="submit" loading={form.submitting}>
            {workItem ? 'Kaydet' : 'Ekle'}
          </Button>
        </Group>
      </Stack>
    </form>
  )
}
