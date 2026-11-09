import { Alert, Button, Center, Loader, Stack, Text } from '@mantine/core'
import type { UseQueryResult } from '@tanstack/react-query'
import type { ReactNode } from 'react'

import { errorMessage } from '../api/client'

interface QueryStateProps<T> {
  query: UseQueryResult<T>
  /** Veri geldi ama gosterilecek kayit yoksa true; bos durum mesaji gosterilir. */
  isEmpty?: (data: T) => boolean
  /** Bos durumda gosterilecek icerik (or. "Filtreye uyan proje yok."). */
  empty?: ReactNode
  children: (data: T) => ReactNode
}

/**
 * Bir sorgunun yukleniyor / hata / bos / veri durumlarini tek bicimde gosterir; her ekran ayni
 * davranisi tekrar yazmaz. Hata mesaji backend'den gelir (ApiError.message).
 */
export function QueryState<T>({ query, isEmpty, empty = 'Kayıt yok.', children }: QueryStateProps<T>) {
  if (query.isPending) {
    return (
      <Center py="xl">
        <Loader size="sm" aria-label="Yükleniyor" />
      </Center>
    )
  }

  if (query.isError) {
    const message = errorMessage(query.error)
    return (
      <Alert color="red" title="Veriler yüklenemedi">
        <Stack gap="xs" align="flex-start">
          <Text size="sm">{message}</Text>
          <Button size="xs" variant="light" color="red" onClick={() => query.refetch()} loading={query.isFetching}>
            Tekrar dene
          </Button>
        </Stack>
      </Alert>
    )
  }

  if (isEmpty?.(query.data)) {
    return (
      <Text c="dimmed" ta="center" py="xl">
        {empty}
      </Text>
    )
  }

  return <>{children(query.data)}</>
}
