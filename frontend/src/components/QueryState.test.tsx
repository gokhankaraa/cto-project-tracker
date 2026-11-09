import { useQuery } from '@tanstack/react-query'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { ApiRequestError } from '../api/client'
import { renderWithProviders } from '../test/render'
import { QueryState } from './QueryState'

function Example({ queryFn }: { queryFn: () => Promise<string[]> }) {
  const query = useQuery({ queryKey: ['example'], queryFn })
  return (
    <QueryState query={query} isEmpty={(items) => items.length === 0} empty="Proje yok.">
      {(items) => <ul>{items.map((item) => <li key={item}>{item}</li>)}</ul>}
    </QueryState>
  )
}

describe('QueryState', () => {
  it('once yukleniyor, sonra veriyi gosterir', async () => {
    renderWithProviders(<Example queryFn={() => Promise.resolve(['Atlas IK'])} />)

    expect(screen.getByLabelText('Yükleniyor')).toBeInTheDocument()
    expect(await screen.findByText('Atlas IK')).toBeInTheDocument()
  })

  it('kayit yoksa bos durum mesajini gosterir', async () => {
    renderWithProviders(<Example queryFn={() => Promise.resolve([])} />)

    expect(await screen.findByText('Proje yok.')).toBeInTheDocument()
  })

  it('404 te "Kayit bulunamadi" gosterir; tekrar dene yerine ana sayfa baglantisi', async () => {
    renderWithProviders(
      <Example
        queryFn={() => Promise.reject(new ApiRequestError(404, 'RESOURCE_NOT_FOUND', 'Haftalık rapor bulunamadı: 5'))}
      />,
    )

    expect(await screen.findByRole('heading', { name: 'Kayıt bulunamadı' })).toBeInTheDocument()
    expect(screen.getByText('Haftalık rapor bulunamadı: 5')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ana sayfaya dön' })).toHaveAttribute('href', '/')
    expect(screen.queryByRole('button', { name: 'Tekrar dene' })).not.toBeInTheDocument()
  })

  it("hatada backend'in mesajini gosterir; Tekrar dene sorguyu yeniler", async () => {
    const queryFn = vi
      .fn<() => Promise<string[]>>()
      .mockRejectedValueOnce(new ApiRequestError(500, 'INTERNAL_ERROR', 'Beklenmeyen bir hata olustu.'))
      .mockResolvedValueOnce(['Atlas IK'])
    renderWithProviders(<Example queryFn={queryFn} />)

    expect(await screen.findByText('Beklenmeyen bir hata olustu.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Tekrar dene' }))
    expect(await screen.findByText('Atlas IK')).toBeInTheDocument()
  })
})
