import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes, useParams } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { WeeklyReport } from '../../api/types'
import { ATLAS, report } from '../../test/fixtures'
import { mockApi, requests } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { EditReportPage, NewReportPage } from './ReportFormPage'

function ReportStub() {
  return <p>Rapor detayı {useParams().id}</p>
}

function renderForm(route: string) {
  renderWithProviders(
    <Routes>
      <Route path="/projects/:id/reports/new" element={<NewReportPage />} />
      <Route path="/reports/:id/edit" element={<EditReportPage />} />
      <Route path="/reports/:id" element={<ReportStub />} />
    </Routes>,
    { route },
  )
}

const writes = (fetchMock: ReturnType<typeof mockApi>) => requests(fetchMock).filter((r) => r.method !== 'GET')

async function choose(label: RegExp, option: string) {
  await userEvent.click(screen.getByRole('combobox', { name: label }))
  await userEvent.click(await screen.findByRole('option', { name: option }))
}

const LATEST = report(4, 31, { progressStage: 'TEST', progressPercentage: 75, reportDate: '2026-08-03' })

// Varsayilan tarih ve hafta bugune bagli: testler pazartesi 10 Agustos 2026'da calisiyormus gibi.
// Yalnizca Date sahtelenir; zamanlayicilar gercek kalir (userEvent ve Mantine onlara dayanir).
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 7, 10, 9, 0))
})
afterEach(() => {
  vi.useRealTimers()
})

function apiForNew(post: Parameters<typeof mockApi>[0][string]) {
  return mockApi({
    '/api/projects/1': ATLAS,
    '/api/projects/1/reports': [report(1, 29), LATEST, report(2, 30)],
    'POST /api/projects/1/reports': post,
  })
}

describe('NewReportPage', () => {
  it('bugunden onceki hafta ve son kademeyle acilir; raporu olusturup detayina gider', async () => {
    const fetchMock = apiForNew(({ body }) => ({ status: 201, body: { ...LATEST, ...(body as object), id: 9 } }))
    renderForm('/projects/1/reports/new')

    const week = await screen.findByRole('textbox', { name: /Hafta/ })
    expect(week).toHaveValue('32')
    expect(screen.getByRole('combobox', { name: /İlerleme aşaması/ })).toHaveValue('Test (%75)')
    expect(screen.getByText('Son rapor: 2026 / 31. hafta, Test.')).toBeInTheDocument()

    expect(screen.getByLabelText(/Rapor tarihi/)).toHaveValue('2026-08-10')
    expect(screen.getByText('Bu rapor 2026 / 32. hafta olarak kaydedilir (03.08.2026 haftası).')).toBeInTheDocument()
    await choose(/Genel durum/, 'Riskli')
    await choose(/Risk/, 'Yüksek')
    // Tus tus yazmak her tusta formu yeniden cizer (testi ~2 sn yapiyordu); kirpma kurali icin yapistirmak yeter.
    await userEvent.click(screen.getByRole('textbox', { name: 'Bu hafta yapılanlar' }))
    await userEvent.paste('  Entegrasyon bitti.  ')
    await userEvent.click(screen.getByRole('button', { name: 'Oluştur' }))

    expect(await screen.findByText('Rapor detayı 9')).toBeInTheDocument()
    expect(writes(fetchMock)[0].body).toEqual({
      weekNumber: 32,
      reportDate: '2026-08-10',
      progressStage: 'TEST',
      overallStatus: 'RISKLI',
      riskLevel: 'YUKSEK',
      done: 'Entegrasyon bitti.',
      planned: null,
      risks: null,
      note: null,
    })
  })

  it('zorunlu secimler yapilmadan istek atmaz', async () => {
    const fetchMock = apiForNew(() => ({ status: 201 }))
    renderForm('/projects/1/reports/new')

    await userEvent.click(await screen.findByRole('button', { name: 'Oluştur' }))

    expect(await screen.findByText('Genel durum zorunludur.')).toBeInTheDocument()
    expect(screen.getByText('Risk seviyesi zorunludur.')).toBeInTheDocument()
    expect(writes(fetchMock)).toEqual([])
  })

  it('hafta ile tarih uyusmazsa istek atmadan mesaji tarih alaninda gosterir', async () => {
    const fetchMock = apiForNew(() => ({ status: 201 }))
    renderForm('/projects/1/reports/new')

    const week = await screen.findByRole('textbox', { name: /Hafta/ })
    await userEvent.clear(week)
    await userEvent.type(week, '29')
    await choose(/Genel durum/, 'Yolunda')
    await choose(/Risk/, 'Düşük')
    await userEvent.click(screen.getByRole('button', { name: 'Oluştur' }))

    expect(
      await screen.findByText(
        'Rapor en geç anlattığı haftanın ertesi haftasında yazılabilir (2026 / 29. hafta için son gün 26.07.2026).',
      ),
    ).toBeInTheDocument()
    expect(writes(fetchMock)).toEqual([])
  })

  it('ayni hafta icin rapor varsa (409) mesaji hafta alaninin altinda gosterir', async () => {
    const message = 'Bu proje icin 2026 yilinin 32. haftasina ait rapor zaten mevcut.'
    apiForNew(() => ({ status: 409, body: { status: 409, code: 'DUPLICATE_RESOURCE', message } }))
    renderForm('/projects/1/reports/new')

    await screen.findByRole('textbox', { name: /Hafta/ })
    await choose(/Genel durum/, 'Yolunda')
    await choose(/Risk/, 'Düşük')
    await userEvent.click(screen.getByRole('button', { name: 'Oluştur' }))

    expect(await screen.findByRole('textbox', { name: /Hafta/ })).toHaveAccessibleDescription(
      expect.stringContaining(message),
    )
    expect(screen.queryByText('Rapor kaydedilemedi')).not.toBeInTheDocument()
  })
})

