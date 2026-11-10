import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import type { DashboardSummary, User } from '../../api/types'
import { mockApi, requestedUrls } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { saveFile } from '../../lib/download'
import { DashboardPage } from './DashboardPage'

// Tarayici indirmesi jsdom'da yok; kaydetme cagrisi ve verilen ad kontrol edilir.
vi.mock('../../lib/download', () => ({ saveFile: vi.fn() }))

const USERS: User[] = [{ id: 1, fullName: 'Ayse Yilmaz', role: 'PROJECT_MANAGER' }]

/** Seed verisindeki /api/dashboard/summary yanitinin kisaltilmisi. */
const SUMMARY: DashboardSummary = {
  totalProjects: 2,
  highRiskProjects: 0,
  projectsWithoutReport: 1,
  totalLiveTasks: 1,
  delayedProjects: 1,
  openRisks: 1,
  openBlockers: 0,
  pendingDecisions: 0,
  overdueTrackingItems: 1,
  projects: [
    {
      projectId: 1,
      name: 'Atlas IK',
      customer: 'Demo Musteri A.S.',
      ownerFullName: 'Ayse Yilmaz',
      projectStatus: 'DEVAM_EDIYOR',
      hasReport: true,
      lastYear: 2026,
      lastWeekNumber: 31,
      progressStage: 'TEST',
      progressPercentage: 75,
      overallStatus: 'YOLUNDA',
      riskLevel: 'ORTA',
      liveTaskCount: 1,
      delayed: true,
      openTrackingItems: 2,
      overdueTrackingItems: 1,
      highestOpenRiskLevel: 'YUKSEK',
    },
    {
      projectId: 3,
      name: 'Nova POS',
      customer: null,
      ownerFullName: 'Ayse Yilmaz',
      projectStatus: 'PLANLANDI',
      hasReport: false,
      lastYear: null,
      lastWeekNumber: null,
      progressStage: null,
      progressPercentage: 0,
      overallStatus: null,
      riskLevel: null,
      liveTaskCount: 0,
      delayed: false,
      openTrackingItems: 0,
      overdueTrackingItems: 0,
      highestOpenRiskLevel: null,
    },
  ],
}

const SUMMARY_PATH = '/api/dashboard/summary'

function setup(route = '/', summary: DashboardSummary = SUMMARY) {
  const fetchMock = mockApi({ [SUMMARY_PATH]: summary, '/api/users': USERS, '/api/dashboard/trend': { weeks: [] } })
  renderWithProviders(<DashboardPage />, { route })
  return fetchMock
}

