import { Anchor, Button, Paper, Stack, Table, Text, TextInput } from '@mantine/core'
import { Link, useNavigate, useSearchParams } from 'react-router'

import { useProjects } from '../../api/projects'
import type { Project } from '../../api/types'
import { EnumBadge } from '../../components/EnumBadge'
import { PageHeader } from '../../components/PageHeader'
import { QueryState } from '../../components/QueryState'
import { matchesSearch } from '../../lib/format'
import { PROJECT_STATUS_LABELS } from '../../lib/labels'
import { toSearchString } from '../../lib/searchParams'

/**
 * Proje listesi. Proje sayisi kucuk oldugu icin backend tum listeyi doner; arama istemcide yapilir
 * ve adreste tutulur (/projects?q=atlas).
 */
export function ProjectListPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const query = searchParams.get('q') ?? ''
  const projects = useProjects()

  return (
    <Stack gap="md">
      <PageHeader
        title="Projeler"
        actions={
          <Button component={Link} to="/projects/new">
            Yeni proje
          </Button>
        }
      />

      <TextInput
        placeholder="Proje, müşteri ya da sorumlu ara"
        aria-label="Proje ara"
        value={query}
        onChange={(event) => navigate({ search: toSearchString({ q: event.currentTarget.value }) }, { replace: true })}
        maw={360}
      />

      <QueryState query={projects} isEmpty={(data) => data.length === 0} empty="Henüz proje yok.">
        {(data) => (
          <ProjectTable projects={data.filter((p) => matchesSearch(query, p.name, p.customer, p.ownerFullName))} />
        )}
      </QueryState>
    </Stack>
  )
}

function ProjectTable({ projects }: { projects: Project[] }) {
  return (
    <Paper withBorder>
      <Table.ScrollContainer minWidth={560}>
        <Table striped highlightOnHover verticalSpacing="sm" style={{ whiteSpace: 'nowrap' }}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Proje</Table.Th>
              <Table.Th>Müşteri</Table.Th>
              <Table.Th>Sorumlu</Table.Th>
              <Table.Th>Durum</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {projects.length === 0 ? (
              <Table.Tr>
                <Table.Td colSpan={4}>
                  <Text c="dimmed" ta="center" py="md">
                    Aramaya uyan proje yok.
                  </Text>
                </Table.Td>
              </Table.Tr>
            ) : (
              projects.map((project) => (
                <Table.Tr key={project.id}>
                  <Table.Td>
                    <Anchor component={Link} to={`/projects/${project.id}`} fw={500} size="sm">
                      {project.name}
                    </Anchor>
                  </Table.Td>
                  <Table.Td>{project.customer ?? '–'}</Table.Td>
                  <Table.Td>{project.ownerFullName ?? '–'}</Table.Td>
                  <Table.Td>
                    <EnumBadge value={project.status} labels={PROJECT_STATUS_LABELS} />
                  </Table.Td>
                </Table.Tr>
              ))
            )}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  )
}