describe('EditReportPage', () => {
  const CURRENT: WeeklyReport = report(4, 31, { progressStage: 'TEST', done: 'Testler yazildi.', reportDate: '2026-08-03' })

  function apiForEdit(put: Parameters<typeof mockApi>[0][string]) {
    return mockApi({ '/api/reports/4': CURRENT, 'PUT /api/reports/4': put })
  }

  it('yalnizca mevcut kademe ve bir sonraki secilebilir', async () => {
    apiForEdit(() => ({ body: CURRENT }))
    renderForm('/reports/4/edit')

    await userEvent.click(await screen.findByRole('combobox', { name: /İlerleme aşaması/ }))
    const listbox = await screen.findByRole('listbox')
    const option = (name: string) => within(listbox).getByRole('option', { name })

    expect(option('Analiz (%25)')).toHaveAttribute('data-combobox-disabled')
    expect(option('Geliştirme (%50)')).toHaveAttribute('data-combobox-disabled')
    expect(option('Test (%75)')).not.toHaveAttribute('data-combobox-disabled')
    expect(option('Tamamlandı (%100)')).not.toHaveAttribute('data-combobox-disabled')
  })

  it('mevcut degerlerle acilir ve degisikligi PUT ile gonderir', async () => {
    const fetchMock = apiForEdit(({ body }) => ({ body: { ...CURRENT, ...(body as object) } }))
    renderForm('/reports/4/edit')

    expect(await screen.findByRole('textbox', { name: 'Bu hafta yapılanlar' })).toHaveValue('Testler yazildi.')
    await choose(/İlerleme aşaması/, 'Tamamlandı (%100)')
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }))

    expect(await screen.findByText('Rapor detayı 4')).toBeInTheDocument()
    expect(writes(fetchMock)[0]).toMatchObject({
      method: 'PUT',
      body: { weekNumber: 31, reportDate: '2026-08-03', progressStage: 'TAMAMLANDI', done: 'Testler yazildi.' },
    })
  })

  it('kademe kurali ihlalini (400 is kurali) asama alaninin altinda gosterir', async () => {
    const message = 'Ilerleme kademesi geriye alinamaz: TEST -> ANALIZ.'
    apiForEdit(() => ({ status: 400, body: { status: 400, code: 'BUSINESS_RULE_VIOLATION', message } }))
    renderForm('/reports/4/edit')

    await userEvent.click(await screen.findByRole('button', { name: 'Kaydet' }))

    expect(await screen.findByRole('combobox', { name: /İlerleme aşaması/ })).toHaveAccessibleDescription(
      expect.stringContaining(message),
    )
  })
})
