import { Alert, Anchor, Button, Group, Modal, Paper, SimpleGrid, Stack, Table, Text, Title, Tooltip } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import type { ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router'

import { errorMessage } from '../../api/client'
import { useDeleteProject, useProject, useProjectReports } from '../../api/projects'
import { useProjectTrackingItems } from '../../api/trackingItems'
import type { Project, WeeklyReport } from '../../api/types'
import { usePermissions } from '../../auth/permissions'
import { EnumBadge } from '../../components/EnumBadge'
import { PageHeader } from '../../components/PageHeader'
import { ProgressValue } from '../../components/ProgressValue'
import { QueryState } from '../../components/QueryState'
import { formatDate, formatWeek, parseId } from '../../lib/format'
import {
  OVERALL_STATUS_LABELS,
  PROGRESS_STAGE_LABELS,
  PROJECT_STATUS_LABELS,
  RISK_LEVEL_LABELS,
} from '../../lib/labels'
import { NotFoundPage } from '../NotFoundPage'
import { ProjectHistorySection } from './history/ProjectHistorySection'
import { TrackingItemsSection } from '../tracking/TrackingItemsSection'

/** /projects/:id. Gecersiz id (/projects/abc) istek atilmadan "sayfa bulunamadi" gosterir. */
export function ProjectDetailPage() {
  const id = parseId(useParams().id)
  return id === null ? <NotFoundPage /> : <ProjectDetail id={id} />
}

function ProjectDetail({ id }: { id: number }) {
  const project = useProject(id)
  const reports = useProjectReports(id)
  // Bolumle ayni sorgu (ortak onbellek): silme dugmesi kayit olup olmadigini bilir.
  const trackingItems = useProjectTrackingItems(id)
  const can = usePermissions()

  return (
    <QueryState query={project}>
      {(data) => (
        <Stack gap="md">
          <PageHeader
            title={data.name}
            back={{ to: '/projects', label: 'Projeler' }}
            actions={
              can.manageProjects && (
                <>
                  <Button component={Link} to={`/projects/${id}/edit`} variant="default">
                    Düzenle
                  </Button>
                  <DeleteProjectButton
                    project={data}
                    reportCount={reports.data?.length}
                    trackingItemCount={trackingItems.data?.length}
                  />
                </>
              )
            }
          />

          <ProjectInfo project={data} />

          <TrackingItemsSection projectId={id} />

          <Group justify="space-between">
            <Title order={3}>Haftalık raporlar</Title>
            {can.writeReports && (
              <Button component={Link} to={`/projects/${id}/reports/new`}>
                Yeni rapor
              </Button>
            )}
          </Group>
          <QueryState query={reports} isEmpty={(list) => list.length === 0} empty="Bu proje için henüz rapor yok.">
            {(list) => <ReportHistoryTable reports={list} />}
          </QueryState>

          <ProjectHistorySection projectId={id} />
        </Stack>
      )}
    </QueryState>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <Text size="sm" c="dimmed" fw={500}>
        {label}
      </Text>
      <Text size="sm" component="div">
        {children}
      </Text>
    </div>
  )
}

function ProjectInfo({ project }: { project: Project }) {
  return (
    <Paper withBorder p="md">
      <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="md">
        <Field label="Durum">
          <EnumBadge value={project.status} labels={PROJECT_STATUS_LABELS} />
        </Field>
        <Field label="Sorumlu">{project.ownerFullName ?? '–'}</Field>
        <Field label="Müşteri">{project.customer ?? '–'}</Field>
      </SimpleGrid>
      {project.description && (
        <Stack mt="md" gap={0}>
          <Field label="Açıklama">
            <Text size="sm" style={{ whiteSpace: 'pre-line' }}>
              {project.description}
            </Text>
          </Field>
        </Stack>
      )}
    </Paper>
  )
}

/** En yeni hafta ustte (siralama useProjectReports'ta). */
function ReportHistoryTable({ reports }: { reports: WeeklyReport[] }) {
  return (
    <Paper withBorder>
      <Table.ScrollContainer minWidth={760}>
        <Table striped highlightOnHover verticalSpacing="sm" style={{ whiteSpace: 'nowrap' }}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Hafta</Table.Th>
              <Table.Th>Rapor tarihi</Table.Th>
              <Table.Th>Aşama</Table.Th>
              <Table.Th>İlerleme</Table.Th>
              <Table.Th>Genel durum</Table.Th>
              <Table.Th>Risk</Table.Th>
              <Table.Th ta="right">Canlı task</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {reports.map((report) => (
              <Table.Tr key={report.id}>
                <Table.Td>
                  <Anchor component={Link} to={`/reports/${report.id}`} size="sm" fw={500}>
                    {formatWeek(report.year, report.weekNumber)}
                  </Anchor>
                </Table.Td>
                <Table.Td>{formatDate(report.reportDate)}</Table.Td>
                <Table.Td>
                  <EnumBadge value={report.progressStage} labels={PROGRESS_STAGE_LABELS} />
                </Table.Td>
                <Table.Td>
                  <ProgressValue value={report.progressPercentage} label={`İlerleme ${report.weekNumber}. hafta`} />
                </Table.Td>
                <Table.Td>
                  <EnumBadge value={report.overallStatus} labels={OVERALL_STATUS_LABELS} />
                </Table.Td>
                <Table.Td>
                  <EnumBadge value={report.riskLevel} labels={RISK_LEVEL_LABELS} />
                </Table.Td>
                <Table.Td ta="right">{report.liveTaskCount}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  )
}

/**
 * Silme onayi tarayicinin confirm() penceresi yerine uygulama icinde sorulur. Raporu ya da risk / aksiyon kaydi
 * oldugu bilinen projede dugme bastan pasiftir; yine de backend 409 donerse (kayit o arada eklendiyse) mesaji
 * gosterilir.
 */
function DeleteProjectButton({
  project,
  reportCount,
  trackingItemCount,
}: {
  project: Project
  reportCount: number | undefined
  trackingItemCount: number | undefined
}) {
  const [opened, { open, close }] = useDisclosure()
  const navigate = useNavigate()
  const deletion = useDeleteProject(project.id)
  const blockedBy =
    (reportCount ?? 0) > 0
      ? 'Haftalık raporu olan proje silinemez.'
      : (trackingItemCount ?? 0) > 0
        ? 'Risk, engel, karar ya da aksiyon kaydı olan proje silinemez.'
        : null

  const closeModal = () => {
    close()
    deletion.reset()
  }

  const confirm = () =>
    deletion.mutate(undefined, {
      onSuccess: () => {
        notifications.show({ color: 'teal', message: `"${project.name}" silindi.` })
        navigate('/projects')
      },
    })

  return (
    <>
      <Tooltip label={blockedBy} disabled={blockedBy === null}>
        {/* Pasif dugme fare olaylarini almaz; ipucu sarmalayicidaki span uzerinden gosterilir. */}
        <span>
          <Button color="red" variant="light" onClick={open} disabled={blockedBy !== null}>
            Sil
          </Button>
        </span>
      </Tooltip>

      <Modal opened={opened} onClose={closeModal} title="Projeyi sil" centered>
        <Stack gap="md">
          <Text size="sm">
            <b>{project.name}</b> projesi silinecek. Bu işlem geri alınamaz.
          </Text>
          {deletion.isError && (
            <Alert color="red" title="Proje silinemedi">
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
