import { Alert, Button, Group, NumberInput, Paper, Select, SimpleGrid, Stack, Textarea, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import type { UseMutationResult } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router'

import { ApiRequestError, errorMessage } from '../../api/client'
import { useProject, useProjectReports } from '../../api/projects'
import {
  allowedStages,
  REPORT_TEXT_LIMIT,
  STAGE_PERCENTAGES,
  useCreateReport,
  useReport,
  useUpdateReport,
  type WeeklyReportRequest,
} from '../../api/reports'
import {
  OVERALL_STATUSES,
  PROGRESS_STAGES,
  RISK_LEVELS,
  type OverallStatus,
  type ProgressStage,
  type RiskLevel,
  type WeeklyReport,
} from '../../api/types'
import { PageHeader } from '../../components/PageHeader'
import { QueryState } from '../../components/QueryState'
import { formatDate, formatWeek, parseId } from '../../lib/format'
import { addDays, checkReportWeek, isoWeekOf, todayIso } from '../../lib/isoWeek'
import { enumOptions, OVERALL_STATUS_LABELS, PROGRESS_STAGE_LABELS, RISK_LEVEL_LABELS } from '../../lib/labels'
import { NotFoundPage } from '../NotFoundPage'

/** /projects/:id/reports/new */
export function NewReportPage() {
  const projectId = parseId(useParams().id)
  return projectId === null ? <NotFoundPage /> : <NewReport projectId={projectId} />
}

function NewReport({ projectId }: { projectId: number }) {
  const project = useProject(projectId)
  const reports = useProjectReports(projectId)
  const mutation = useCreateReport(projectId)
  return (
    <QueryState query={project}>
      {(projectData) => (
        <Stack gap="md">
          <PageHeader
            title="Yeni haftalık rapor"
            description={projectData.name}
            back={{ to: `/projects/${projectId}`, label: projectData.name }}
          />
          {/* Varsayilan kademe ve "son rapor" ipucu son rapordan gelir; form raporlar yuklenince acilir. */}
          <QueryState query={reports}>
            {(list) => <ReportForm latest={list[0]} mutation={mutation} cancelTo={`/projects/${projectId}`} />}
          </QueryState>
        </Stack>
      )}
    </QueryState>
  )
}

/** /reports/:id/edit */
export function EditReportPage() {
  const id = parseId(useParams().id)
  return id === null ? <NotFoundPage /> : <EditReport id={id} />
}

function EditReport({ id }: { id: number }) {
  const report = useReport(id)
  const mutation = useUpdateReport(id)
  return (
    <QueryState query={report}>
      {(data) => (
        <Stack gap="md">
          <PageHeader
            title={`${formatWeek(data.year, data.weekNumber)} · düzenle`}
            description={data.projectName}
            back={{ to: `/reports/${id}`, label: 'Rapor' }}
          />
          <ReportForm report={data} mutation={mutation} cancelTo={`/reports/${id}`} />
        </Stack>
      )}
    </QueryState>
  )
}

interface FormValues {
  reportDate: string
  /** NumberInput bos birakilinca '' verir. */
  weekNumber: number | string
  progressStage: ProgressStage | null
  overallStatus: OverallStatus | null
  riskLevel: RiskLevel | null
  done: string
  planned: string
  risks: string
  note: string
}

type TextField = 'done' | 'planned' | 'risks' | 'note'

const TEXT_FIELDS: { field: TextField; label: string }[] = [
  { field: 'done', label: 'Bu hafta yapılanlar' },
  { field: 'planned', label: 'Gelecek hafta planlananlar' },
  { field: 'risks', label: 'Riskler / engeller' },
  { field: 'note', label: 'Genel not' },
]

const maxText = (label: string) => (value: string) =>
  value.trim().length > REPORT_TEXT_LIMIT ? `${label} en fazla ${REPORT_TEXT_LIMIT} karakter olabilir.` : null

const required = (message: string) => (value: unknown) => (value === null || value === '' ? message : null)

function toRequest(values: FormValues): WeeklyReportRequest {
  const optional = (value: string) => (value.trim() === '' ? null : value.trim())
  return {
    weekNumber: Number(values.weekNumber),
    reportDate: values.reportDate,
    progressStage: values.progressStage!,
    overallStatus: values.overallStatus!,
    riskLevel: values.riskLevel!,
    done: optional(values.done),
    planned: optional(values.planned),
    risks: optional(values.risks),
    note: optional(values.note),
  }
}

/**
 * Yeni raporun varsayilan haftasi: bugunden bir onceki hafta. Rapor, anlattigi haftanin ertesi gunlerinde
 * yazildigi icin (seed verisinde ertesi pazartesi) bugunun tarihiyle her zaman gecerli bir ciftir.
 */
function defaultWeek(today: string): number {
  return isoWeekOf(addDays(today, -7))!.week
}

/** Hafta ve tarih ikisi de bicimce gecerliyse aralarindaki kurali denetler (backend ile ayni mesajlar). */
function weekCheck(values: FormValues) {
  const week = Number(values.weekNumber)
  if (values.weekNumber === '' || !Number.isInteger(week) || week < 1 || week > 53) return null
  if (isoWeekOf(values.reportDate) === null) return null
  return checkReportWeek(week, values.reportDate)
}

/**
 * Backend hatasini forma yerlestirir. Ayni hafta (409) hafta alanina, kademe kurali (400 is kurali)
 * kademe alanina, dogrulama hatalari kendi alanlarina yazilir; yerlestirildiyse true.
 */
interface ErrorTarget {
  setFieldError: (field: keyof FormValues, message: string) => void
  setErrors: (errors: Record<string, string>) => void
}

function placeError(error: Error, form: ErrorTarget): boolean {
  if (!(error instanceof ApiRequestError)) return false
  if (error.code === 'DUPLICATE_RESOURCE') {
    form.setFieldError('weekNumber', error.message)
    return true
  }
  if (error.code === 'BUSINESS_RULE_VIOLATION') {
    form.setFieldError('progressStage', error.message)
    return true
  }
  if (error.fieldErrors.length > 0) {
    form.setErrors(error.fieldErrorMap())
    return true
  }
  return false
}

const isPlaceable = (error: Error | null) =>
  error instanceof ApiRequestError &&
  (error.code === 'DUPLICATE_RESOURCE' || error.code === 'BUSINESS_RULE_VIOLATION' || error.fieldErrors.length > 0)

interface ReportFormProps {
  /** Verilirse duzenleme. */
  report?: WeeklyReport
  /** Yeni raporda projenin en son raporu (varsayilanlar icin). */
  latest?: WeeklyReport
  mutation: UseMutationResult<WeeklyReport, Error, WeeklyReportRequest>
  cancelTo: string
}

/**
 * Haftalik rapor formu. Hafta, anlatilan haftadir; rapor tarihi o haftanin icinde ya da en gec ertesi
 * haftasinda olmalidir (seed verisinde ertesi pazartesi). Yil girilmez: anlatilan haftanin yilidir ve
 * formda "Bu rapor 2026 / 31. hafta olarak kaydedilir" diye gosterilir; yil sinirinda rapor tarihinin
 * yilindan farkli olabilir (53. haftanin raporu ocakta yazilir).
 */
function ReportForm({ report, latest, mutation, cancelTo }: ReportFormProps) {
  const navigate = useNavigate()
  const today = todayIso()

  const form = useForm<FormValues>({
    initialValues: {
      reportDate: report?.reportDate ?? today,
      weekNumber: report?.weekNumber ?? defaultWeek(today),
      progressStage: report?.progressStage ?? latest?.progressStage ?? 'ANALIZ',
      overallStatus: report?.overallStatus ?? null,
      riskLevel: report?.riskLevel ?? null,
      done: report?.done ?? '',
      planned: report?.planned ?? '',
      risks: report?.risks ?? '',
      note: report?.note ?? '',
    },
    validate: {
      reportDate: (value, values) => {
        if (value === '') return 'Rapor tarihi zorunludur.'
        if (isoWeekOf(value) === null) return 'Geçerli bir tarih girin.'
        const check = weekCheck(values)
        return check && !check.ok && check.field === 'reportDate' ? check.message : null
      },
      weekNumber: (value, values) => {
        if (value === '') return 'Hafta numarası zorunludur.'
        const week = Number(value)
        if (!Number.isInteger(week) || week < 1 || week > 53) return 'Hafta numarası 1 ile 53 arasında olmalıdır.'
        const check = weekCheck(values)
        return check && !check.ok && check.field === 'weekNumber' ? check.message : null
      },
      progressStage: required('İlerleme aşaması zorunludur.'),
      overallStatus: required('Genel durum zorunludur.'),
      riskLevel: required('Risk seviyesi zorunludur.'),
      done: maxText('Yapılanlar'),
      planned: maxText('Planlananlar'),
      risks: maxText('Riskler'),
      note: maxText('Genel not'),
    },
  })

  // Duzenlemede yalnizca mevcut kademe ya da bir sonraki secilebilir (backend kurali); digerleri pasif.
  const allowed = report ? allowedStages(report.progressStage) : PROGRESS_STAGES
  const stageOptions = PROGRESS_STAGES.map((stage) => ({
    value: stage,
    label: `${PROGRESS_STAGE_LABELS[stage].label} (%${STAGE_PERCENTAGES[stage]})`,
    disabled: !allowed.includes(stage),
  }))

  const check = weekCheck(form.values)
  const weekHint =
    check?.ok === true
      ? `Bu rapor ${formatWeek(check.year, Number(form.values.weekNumber))} olarak kaydedilir (${formatDate(check.monday)} haftası).`
      : 'Anlatılan hafta. Rapor bu hafta ya da ertesi hafta yazılabilir.'
  const stageHint = report
    ? 'Aşama aynı kalabilir ya da bir sonrakine geçebilir.'
    : latest
      ? `Son rapor: ${formatWeek(latest.year, latest.weekNumber)}, ${PROGRESS_STAGE_LABELS[latest.progressStage].label}.`
      : undefined

  const submit = form.onSubmit((values) =>
    mutation.mutate(toRequest(values), {
      onSuccess: (saved) => {
        notifications.show({ color: 'teal', message: report ? 'Rapor güncellendi.' : 'Rapor oluşturuldu.' })
        navigate(`/reports/${saved.id}`)
      },
      onError: (error) => placeError(error, form),
    }),
  )

  return (
    <Paper withBorder p="md" maw={880}>
      <form onSubmit={submit} noValidate>
        <Stack gap="md">
          {mutation.isError && !isPlaceable(mutation.error) && (
            <Alert color="red" title="Rapor kaydedilemedi">
              {errorMessage(mutation.error)}
            </Alert>
          )}

          <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
            <TextInput type="date" label="Rapor tarihi" withAsterisk {...form.getInputProps('reportDate')} />
            <NumberInput
              label="Hafta"
              withAsterisk
              min={1}
              max={53}
              allowDecimal={false}
              allowNegative={false}
              description={weekHint}
              inputWrapperOrder={['label', 'input', 'description', 'error']}
              {...form.getInputProps('weekNumber')}
            />
          </SimpleGrid>

          <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="md">
            <Select
              label="İlerleme aşaması"
              withAsterisk
              data={stageOptions}
              allowDeselect={false}
              description={stageHint}
              inputWrapperOrder={['label', 'input', 'description', 'error']}
              {...form.getInputProps('progressStage')}
            />
            <Select
              label="Genel durum"
              withAsterisk
              placeholder="Seçin"
              data={enumOptions(OVERALL_STATUSES, OVERALL_STATUS_LABELS)}
              {...form.getInputProps('overallStatus')}
            />
            <Select
              label="Risk"
              withAsterisk
              placeholder="Seçin"
              data={enumOptions(RISK_LEVELS, RISK_LEVEL_LABELS)}
              {...form.getInputProps('riskLevel')}
            />
          </SimpleGrid>

          <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
            {TEXT_FIELDS.map(({ field, label }) => (
              <Textarea key={field} label={label} autosize minRows={3} maxRows={10} {...form.getInputProps(field)} />
            ))}
          </SimpleGrid>

          <Group justify="flex-end">
            <Button component={Link} to={cancelTo} variant="default">
              Vazgeç
            </Button>
            <Button type="submit" loading={mutation.isPending}>
              {report ? 'Kaydet' : 'Oluştur'}
            </Button>
          </Group>
        </Stack>
      </form>
    </Paper>
  )
}
