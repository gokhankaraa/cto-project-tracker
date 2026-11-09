import { Anchor, Badge, Paper, Stack, Table, Text } from '@mantine/core'
import { Link } from 'react-router'

import type { DashboardProjectSummary } from '../../api/types'
import { EnumBadge } from '../../components/EnumBadge'
import { ProgressValue } from '../../components/ProgressValue'
import { formatWeek } from '../../lib/format'
import { OVERALL_STATUS_LABELS, PROGRESS_STAGE_LABELS, PROJECT_STATUS_LABELS, RISK_LEVEL_LABELS } from '../../lib/labels'

const Dash = () => (
  <Text span c="dimmed" size="sm">
    –
  </Text>
)

/**
 * Projelerin degerlendirilen rapora gore durumu. Dar ekranda tablo kendi icinde yatay kayar;
 * sayfanin geri kalani tasmaz.
 */
export function ProjectSummaryTable({ projects }: { projects: DashboardProjectSummary[] }) {
  return (
    <Paper withBorder>
      <Table.ScrollContainer minWidth={960}>
        {/* Hucreler satir kirmaz; rozetler kesilmez, dar ekranda tablo yatay kayar. */}
        <Table striped highlightOnHover verticalSpacing="sm" style={{ whiteSpace: 'nowrap' }}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Proje</Table.Th>
              <Table.Th>Sorumlu</Table.Th>
              <Table.Th>Proje durumu</Table.Th>
              <Table.Th>Rapor haftası</Table.Th>
              {/* Ilerleme yuzdesi asamadan turetilir; ikisi tek sutunda (1280 px'de tablo yana kaymaz). */}
              <Table.Th>Aşama / ilerleme</Table.Th>
              <Table.Th>Genel durum</Table.Th>
              <Table.Th>Risk</Table.Th>
              <Table.Th ta="right">Canlı task</Table.Th>
              <Table.Th>Gecikme</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {projects.length === 0 ? (
              <Table.Tr>
                <Table.Td colSpan={9}>
                  <Text c="dimmed" ta="center" py="md">
                    Filtreye uyan proje yok.
                  </Text>
                </Table.Td>
              </Table.Tr>
            ) : (
              projects.map((project) => <ProjectRow key={project.projectId} project={project} />)
            )}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  )
}

function ProjectRow({ project }: { project: DashboardProjectSummary }) {
  return (
    <Table.Tr>
      <Table.Td>
        <Anchor component={Link} to={`/projects/${project.projectId}`} fw={500} size="sm">
          {project.name}
        </Anchor>
        {project.customer && (
          <Text size="xs" c="dimmed">
            {project.customer}
          </Text>
        )}
      </Table.Td>
      <Table.Td>{project.ownerFullName ?? <Dash />}</Table.Td>
      <Table.Td>
        <EnumBadge value={project.projectStatus} labels={PROJECT_STATUS_LABELS} />
      </Table.Td>
      <Table.Td>
        {project.hasReport ? (
          <Text size="sm">{formatWeek(project.lastYear, project.lastWeekNumber)}</Text>
        ) : (
          <Badge color="gray" variant="outline" miw="max-content">
            Rapor yok
          </Badge>
        )}
      </Table.Td>
      <Table.Td>
        {project.hasReport ? (
          <Stack gap={4} align="flex-start">
            <EnumBadge value={project.progressStage} labels={PROGRESS_STAGE_LABELS} />
            <ProgressValue value={project.progressPercentage} label={`İlerleme ${project.name}`} />
          </Stack>
        ) : (
          <Dash />
        )}
      </Table.Td>
      <Table.Td>
        <EnumBadge value={project.overallStatus} labels={OVERALL_STATUS_LABELS} />
      </Table.Td>
      <Table.Td>
        <EnumBadge value={project.riskLevel} labels={RISK_LEVEL_LABELS} />
      </Table.Td>
      <Table.Td ta="right">{project.liveTaskCount}</Table.Td>
      <Table.Td>
        {project.delayed ? (
          <Badge color="orange" variant="filled" miw="max-content">
            Gecikmede
          </Badge>
        ) : (
          <Dash />
        )}
      </Table.Td>
    </Table.Tr>
  )
}
