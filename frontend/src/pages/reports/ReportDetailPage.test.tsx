import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes, useParams } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'

import { report } from '../../test/fixtures'
import { mockApi, requests } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { CTO, PROJECT_MANAGER, signIn } from '../../test/session'
import { ReportDetailPage } from './ReportDetailPage'

const REPORT = report(4, 31, {
  reportDate: '2026-08-03',
  progressStage: 'TEST',
  progressPercentage: 75,
  done: 'Regresyon testleri tamamlandi.',
  risks: 'Entegrasyon gecikmesi.',
})

function ProjectStub() {
  return <p>Proje detayı {useParams().id}</p>
}

function renderDetail(route = '/reports/4') {
  renderWithProviders(
    <Routes>
      <Route path="/reports/:id" element={<ReportDetailPage />} />
      <Route path="/projects/:id" element={<ProjectStub />} />
    </Routes>,
    { route },
  )
}

describe('ReportDetailPage', () => {
  // Yazma butonlari role gore gorunur; asagidaki rol testleri disinda testler Admin olarak calisir.
  beforeEach(() => signIn())

  it('rapor bilgilerini ve metinlerini gosterir', async () => {
    mockApi({ '/api/reports/4': REPORT })
    renderDetail()

    expect(await screen.findByRole('heading', { name: '2026 / 31. hafta' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '← Atlas IK' })).toHaveAttribute('href', '/projects/1')
    expect(screen.getByText('03.08.2026')).toBeInTheDocument()
    expect(screen.getByText('Regresyon testleri tamamlandi.')).toBeInTheDocument()
    // Bos metin alanlari
    expect(screen.getAllByText('Girilmemiş.')).toHaveLength(2)
    expect(screen.getByRole('link', { name: 'Düzenle' })).toHaveAttribute('href', '/reports/4/edit')
  })

  it('silme onaylaninca projeye doner', async () => {
    const fetchMock = mockApi({ '/api/reports/4': REPORT, 'DELETE /api/reports/4': () => ({ status: 204 }) })
    renderDetail()

    await userEvent.click(await screen.findByRole('button', { name: 'Sil' }))
    const dialog = await screen.findByRole('dialog', { name: 'Raporu sil' })
    expect(within(dialog).getByText(/bağlı tüm iş kalemleri silinecek/)).toBeInTheDocument()
    await userEvent.click(within(dialog).getByRole('button', { name: 'Sil' }))

    expect(await screen.findByText('Proje detayı 1')).toBeInTheDocument()
    expect(requests(fetchMock).some((r) => r.method === 'DELETE' && r.url.pathname === '/api/reports/4')).toBe(true)
  })

  it('CTO raporu duzenleyemez ve silemez', async () => {
    signIn(CTO)
    mockApi({ '/api/reports/4': REPORT })
    renderDetail()

    expect(await screen.findByRole('heading', { name: '2026 / 31. hafta' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Düzenle' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Sil' })).not.toBeInTheDocument()
  })

  it('proje yoneticisi raporu duzenleyebilir ve silebilir', async () => {
    signIn(PROJECT_MANAGER)
    mockApi({ '/api/reports/4': REPORT })
    renderDetail()

    expect(await screen.findByRole('link', { name: 'Düzenle' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sil' })).toBeInTheDocument()
  })
})
