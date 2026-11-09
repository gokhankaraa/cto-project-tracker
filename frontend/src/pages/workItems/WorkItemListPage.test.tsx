import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import type { PageResponse, WorkItem } from '../../api/types'
import { PROJECTS, workItem } from '../../test/fixtures'
import { mockApi, requestedUrls } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { WorkItemListPage } from './WorkItemListPage'

function page(content: WorkItem[], totalPages = 1): PageResponse<WorkItem> {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages }
}

const PATH = '/api/work-items'

function setup(route: string, result = page([workItem(1, { delayed: true }), workItem(2)], 3)) {
  const fetchMock = mockApi({ [PATH]: result, '/api/projects': PROJECTS })
  renderWithProviders(<WorkItemListPage />, { route })
  return fetchMock
}

const lastRequest = (fetchMock: ReturnType<typeof mockApi>) => requestedUrls(fetchMock, PATH).at(-1)!.searchParams

describe('WorkItemListPage', () => {
  it('is kalemlerini proje ve rapor baglantilariyla listeler', async () => {
    setup('/work-items')

    expect(await screen.findByText('Is kalemi 1')).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: '2026 / 31. hafta' })[0]).toHaveAttribute('href', '/reports/4')
    expect(screen.getAllByRole('link', { name: 'Atlas IK' })[0]).toHaveAttribute('href', '/projects/1')
    expect(screen.getByText('Gecikmede')).toBeInTheDocument()
  })

  it('adresteki filtre, sayfa ve siralamayi istege aktarir (sayfa backend icin 0 tabanli)', async () => {
    const fetchMock = setup('/work-items?status=GECIKTI&year=2026&page=2&sort=title,asc')
    await screen.findByText('Is kalemi 1')

    const params = lastRequest(fetchMock)
    expect(params.get('status')).toBe('GECIKTI')
    expect(params.get('year')).toBe('2026')
    expect(params.get('page')).toBe('1')
    expect(params.get('size')).toBe('20')
    expect(params.get('sort')).toBe('title,asc')
  })

  it('sonraki sayfa dugmesi bir sonraki sayfayi ister', async () => {
    const fetchMock = setup('/work-items')
    await screen.findByText('Is kalemi 1')

    await userEvent.click(screen.getByRole('button', { name: 'Sonraki sayfa' }))

    await waitFor(() => expect(lastRequest(fetchMock).get('page')).toBe('1'))
  })

  it('filtre temizlenince 1. sayfaya doner', async () => {
    const fetchMock = setup('/work-items?status=GECIKTI&page=3')
    await screen.findByText('Is kalemi 1')

    await userEvent.click(screen.getByRole('button', { name: 'Filtreleri temizle' }))

    await waitFor(() => {
      const params = lastRequest(fetchMock)
      expect(params.get('page')).toBe('0')
      expect(params.has('status')).toBe(false)
    })
  })

  it('sorumlu aramasi yazmayi birakinca tek istekle uygulanir', async () => {
    const fetchMock = setup('/work-items')
    await screen.findByText('Is kalemi 1')
    const before = requestedUrls(fetchMock, PATH).length

    await userEvent.type(screen.getByRole('textbox', { name: 'Sorumlu' }), 'meh')

    await waitFor(() => expect(lastRequest(fetchMock).get('assignee')).toBe('meh'))
    // Uc tus vurusu, tek istek.
    expect(requestedUrls(fetchMock, PATH).length).toBe(before + 1)
  })

  it('sonuc yoksa mesaj gosterir', async () => {
    setup('/work-items?delayed=true', page([]))

    expect(await screen.findByText('Filtreye uyan iş kalemi yok.')).toBeInTheDocument()
    expect(screen.getByText('0 iş kalemi')).toBeInTheDocument()
  })
})
