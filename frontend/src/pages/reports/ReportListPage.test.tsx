import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import type { PageResponse, WeeklyReport } from '../../api/types'
import { PROJECTS, report, USERS } from '../../test/fixtures'
import { mockApi, requestedUrls } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { ReportListPage } from './ReportListPage'

function page(content: WeeklyReport[], totalPages = 1): PageResponse<WeeklyReport> {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages }
}

const REPORTS = [
  report(4, 31, { reportDate: '2026-08-03', progressStage: 'TEST', progressPercentage: 75 }),
  report(3, 30, { projectId: 2, projectName: 'e-Belge', riskLevel: 'YUKSEK', reportDate: '2026-07-27' }),
]

const PATH = '/api/reports'

function setup(route: string, result = page(REPORTS, 2)) {
  const fetchMock = mockApi({ [PATH]: result, '/api/projects': PROJECTS, '/api/users': USERS })
  renderWithProviders(<ReportListPage />, { route })
  return fetchMock
}

const lastRequest = (fetchMock: ReturnType<typeof mockApi>) => requestedUrls(fetchMock, PATH).at(-1)!.searchParams

describe('ReportListPage', () => {
  it('raporlari proje ve rapor baglantilariyla listeler', async () => {
    setup('/reports')

    const row = (await screen.findByRole('link', { name: 'e-Belge' })).closest('tr')!
    expect(within(row).getByRole('link', { name: '2026 / 30. hafta' })).toHaveAttribute('href', '/reports/3')
    expect(within(row).getByText('27.07.2026')).toBeInTheDocument()
    expect(within(row).getByText('Yüksek')).toBeInTheDocument()
    expect(screen.getByText('2 rapor')).toBeInTheDocument()
  })

  it('adresteki filtre, sayfa ve siralamayi istege aktarir; varsayilan siralama acikca gonderilir', async () => {
    const fetchMock = setup('/reports?year=2026&weekFrom=29&weekTo=30&riskLevel=ORTA,YUKSEK&page=2')
    await screen.findByText('2 rapor')

    const params = lastRequest(fetchMock)
    expect(Object.fromEntries(params)).toEqual({
      year: '2026',
      weekFrom: '29',
      weekTo: '30',
      riskLevel: 'ORTA,YUKSEK',
      page: '1',
      size: '20',
      sort: 'reportDate,desc',
    })
  })

  it('yil secilmeden hafta araligi pasif', async () => {
    setup('/reports')
    await screen.findByText('2 rapor')

    expect(screen.getByRole('combobox', { name: 'İlk hafta' })).toBeDisabled()
    expect(screen.getByRole('combobox', { name: 'Son hafta' })).toBeDisabled()
  })

  it('bitis haftasinda baslangictan onceki haftalar secilemez', async () => {
    setup('/reports?year=2026&weekFrom=30')
    await screen.findByText('2 rapor')

    await userEvent.click(screen.getByRole('combobox', { name: 'Son hafta' }))
    const listbox = await screen.findByRole('listbox')
    expect(within(listbox).getByRole('option', { name: '29. hafta' })).toHaveAttribute('data-combobox-disabled')
    expect(within(listbox).getByRole('option', { name: '30. hafta' })).not.toHaveAttribute('data-combobox-disabled')
  })

  it('siralama degisince 1. sayfadan ister', async () => {
    const fetchMock = setup('/reports?page=2')
    await screen.findByText('2 rapor')

    await userEvent.click(screen.getByRole('combobox', { name: 'Sıralama' }))
    await userEvent.click(await screen.findByRole('option', { name: 'Proje (A–Z)' }))

    await waitFor(() => {
      const params = lastRequest(fetchMock)
      expect(params.get('sort')).toBe('projectName,asc')
      expect(params.get('page')).toBe('0')
    })
  })

  it('sonuc yoksa mesaj gosterir', async () => {
    setup('/reports?year=2027', page([]))

    expect(await screen.findByText('Filtreye uyan rapor yok.')).toBeInTheDocument()
  })
})
