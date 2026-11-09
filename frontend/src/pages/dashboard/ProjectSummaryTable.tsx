import { Badge, Group, Paper, Progress, Table, Text } from '@mantine/core'

import type { DashboardProjectSummary } from '../../api/types'
import { EnumBadge } from '../../components/EnumBadge'
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
              <Table.Th>Aşama</Table.Th>
              <Table.Th>İlerleme</Table.Th>
              <Table.Th>Genel durum</Table.Th>
              <Table.Th>Risk</Table.Th>
              <Table.Th ta="right">Canlı task</Table.Th>
              <Table.Th>Gecikme</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {projects.length === 0 ? (
              <Table.Tr>
                <Table.Td colSpan={10}>
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
        <Text fw={500} size="sm">
          {project.name}
        </Text>
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
          <Text size="sm">
            {project.lastYear} / {project.lastWeekNumber}. hafta
          </Text>
        ) : (
          <Badge color="gray" variant="outline" miw="max-content">
            Rapor yok
          </Badge>
        )}
      </Table.Td>
      <Table.Td>
        <EnumBadge value={project.progressStage} labels={PROGRESS_STAGE_LABELS} />
      </Table.Td>
      <Table.Td>
        {project.hasReport ? (
          <Group gap="xs" wrap="nowrap">
            <Progress value={project.progressPercentage} w={72} aria-label={`İlerleme ${project.name}`} />
            <Text size="sm">%{project.progressPercentage}</Text>
          </Group>
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