describe('DashboardPage', () => {
  it('sayaclari ve proje satirlarini gosterir', async () => {
    setup()

    const row = (await screen.findByText('Atlas IK')).closest('tr')!
    expect(within(row).getByText('2026 / 31. hafta')).toBeInTheDocument()
    expect(within(row).getByText('Gecikmede')).toBeInTheDocument()
    expect(within(row).getByText('Test')).toBeInTheDocument()

    const nova = screen.getByText('Nova POS').closest('tr')!
    expect(within(nova).getByText('Rapor yok')).toBeInTheDocument()

    expect(within(screen.getByRole('group', { name: 'Toplam proje' })).getByText('2')).toBeInTheDocument()
    expect(within(screen.getByRole('group', { name: 'Geciken' })).getByText('1')).toBeInTheDocument()
  })

  it('risk ve aksiyon kartlari sayilan kayitlarin listesine gider; proje satirinda acik kayitlar', async () => {
    setup()

    const overdue = await screen.findByRole('link', { name: 'Gecikmiş takip: 1, listeyi aç' })
    expect(overdue).toHaveAttribute('href', '/tracking-items?overdue=true')
    expect(screen.getByRole('link', { name: 'Açık risk: 1, listeyi aç' })).toHaveAttribute('href', '/tracking-items?type=RISK')
    expect(screen.getByRole('link', { name: 'Bekleyen karar: 0, listeyi aç' })).toHaveAttribute(
      'href',
      '/tracking-items?type=KARAR',
    )
    expect(screen.getByText('Bugünkü durum.')).toBeInTheDocument()

    const atlas = screen.getByText('Atlas IK').closest('tr')!
    expect(within(atlas).getByRole('link', { name: 'Atlas IK: 2 açık kayıt' })).toHaveAttribute(
      'href',
      '/tracking-items?projectId=1',
    )
    expect(within(atlas).getByText('Yüksek')).toBeInTheDocument()
    expect(within(atlas).getByText('1 gecikmiş')).toBeInTheDocument()
    // Kaydi olmayan proje: tire.
    const nova = screen.getByText('Nova POS').closest('tr')!
    expect(within(nova).queryByRole('link', { name: /açık kayıt/ })).not.toBeInTheDocument()
  })

  it('yil secilince takip kartlari bu secimden etkilenmedigini soyler', async () => {
    setup('/?year=2026&weekNumber=29')
    expect(await screen.findByText('Bugünkü durum; seçilen yıl ve hafta bu sayıları değiştirmez.')).toBeInTheDocument()
  })

  it('adresteki filtreleri istege aktarir; yilsiz hafta gonderilmez', async () => {
    const fetchMock = setup('/?riskLevel=ORTA,YUKSEK&ownerId=1&weekNumber=29')

    await screen.findByText('Atlas IK')
    const [url] = requestedUrls(fetchMock, SUMMARY_PATH)
    expect(url.searchParams.get('riskLevel')).toBe('ORTA,YUKSEK')
    expect(url.searchParams.get('ownerId')).toBe('1')
    expect(url.searchParams.has('weekNumber')).toBe(false)
  })

  it('referans yil ve hafta secilince aciklama degisir', async () => {
    setup('/?year=2026&weekNumber=29')

    expect(await screen.findByText('Her proje için 2026 yılı 29. haftaya kadarki en son rapora göre.')).toBeInTheDocument()
  })

  it('Filtreleri temizle filtresiz istek atar', async () => {
    const fetchMock = setup('/?riskLevel=YUKSEK')
    await screen.findByText('Atlas IK')

    await userEvent.click(screen.getByRole('button', { name: 'Filtreleri temizle' }))

    await waitFor(() => {
      const urls = requestedUrls(fetchMock, SUMMARY_PATH)
      expect(urls.at(-1)!.search).toBe('')
    })
    expect(screen.getByRole('button', { name: 'Filtreleri temizle' })).toBeDisabled()
  })

  it('filtreye uyan proje yoksa bos durum mesaji', async () => {
    setup('/', { ...SUMMARY, totalProjects: 0, projectsWithoutReport: 0, totalLiveTasks: 0, delayedProjects: 0, projects: [] })

    expect(await screen.findByText('Filtreye uyan proje yok.')).toBeInTheDocument()
  })

  it("Excel'e aktar: adresteki filtrelerle indirir ve dosyayi kaydeder", async () => {
    const fetchMock = mockApi({
      [SUMMARY_PATH]: SUMMARY,
      '/api/users': USERS,
      '/api/dashboard/trend': { weeks: [] },
      '/api/dashboard/export': () => ({ body: { dosya: true } }),
    })
    renderWithProviders(<DashboardPage />, { route: '/?riskLevel=YUKSEK&ownerId=1&year=2026' })

    await userEvent.click(await screen.findByRole('button', { name: "Excel'e aktar" }))

    await waitFor(() => expect(saveFile).toHaveBeenCalledTimes(1))
    expect(vi.mocked(saveFile).mock.calls[0][1]).toBe('cto-ozeti.xlsx')
    const url = requestedUrls(fetchMock, '/api/dashboard/export')[0]
    expect(Object.fromEntries(url.searchParams)).toEqual({ riskLevel: 'YUKSEK', ownerId: '1', year: '2026' })
  })

  it('indirme hatasini bildirimle gosterir', async () => {
    mockApi({
      [SUMMARY_PATH]: SUMMARY,
      '/api/users': USERS,
      '/api/dashboard/trend': { weeks: [] },
      '/api/dashboard/export': () => ({ status: 500, body: { status: 500, code: 'INTERNAL_ERROR', message: 'Beklenmeyen bir hata oluştu.' } }),
    })
    renderWithProviders(<DashboardPage />)

    await userEvent.click(await screen.findByRole('button', { name: "Excel'e aktar" }))

    expect(await screen.findByText('Excel dosyası indirilemedi')).toBeInTheDocument()
    expect(screen.getByText('Beklenmeyen bir hata oluştu.')).toBeInTheDocument()
  })
})
