import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { NO_DASHBOARD_FILTERS, type DashboardFilters } from '../../api/dashboard'
import type { TrendWeek } from '../../api/types'
import { mockApi, requestedUrls } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { TrendSection } from './TrendSection'

function week(weekNumber: number, overrides: Partial<TrendWeek> = {}): TrendWeek {
  return {
    year: 2026,
    weekNumber,
    weekStart: '2026-07-27',
    totalProjects: 3,
    projectsWithReport: 2,
    projectsByStatus: { YOLUNDA: 1, RISKLI: 1, GECIKMELI: 0, BLOKE: 0 },
    highRiskProjects: 1,
    delayedProjects: 1,
    averageProgress: 50,
    openRisks: 1,
    openBlockers: 2,
    pendingDecisions: 0,
    openActions: 3,
    openedTrackingItems: 1,
    closedTrackingItems: 0,
    ...overrides,
  }
}

function setup(filters: DashboardFilters = NO_DASHBOARD_FILTERS) {
  const fetchMock = mockApi({
    '/api/dashboard/trend': { weeks: [week(30, { weekStart: '2026-07-20' }), week(31, { projectsWithReport: 1 })] },
  })
  renderWithProviders(<TrendSection filters={filters} />)
  return fetchMock
}

describe('TrendSection', () => {
  it('istege yalnizca proje filtreleri ve referans gider; rapor filtreleri trende uygulanmaz', async () => {
    const fetchMock = setup({
      ...NO_DASHBOARD_FILTERS,
      ownerId: 1,
      projectStatus: ['DEVAM_EDIYOR'],
      year: 2026,
      weekNumber: 31,
      riskLevel: ['YUKSEK'],
      overallStatus: ['RISKLI'],
      delayed: true,
    })
    expect(await screen.findByText(/2026 yılı 31\. haftaya kadarki 12 hafta\./)).toBeInTheDocument()

    const url = requestedUrls(fetchMock, '/api/dashboard/trend')[0]
    expect(Object.fromEntries(url.searchParams)).toEqual({
      ownerId: '1',
      projectStatus: 'DEVAM_EDIYOR',
      year: '2026',
      weekNumber: '31',
    })
  })

  it('referans yoksa bu haftaya kadar', async () => {
    setup()
    expect(await screen.findByText(/Bu haftaya kadarki 12 hafta\./)).toBeInTheDocument()
  })

  it('tablo gorunumu grafiklerin sayilarini gosterir; rapor yok = toplam - raporlu', async () => {
    setup()
    const toggle = await screen.findByRole('button', { name: 'Tablo olarak göster' })
    expect(toggle).toHaveAttribute('aria-expanded', 'false')

    await userEvent.click(toggle)
    const table = await screen.findByRole('table', { name: 'Son 12 haftanın sayıları' })
    const row = within(table).getByText('2026 / 31. hafta').closest('tr')!
    // Raporlu, Yolunda, Riskli, Gecikmeli, Bloke, Rapor yok, Acik risk, Acik engel, Bekleyen karar, Acik aksiyon
    expect(within(row).getAllByRole('cell').slice(1).map((cell) => cell.textContent)).toEqual([
      '1', '1', '1', '0', '0', '2', '1', '2', '0', '3',
    ])
    expect(screen.getByRole('button', { name: 'Tabloyu gizle' })).toHaveAttribute('aria-expanded', 'true')
  })
})
