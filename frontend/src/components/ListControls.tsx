import { Group, Pagination, Select, Text } from '@mantine/core'

interface ResultHeaderProps<S extends string> {
  total: number
  /** Sayinin yanindaki ad: "iş kalemi", "rapor". */
  noun: string
  sortOptions: { value: S; label: string }[]
  sort: S
  onSortChange: (sort: S) => void
}

/** Sayfali listelerin ust satiri: toplam kayit sayisi ve siralama secimi. */
export function ResultHeader<S extends string>({ total, noun, sortOptions, sort, onSortChange }: ResultHeaderProps<S>) {
  return (
    <Group justify="space-between">
      <Text size="sm" c="dimmed">
        {total} {noun}
      </Text>
      <Select
        aria-label="Sıralama"
        data={sortOptions}
        value={sort}
        onChange={(value) => value && onSortChange(value as S)}
        allowDeselect={false}
        w={240}
      />
    </Group>
  )
}

const PAGINATION_LABELS = { first: 'İlk sayfa', previous: 'Önceki sayfa', next: 'Sonraki sayfa', last: 'Son sayfa' }

/** Sayfa gecisi; tek sayfalik sonucta gizlenir. page 1'den baslar (adresteki gibi). */
export function ListPagination({
  totalPages,
  page,
  onChange,
}: {
  totalPages: number
  page: number
  onChange: (page: number) => void
}) {
  if (totalPages <= 1) return null
  return (
    <Group justify="center">
      <Pagination
        total={totalPages}
        value={page}
        onChange={onChange}
        getControlProps={(control) => ({ 'aria-label': PAGINATION_LABELS[control] })}
      />
    </Group>
  )
}
