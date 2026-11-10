import {
  Alert,
  Button,
  Group,
  Input,
  Modal,
  SegmentedControl,
  Select,
  SimpleGrid,
  Stack,
  Textarea,
  TextInput,
} from '@mantine/core'
import { useForm } from '@mantine/form'
import { useState } from 'react'

import { ApiRequestError, errorMessage } from '../../api/client'
import { hasRiskLevel, TRACKING_ITEM_LIMITS, type TrackingItemRequest } from '../../api/trackingItems'
import {
  RISK_LEVELS,
  TRACKING_ITEM_STATUSES,
  TRACKING_ITEM_TYPES,
  type RiskLevel,
  type TrackingItem,
  type TrackingItemStatus,
  type TrackingItemType,
} from '../../api/types'
import { isoWeekOf, todayIso } from '../../lib/isoWeek'
import { enumOptions, RISK_LEVEL_LABELS, TRACKING_ITEM_TYPE_LABELS, trackingStatusLabels } from '../../lib/labels'

interface FormValues {
  type: TrackingItemType
  title: string
  riskLevel: RiskLevel | null
  impact: string
  actionPlan: string
  assignee: string
  followUpDate: string
  status: TrackingItemStatus
  description: string
}

const isOpen = (status: TrackingItemStatus) => status !== 'KAPANDI'

const maxLength = (limit: number, label: string) => (value: string) =>
  value.trim().length > limit ? `${label} en fazla ${limit} karakter olabilir.` : null

function toRequest(values: FormValues): TrackingItemRequest {
  const optional = (value: string) => (value.trim() === '' ? null : value.trim())
  return {
    type: values.type,
    title: values.title.trim(),
    description: optional(values.description),
    // Tur risk seviyesi tasimiyorsa onceki secim gonderilmez (backend reddederdi).
    riskLevel: hasRiskLevel(values.type) ? values.riskLevel : null,
    impact: optional(values.impact),
    actionPlan: optional(values.actionPlan),
    assignee: optional(values.assignee),
    followUpDate: optional(values.followUpDate),
    status: values.status,
  }
}

interface TrackingItemFormModalProps {
  opened: boolean
  onClose: () => void
  /** Verilirse duzenleme. */
  item?: TrackingItem
  /** Kaydi gonderir; hata olursa reddedilen Promise doner (alan hatalari forma yazilir). */
  onSubmit: (request: TrackingItemRequest) => Promise<unknown>
}

/**
 * Risk, engel, karar ya da aksiyon formu (pencere icinde). Pencere her acilista yeniden olusur (key), boylece
 * onceki kaydin degerleri ve hatalari kalmaz.
 */
export function TrackingItemFormModal({ opened, onClose, item, onSubmit }: TrackingItemFormModalProps) {
  return (
    <Modal opened={opened} onClose={onClose} title={item ? 'Kaydı düzenle' : 'Yeni kayıt'} size="lg" centered>
      {opened && <TrackingItemForm key={item?.id ?? 'new'} item={item} onSubmit={onSubmit} onCancel={onClose} />}
    </Modal>
  )
}

