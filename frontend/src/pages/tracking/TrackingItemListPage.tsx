import { Box, Stack } from '@mantine/core'
import { useMemo } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { useTrackingItemSearch, type TrackingItemSort } from '../../api/trackingItems'
import { ListPagination, ResultHeader } from '../../components/ListControls'
import { PageHeader } from '../../components/PageHeader'
import { QueryState } from '../../components/QueryState'
import {
  parseTrackingList,
  toApiFilters,
  trackingListSearch,
  type TrackingListFilters,
  type TrackingListState,
} from './filters'
import { TrackingItemFilterBar } from './TrackingItemFilterBar'
import { TrackingItemTable } from './TrackingItemTable'

const SORT_OPTIONS: { value: TrackingItemSort; label: string }[] = [
  { value: 'followUpDate,asc', label: 'Takip tarihi (yakın önce)' },
  { value: 'createdAt,desc', label: 'En yeni kayıt önce' },
  { value: 'title,asc', label: 'Başlık (A–Z)' },
  { value: 'projectName,asc', label: 'Proje (A–Z)' },
]

/** /tracking-items: projeler arasi risk, engel, karar ve aksiyon listesi. Durum adreste tutulur. */
export function TrackingItemListPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const state = useMemo(() => parseTrackingList(searchParams), [searchParams])
  const apiFilters = useMemo(() => toApiFilters(state.filters), [state.filters])
  const result = useTrackingItemSearch(apiFilters, state.page - 1, state.sort)

  const go = (next: TrackingListState) => navigate({ search: trackingListSearch(next) }, { replace: true })
  // Filtre ya da siralama degisince 1. sayfaya donulur; eski sayfa numarasi yeni sonucta olmayabilir.
  const setFilters = (filters: TrackingListFilters) => go({ ...state, filters, page: 1 })

  return (
    <Stack gap="md">
      <PageHeader
        title="Riskler ve aksiyonlar"
        description="Tüm projelerin risk, engel, karar ve aksiyon kayıtları. Takip tarihi bugünden önce olan açık kayıtlar gecikmede sayılır."
      />
      <TrackingItemFilterBar filters={state.filters} onChange={setFilters} />

      <QueryState query={result}>
        {(data) => (
          <Box style={{ opacity: result.isPlaceholderData ? 0.6 : 1, transition: 'opacity 150ms' }}>
            <Stack gap="sm">
              <ResultHeader
                total={data.totalElements}
                noun="kayıt"
                sortOptions={SORT_OPTIONS}
                sort={state.sort}
                onSortChange={(sort) => go({ ...state, sort, page: 1 })}
              />
              <TrackingItemTable items={data.content} showProject emptyText="Filtreye uyan kayıt yok." />
              <ListPagination totalPages={data.totalPages} page={state.page} onChange={(page) => go({ ...state, page })} />
            </Stack>
          </Box>
        )}
      </QueryState>
    </Stack>
  )
}
