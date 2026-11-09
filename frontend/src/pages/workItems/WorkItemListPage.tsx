import { Anchor, Badge, Box, Paper, Stack, Table, Text } from '@mantine/core'
import { useMemo } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'

import type { WorkItem } from '../../api/types'
import { useWorkItemSearch, type WorkItemFilters, type WorkItemSort } from '../../api/workItems'
import { EnumBadge } from '../../components/EnumBadge'
import { ListPagination, ResultHeader } from '../../components/ListControls'
import { PageHeader } from '../../components/PageHeader'
import { QueryState } from '../../components/QueryState'
import { formatDate, formatWeek } from '../../lib/format'
import { WORK_ITEM_STATUS_LABELS } from '../../lib/labels'
import { parseWorkItemList, workItemListSearch, type WorkItemListState } from './filters'
import { WorkItemFilterBar } from './WorkItemFilterBar'

const SORT_OPTIONS: { value: WorkItemSort; label: string }[] = [
  { value: 'plannedDate,asc', label: 'Planlanan tarih (yakın önce)' },
  { value: 'plannedDate,desc', label: 'Planlanan tarih (uzak önce)' },
  { value: 'title,asc', label: 'Başlık (A–Z)' },
  { value: 'projectName,asc', label: 'Proje (A–Z)' },
]

function referenceText({ year, weekNumber }: WorkItemFilters) {
  if (year === null) return 'Her projenin en son haftalık raporundaki iş kalemleri.'
  if (weekNumber === null) return `Her projenin ${year} sonuna kadarki en son raporundaki iş kalemleri.`
  return `Her projenin ${year} yılı ${weekNumber}. haftaya kadarki en son raporundaki iş kalemleri.`
}

/** /work-items: projeler arasi, filtreli ve sayfali is kalemi listesi. Durum adreste tutulur. */
export function WorkItemListPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const state = useMemo(() => parseWorkItemList(searchParams), [searchParams])
  const result = useWorkItemSearch(state.filters, state.page - 1, state.sort)

  const go = (next: WorkItemListState) => navigate({ search: workItemListSearch(next) }, { replace: true })
  // Filtre ya da siralama degisince 1. sayfaya donulur; eski sayfa numarasi yeni sonucta olmayabilir.
  const setFilters = (filters: WorkItemFilters) => go({ ...state, filters, page: 1 })

  return (
    <Stack gap="md">
      <PageHeader title="İş kalemleri" description={referenceText(state.filters)} />
      <WorkItemFilterBar filters={state.filters} onChange={setFilters} />

      <QueryState query={result}>
        {(data) => (
          <Box style={{ opacity: result.isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <Stack gap="sm">
              <ResultHeader
                total={data.totalElements}
                noun="iş kalemi"
                sortOptions={SORT_OPTIONS}
                sort={state.sort}
                onSortChange={(sort) => go({ ...state, sort, page: 1 })}
              />
              <WorkItemTable items={data.content} />
              <ListPagination totalPages={data.totalPages} page={state.page} onChange={(page) => go({ ...state, page })} />
            </Stack>
          </Box>
        )}
      </QueryState>
    </Stack>
  )
}

function WorkItemTable({ items }: { items: WorkItem[] }) {
  return (
    <Paper withBorder>
      <Table.ScrollContainer minWidth={900}>
        <Table striped highlightOnHover verticalSpacing="sm">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Başlık</Table.Th>
              <Table.Th>Proje</Table.Th>
              <Table.Th>Rapor</Table.Th>
              <Table.Th>Sorumlu</Table.Th>
              <Table.Th>Durum</Table.Th>
              <Table.Th>Planlanan</Table.Th>
              <Table.Th>Gecikme</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {items.length === 0 ? (
              <Table.Tr>
                <Table.Td colSpan={7}>
                  <Text c="dimmed" ta="center" py="md">
                    Filtreye uyan iş kalemi yok.
                  </Text>
                </Table.Td>
              </Table.Tr>
            ) : (
              items.map((item) => (
                <Table.Tr key={item.id}>
                  <Table.Td maw={320}>
                    <Text size="sm" fw={500}>
                      {item.title}
                    </Text>
                    {item.description && (
                      <Text size="xs" c="dimmed" lineClamp={1}>
                        {item.description}
                      </Text>
                    )}
                  </Table.Td>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>
                    <Anchor component={Link} to={`/projects/${item.projectId}`} size="sm">
                      {item.projectName}
                    </Anchor>
                  </Table.Td>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>
                    <Anchor component={Link} to={`/reports/${item.weeklyReportId}`} size="sm">
                      {formatWeek(item.year, item.weekNumber)}
                    </Anchor>
                  </Table.Td>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>{item.assignee ?? '–'}</Table.Td>
                  <Table.Td>
                    <EnumBadge value={item.status} labels={WORK_ITEM_STATUS_LABELS} />
                  </Table.Td>
                  <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDate(item.plannedDate)}</Table.Td>
                  <Table.Td>
                    {item.delayed ? (
                      <Badge color="orange" variant="filled" miw="max-content">
                        Gecikmede
                      </Badge>
                    ) : (
                      '–'
                    )}
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