function TrackingItemForm({
  item,
  onSubmit,
  onCancel,
}: {
  item?: TrackingItem
  onSubmit: (request: TrackingItemRequest) => Promise<unknown>
  onCancel: () => void
}) {
  const today = todayIso()
  const form = useForm<FormValues>({
    initialValues: {
      type: item?.type ?? 'RISK',
      title: item?.title ?? '',
      riskLevel: item?.riskLevel ?? null,
      impact: item?.impact ?? '',
      actionPlan: item?.actionPlan ?? '',
      assignee: item?.assignee ?? '',
      followUpDate: item?.followUpDate ?? '',
      status: item?.status ?? 'ACIK',
      description: item?.description ?? '',
    },
    // Backend'deki kurallarin aynisi (TrackingItemService); istek gitmeden alanin altinda gorunur.
    validate: {
      title: (value) =>
        value.trim() === '' ? 'Başlık zorunludur.' : maxLength(TRACKING_ITEM_LIMITS.title, 'Başlık')(value),
      riskLevel: (value, values) =>
        hasRiskLevel(values.type) && value === null ? 'Risk ve engel kayıtlarında risk seviyesi zorunludur.' : null,
      assignee: (value, values) => {
        if (isOpen(values.status) && value.trim() === '') return 'Kapanmamış kaydın sorumlusu zorunludur.'
        return maxLength(TRACKING_ITEM_LIMITS.assignee, 'Sorumlu')(value)
      },
      followUpDate: (value, values) => {
        if (value !== '' && isoWeekOf(value) === null) return 'Geçerli bir tarih girin.'
        if (!isOpen(values.status)) return null
        if (value === '') return 'Kapanmamış kaydın takip tarihi zorunludur.'
        // Degistirilmeyen eski tarih korunabilir: gecikmis kaydin aciklamasi tarihi ertelemeden duzenlenir.
        if (value < today && value !== item?.followUpDate) return 'Takip tarihi bugünden önce olamaz.'
        return null
      },
      impact: maxLength(TRACKING_ITEM_LIMITS.impact, 'Etki'),
      actionPlan: maxLength(TRACKING_ITEM_LIMITS.actionPlan, 'Aksiyon planı'),
      description: maxLength(TRACKING_ITEM_LIMITS.description, 'Açıklama'),
    },
  })

  // Alan hatasi tasimayan hata (or. proje silinmis: 404) formun ustunde gosterilir.
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

  const { type, status } = form.values
  const open = isOpen(status)

  return (
    <form onSubmit={submit} noValidate>
      <Stack gap="md">
        {generalError && (
          <Alert color="red" title="Kayıt kaydedilemedi">
            {generalError}
          </Alert>
        )}
        <Input.Wrapper label="Tür" withAsterisk>
          <SegmentedControl
            fullWidth
            data={enumOptions(TRACKING_ITEM_TYPES, TRACKING_ITEM_TYPE_LABELS)}
            {...form.getInputProps('type')}
          />
        </Input.Wrapper>
        <TextInput label="Başlık" withAsterisk data-autofocus {...form.getInputProps('title')} />
        <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
          {hasRiskLevel(type) && (
            <Select
              label="Risk seviyesi"
              withAsterisk
              placeholder="Seçin"
              data={enumOptions(RISK_LEVELS, RISK_LEVEL_LABELS)}
              {...form.getInputProps('riskLevel')}
            />
          )}
          <Select
            label="Durum"
            withAsterisk
            data={enumOptions(TRACKING_ITEM_STATUSES, trackingStatusLabels(type))}
            allowDeselect={false}
            {...form.getInputProps('status')}
          />
          <TextInput label="Sorumlu" withAsterisk={open} placeholder="Ad soyad" {...form.getInputProps('assignee')} />
          <TextInput type="date" label="Takip tarihi" withAsterisk={open} {...form.getInputProps('followUpDate')} />
        </SimpleGrid>
        <Textarea
          label="Etki"
          description="Gerçekleşirse ne olur"
          autosize
          minRows={2}
          maxRows={6}
          {...form.getInputProps('impact')}
        />
        <Textarea
          label="Aksiyon planı"
          description="Ne yapılacak"
          autosize
          minRows={2}
          maxRows={6}
          {...form.getInputProps('actionPlan')}
        />
        <Textarea label="Açıklama" autosize minRows={2} maxRows={8} {...form.getInputProps('description')} />
        <Group justify="flex-end">
          <Button variant="default" onClick={onCancel}>
            Vazgeç
          </Button>
          <Button type="submit" loading={form.submitting}>
            {item ? 'Kaydet' : 'Ekle'}
          </Button>
        </Group>
      </Stack>
    </form>
  )
}
