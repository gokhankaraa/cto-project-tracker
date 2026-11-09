import { Anchor, Box, Paper, Stack, Table, Text } from '@mantine/core'
import { useMemo } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'

import { useReportSearch, type ReportFilters, type ReportSort } from '../../api/reports'
import type { WeeklyReport } from '../../api/types'
import { EnumBadge } from '../../components/EnumBadge'
import { ListPagination, ResultHeader } from '../../components/ListControls'
import { PageHeader } from '../../components/PageHeader'
import { ProgressValue } from '../../components/ProgressValue'
import { QueryState } from '../../components/QueryState'
import { formatDate, formatWeek } from '../../lib/format'
import { OVERALL_STATUS_LABELS, PROGRESS_STAGE_LABELS, RISK_LEVEL_LABELS } from '../../lib/labels'
import { parseReportList, reportListSearch, type ReportListState } from './listFilters'
import { ReportFilterBar } from './ReportFilterBar'

const SORT_OPTIONS: { value: ReportSort; label: string }[] = [
  { value: 'reportDate,desc', label: 'Rapor tarihi (yeni önce)' },
  { value: 'reportDate,asc', label: 'Rapor tarihi (eski önce)' },
  { value: 'weekNumber,desc', label: 'Hafta (büyükten küçüğe)' },
  { value: 'projectName,asc', label: 'Proje (A–Z)' },
]

/**
 * /reports: tum projelerin haftalik raporlari, filtreli ve sayfali (dashboard yalnizca son raporu
 * gosterir; gecmise bakmak icin bu liste kullanilir). Durum adreste tutulur.
 */
export function ReportListPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const state = useMemo(() => parseReportList(searchParams), [searchParams])
  const result = useReportSearch(state.filters, state.page - 1, state.sort)

  const go = (next: ReportListState) => navigate({ search: reportListSearch(next) }, { replace: true })
  const setFilters = (filters: ReportFilters) => go({ ...state, filters, page: 1 })

  return (
    <Stack gap="md">
      <PageHeader title="Haftalık raporlar" description="Tüm projelerin raporları; geçmiş haftalar dahil." />
      <ReportFilterBar filters={state.filters} onChange={setFilters} />

      <QueryState query={result}>
        {(data) => (
          <Box style={{ opacity: result.isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <Stack gap="sm">
              <ResultHeader
                total={data.totalElements}
                noun="rapor"
                sortOptions={SORT_OPTIONS}
                sort={state.sort}
                onSortChange={(sort) => go({ ...state, sort, page: 1 })}
              />
              <ReportTable reports={data.content} />
              <ListPagination totalPages={data.totalPages} page={state.page} onChange={(page) => go({ ...state, page })} />
            </Stack>
          </Box>
        )}
      </QueryState>
    </Stack>
  )
}

function ReportTable({ reports }: { reports: WeeklyReport[] }) {
  return (
    <Paper withBorder>
      <Table.ScrollContainer minWidth={960}>
        <Table striped highlightOnHover verticalSpacing="sm" style={{ whiteSpace: 'nowrap' }}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Proje</Table.Th>
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
            {reports.length === 0 ? (
              <Table.Tr>
                <Table.Td colSpan={8}>
                  <Text c="dimmed" ta="center" py="md">
                    Filtreye uyan rapor yok.
                  </Text>
                </Table.Td>
              </Table.Tr>
            ) : (
              reports.map((report) => (
                <Table.Tr key={report.id}>
                  <Table.Td>
                    <Anchor component={Link} to={`/projects/${report.projectId}`} size="sm">
                      {report.projectName}
                    </Anchor>
                  </Table.Td>
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
                    <ProgressValue value={report.progressPercentage} label={`İlerleme ${report.projectName}`} />
                  </Table.Td>
                  <Table.Td>
                    <EnumBadge value={report.overallStatus} labels={OVERALL_STATUS_LABELS} />
                  </Table.Td>
                  <Table.Td>
                    <EnumBadge value={report.riskLevel} labels={RISK_LEVEL_LABELS} />
                  </Table.Td>
                  <Table.Td ta="right">{report.liveTaskCount}</Table.Td>
                </Table.Tr>
              ))
            )}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  )
}
