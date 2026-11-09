import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'

import { mockApi, requests } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { ProjectDetailPage } from './ProjectDetailPage'
import { ATLAS, report } from './testData'

function renderDetail(route = '/projects/1') {
  renderWithProviders(
    <Routes>
      <Route path="/projects/:id" element={<ProjectDetailPage />} />
      <Route path="/projects" element={<p>Proje listesi</p>} />
    </Routes>,
    { route },
  )
}

describe('ProjectDetailPage', () => {
  it('proje bilgilerini ve raporlari en yeni hafta ustte gosterir', async () => {
    mockApi({
      '/api/projects/1': ATLAS,
      // Backend sira garanti etmez.
      '/api/projects/1/reports': [report(1, 29), report(4, 31, { progressStage: 'TEST' }), report(2, 30)],
    })
    renderDetail()

    expect(await screen.findByRole('heading', { name: 'Atlas IK' })).toBeInTheDocument()
    expect(screen.getByText('Insan kaynaklari surecleri urunu')).toBeInTheDocument()

    const rows = (await screen.findAllByRole('row')).slice(1)
    expect(rows.map((row) => within(row).getAllByRole('cell')[0].textContent)).toEqual([
      '2026 / 31. hafta',
      '2026 / 30. hafta',
      '2026 / 29. hafta',
    ])
    // Raporu olan proje silinemez.
    expect(screen.getByRole('button', { name: 'Sil' })).toBeDisabled()
  })

  it('silme onaylaninca listeye doner', async () => {
    const fetchMock = mockApi({
      '/api/projects/1': ATLAS,
      '/api/projects/1/reports': [],
      'DELETE /api/projects/1': () => ({ status: 204 }),
    })
    renderDetail()
    expect(await screen.findByText('Bu proje için henüz rapor yok.')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Sil' }))
    const dialog = await screen.findByRole('dialog', { name: 'Projeyi sil' })
    await userEvent.click(within(dialog).getByRole('button', { name: 'Sil' }))

    expect(await screen.findByText('Proje listesi')).toBeInTheDocument()
    const sent = requests(fetchMock).map((r) => `${r.method} ${r.url.pathname}`)
    const deleteAt = sent.indexOf('DELETE /api/projects/1')
    expect(deleteAt).toBeGreaterThanOrEqual(0)
    // Silinen projenin sorgulari yenilenmez (yenilenseydi 404 alirdi). Not: buradaki /projects yolu
    // lazy olmadigi icin sayfa hemen kapanir; lazy gecis sirasindaki yaris tarayicida dogrulandi.
    expect(sent.slice(deleteAt + 1).filter((r) => r.startsWith('GET /api/projects/1'))).toEqual([])
  })

  it("backend 409 donerse mesaji onay penceresinde gosterir", async () => {
    mockApi({
      '/api/projects/1': ATLAS,
      '/api/projects/1/reports': [],
      'DELETE /api/projects/1': () => ({
        status: 409,
        body: { status: 409, code: 'RESOURCE_IN_USE', message: 'Bu projenin haftalik raporlari var; once raporlari silin.' },
      }),
    })
    renderDetail()
    await screen.findByText('Bu proje için henüz rapor yok.')

    await userEvent.click(screen.getByRole('button', { name: 'Sil' }))
    const dialog = await screen.findByRole('dialog', { name: 'Projeyi sil' })
    await userEvent.click(within(dialog).getByRole('button', { name: 'Sil' }))

    expect(await within(dialog).findByText('Bu projenin haftalik raporlari var; once raporlari silin.')).toBeInTheDocument()
    expect(screen.queryByText('Proje listesi')).not.toBeInTheDocument()
  })

  it('olmayan proje icin backend mesajini gosterir', async () => {
    mockApi({})
    renderDetail('/projects/99')
    expect(await screen.findByText('Mock yok: GET /api/projects/99')).toBeInTheDocument()
  })

  it('gecersiz id istek atmadan bulunamadi sayfasi', () => {
    const fetchMock = mockApi({})
    renderDetail('/projects/abc')

    expect(screen.getByRole('heading', { name: 'Sayfa bulunamadı' })).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
