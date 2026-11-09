import { Alert, Anchor, Button, Center, Loader, Stack, Text, Title } from '@mantine/core'
import type { UseQueryResult } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

import { ApiRequestError, errorMessage } from '../api/client'

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
 *
 * 404 ayri gosterilir: tekrar denemek kaydi geri getirmez. Metin kaydin silinmis olmasini da, kullanicinin
 * goremedigi bir kayit olmasini da kapsar (proje yoneticisi baskasinin kaydinda da 404 alir); hangisi
 * oldugu soylenmez, backend de soylemez.
 */
export function QueryState<T>({ query, isEmpty, empty = 'Kayıt yok.', children }: QueryStateProps<T>) {
  if (query.isPending) {
    return (
      <Center py="xl">
        <Loader size="sm" aria-label="Yükleniyor" />
      </Center>
    )
  }

  if (query.isError && query.error instanceof ApiRequestError && query.error.status === 404) {
    return (
      <Stack gap="sm" py="md">
        <Title order={2}>Kayıt bulunamadı</Title>
        <Text c="dimmed">Kayıt silinmiş olabilir ya da bu kaydı görüntüleme yetkiniz yok.</Text>
        <Text size="sm" c="dimmed">
          {query.error.message}
        </Text>
        <Anchor component={Link} to="/">
          Ana sayfaya dön
        </Anchor>
      </Stack>
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
