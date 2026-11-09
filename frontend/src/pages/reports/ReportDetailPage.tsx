import { Alert, Button, Group, Modal, Paper, SimpleGrid, Stack, Text, Title } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import type { ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router'

import { errorMessage } from '../../api/client'
import { useDeleteReport, useReport } from '../../api/reports'
import type { WeeklyReport } from '../../api/types'
import { usePermissions } from '../../auth/permissions'
import { EnumBadge } from '../../components/EnumBadge'
import { PageHeader } from '../../components/PageHeader'
import { ProgressValue } from '../../components/ProgressValue'
import { QueryState } from '../../components/QueryState'
import { formatDate, formatWeek, parseId } from '../../lib/format'
import { OVERALL_STATUS_LABELS, PROGRESS_STAGE_LABELS, RISK_LEVEL_LABELS } from '../../lib/labels'
import { NotFoundPage } from '../NotFoundPage'
import { WorkItemsSection } from './WorkItemsSection'

/** /reports/:id */
export function ReportDetailPage() {
  const id = parseId(useParams().id)
  return id === null ? <NotFoundPage /> : <ReportDetail id={id} />
}

function ReportDetail({ id }: { id: number }) {
  const report = useReport(id)
  const can = usePermissions()

  return (
    <QueryState query={report}>
      {(data) => (
        <Stack gap="md">
          <PageHeader
            title={formatWeek(data.year, data.weekNumber)}
            description={`${data.projectName} · haftalık rapor`}
            back={{ to: `/projects/${data.projectId}`, label: data.projectName }}
            actions={
              can.writeReports && (
                <>
                  <Button component={Link} to={`/reports/${id}/edit`} variant="default">
                    Düzenle
                  </Button>
                  <DeleteReportButton report={data} />
                </>
              )
            }
          />
          <ReportSummary report={data} />
          <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
            <TextSection title="Bu hafta yapılanlar" text={data.done} />
            <TextSection title="Gelecek hafta planlananlar" text={data.planned} />
            <TextSection title="Riskler / engeller" text={data.risks} />
            <TextSection title="Genel not" text={data.note} />
          </SimpleGrid>
          <WorkItemsSection report={data} />
        </Stack>
      )}
    </QueryState>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <Text size="xs" c="dimmed" tt="uppercase" fw={600} mb={4}>
        {label}
      </Text>
      <Text size="sm" component="div">
        {children}
      </Text>
    </div>
  )
}

function ReportSummary({ report }: { report: WeeklyReport }) {
  return (
    <Paper withBorder p="md">
      <SimpleGrid cols={{ base: 2, sm: 3, lg: 6 }} spacing="md">
        <Field label="Rapor tarihi">{formatDate(report.reportDate)}</Field>
        <Field label="Aşama">
          <EnumBadge value={report.progressStage} labels={PROGRESS_STAGE_LABELS} />
        </Field>
        <Field label="İlerleme">
          <ProgressValue value={report.progressPercentage} label="İlerleme" />
        </Field>
        <Field label="Genel durum">
          <EnumBadge value={report.overallStatus} labels={OVERALL_STATUS_LABELS} />
        </Field>
        <Field label="Risk">
          <EnumBadge value={report.riskLevel} labels={RISK_LEVEL_LABELS} />
        </Field>
        <Field label="Canlı task">{report.liveTaskCount}</Field>
      </SimpleGrid>
    </Paper>
  )
}

function TextSection({ title, text }: { title: string; text: string | null }) {
  return (
    <Paper withBorder p="md">
      <Title order={4} mb="xs">
        {title}
      </Title>
      {text ? (
        <Text size="sm" style={{ whiteSpace: 'pre-line' }}>
          {text}
        </Text>
      ) : (
        <Text size="sm" c="dimmed">
          Girilmemiş.
        </Text>
      )}
    </Paper>
  )
}

function DeleteReportButton({ report }: { report: WeeklyReport }) {
  const [opened, { open, close }] = useDisclosure()
  const navigate = useNavigate()
  const deletion = useDeleteReport(report)

  const closeModal = () => {
    close()
    deletion.reset()
  }

  const confirm = () =>
    deletion.mutate(undefined, {
      onSuccess: () => {
        notifications.show({ color: 'teal', message: 'Rapor silindi.' })
        navigate(`/projects/${report.projectId}`)
      },
    })

  return (
    <>
      <Button color="red" variant="light" onClick={open}>
        Sil
      </Button>
      <Modal opened={opened} onClose={closeModal} title="Raporu sil" centered>
        <Stack gap="md">
          <Text size="sm">
            <b>{report.projectName}</b> projesinin {formatWeek(report.year, report.weekNumber)} raporu ve bu rapora
            bağlı tüm iş kalemleri silinecek. Bu işlem geri alınamaz.
          </Text>
          {deletion.isError && (
            <Alert color="red" title="Rapor silinemedi">
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
    </>
  )
}
