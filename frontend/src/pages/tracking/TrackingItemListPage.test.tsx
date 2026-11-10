import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'

import type { PageResponse, TrackingItem } from '../../api/types'
import { PROJECTS, trackingItem } from '../../test/fixtures'
import { mockApi, requestedUrls } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { signIn } from '../../test/session'
import { TrackingItemListPage } from './TrackingItemListPage'

function page(content: TrackingItem[], totalElements = content.length, totalPages = 1): PageResponse<TrackingItem> {
  return { content, page: 0, size: 20, totalElements, totalPages }
}

const ITEMS = [
  trackingItem(1, { title: 'Kapsam toplantisi', type: 'AKSIYON', riskLevel: null, overdue: true }),
  trackingItem(5, { title: 'Test ortami yok', type: 'ENGEL', riskLevel: 'ORTA', projectId: 2, projectName: 'e-Belge' }),
]

function setup(route = '/tracking-items', content = ITEMS) {
  const fetchMock = mockApi({ '/api/tracking-items': page(content, content.length, 3), '/api/projects': PROJECTS })
  renderWithProviders(<TrackingItemListPage />, { route })
  return fetchMock
}

const lastSearch = (fetchMock: ReturnType<typeof mockApi>) => requestedUrls(fetchMock, '/api/tracking-items').at(-1)!

describe('TrackingItemListPage', () => {
  beforeEach(() => signIn())

  it('varsayilan olarak kapanmamis kayitlari takip tarihine gore ister; proje sutunu baglanti', async () => {
    const fetchMock = setup()

    const row = (await screen.findByText('Test ortami yok')).closest('tr')!
    expect(within(row).getByRole('link', { name: 'e-Belge' })).toHaveAttribute('href', '/projects/2')
    expect(screen.getByText('2 kayıt')).toBeInTheDocument()

    const url = lastSearch(fetchMock)
    expect(url.searchParams.get('status')).toBe('ACIK,DEVAM_EDIYOR')
    expect(url.searchParams.get('sort')).toBe('followUpDate,asc')
    expect(url.searchParams.get('page')).toBe('0')
  })

  it('adresteki filtreler backend parametrelerine donusur; "Tumu" durum filtresi gondermez', async () => {
    const fetchMock = setup('/tracking-items?scope=all&type=KARAR,RISK&riskLevel=YUKSEK&overdue=true&assignee=zeynep&projectId=1&page=2')
    await screen.findByText('Kapsam toplantisi')

    const url = lastSearch(fetchMock)
    expect(url.searchParams.has('status')).toBe(false)
    expect(url.searchParams.get('type')).toBe('RISK,KARAR')
    expect(url.searchParams.get('riskLevel')).toBe('YUKSEK')
    expect(url.searchParams.get('overdue')).toBe('true')
    expect(url.searchParams.get('assignee')).toBe('zeynep')
    expect(url.searchParams.get('projectId')).toBe('1')
    expect(url.searchParams.get('page')).toBe('1')
  })

  it('kapsam degisince 1. sayfaya doner ve yalnizca kapananlari ister', async () => {
    const fetchMock = setup('/tracking-items?page=2')
    await screen.findByText('Kapsam toplantisi')

    await userEvent.click(screen.getByRole('combobox', { name: 'Kayıtlar' }))
    await userEvent.click(await screen.findByRole('option', { name: 'Kapananlar' }))

    await waitFor(() => expect(lastSearch(fetchMock).searchParams.get('status')).toBe('KAPANDI'))
    expect(lastSearch(fetchMock).searchParams.get('page')).toBe('0')
  })

  it('sonuc yoksa filtreye uyan kayit olmadigini soyler', async () => {
    setup('/tracking-items', [])
    expect(await screen.findByText('Filtreye uyan kayıt yok.')).toBeInTheDocument()
  })
})
