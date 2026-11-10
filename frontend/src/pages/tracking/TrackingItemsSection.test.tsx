import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { trackingItem } from '../../test/fixtures'
import { mockApi, requests, validationError, type MockReply, type MockRequest } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { CTO, PROJECT_MANAGER, signIn } from '../../test/session'
import { TrackingItemsSection } from './TrackingItemsSection'

const ITEMS = [
  trackingItem(1, {
    type: 'AKSIYON',
    title: 'Kapsam toplantisi',
    riskLevel: null,
    followUpDate: '2026-11-08',
    overdue: true,
    actionPlan: 'Rapor ekranlari netlestirilecek',
  }),
  trackingItem(2, { title: 'API gec gelebilir', impact: 'Entegrasyon gecikir' }),
  trackingItem(3, { type: 'KARAR', title: 'Saglayici secimi', riskLevel: null, assignee: 'Zeynep Demir' }),
  trackingItem(4, {
    type: 'KARAR',
    title: 'Kutuphane secimi',
    riskLevel: null,
    status: 'KAPANDI',
    followUpDate: null,
    closedDate: '2026-11-05',
  }),
]

type Handler = (request: MockRequest) => MockReply

function setup(handlers: Record<string, Handler> = {}) {
  const fetchMock = mockApi({ '/api/projects/1/tracking-items': ITEMS, ...handlers })
  renderWithProviders(<TrackingItemsSection projectId={1} />)
  return fetchMock
}

const writes = (fetchMock: ReturnType<typeof mockApi>) => requests(fetchMock).filter((r) => r.method !== 'GET')

async function choose(dialog: HTMLElement, label: RegExp, option: string) {
  await userEvent.click(within(dialog).getByRole('combobox', { name: label }))
  await userEvent.click(await screen.findByRole('option', { name: option }))
}

const setDate = (dialog: HTMLElement, value: string) =>
  fireEvent.change(within(dialog).getByLabelText(/Takip tarihi/), { target: { value } })

async function openNew() {
  await userEvent.click(await screen.findByRole('button', { name: 'Kayıt ekle' }))
  return screen.findByRole('dialog', { name: 'Yeni kayıt' })
}

// "Takip tarihi gecmiste olamaz" bugune bagli: testler 10 Kasim 2026'da calisiyormus gibi.
// Yalnizca Date sahtelenir; zamanlayicilar gercek kalir (userEvent ve Mantine onlara dayanir).
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 10, 10, 9, 0))
})
afterEach(() => {
  vi.useRealTimers()
})

describe('TrackingItemsSection', () => {
  beforeEach(() => signIn())

  it('kayitlari tur, gecikme, karar durumu ve kapanis tarihiyle listeler', async () => {
    setup()

    const overdue = (await screen.findByText('Kapsam toplantisi')).closest('tr')!
    expect(within(overdue).getByText('Gecikmede')).toBeInTheDocument()
    expect(within(overdue).getByText('Aksiyon: Rapor ekranlari netlestirilecek')).toBeInTheDocument()

    const risk = screen.getByText('API gec gelebilir').closest('tr')!
    expect(within(risk).getByText('Yüksek')).toBeInTheDocument()
    expect(within(risk).getByText('Etki: Entegrasyon gecikir')).toBeInTheDocument()

    expect(within(screen.getByText('Saglayici secimi').closest('tr')!).getByText('Karar bekleniyor')).toBeInTheDocument()
    const decided = screen.getByText('Kutuphane secimi').closest('tr')!
    expect(within(decided).getByText('Karar verildi')).toBeInTheDocument()
    expect(within(decided).getByText('Kapandı: 05.11.2026')).toBeInTheDocument()
  })

  it('pencereden risk ekler; istek govdesi formdaki degerlerle', async () => {
    const fetchMock = setup({
      'POST /api/projects/1/tracking-items': ({ body }) => ({ status: 201, body: { ...ITEMS[1], ...(body as object), id: 9 } }),
    })
    const dialog = await openNew()

    await userEvent.type(within(dialog).getByRole('textbox', { name: /Başlık/ }), 'Donanim gecikebilir')
    await choose(dialog, /Risk seviyesi/, 'Orta')
    await userEvent.type(within(dialog).getByRole('textbox', { name: /Sorumlu/ }), 'Ayse Yilmaz')
    setDate(dialog, '2026-11-20')
    await userEvent.type(within(dialog).getByRole('textbox', { name: /Etki/ }), 'Pilot kayar')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Ekle' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(writes(fetchMock)[0].body).toEqual({
      type: 'RISK',
      title: 'Donanim gecikebilir',
      description: null,
      riskLevel: 'ORTA',
      impact: 'Pilot kayar',
      actionPlan: null,
      assignee: 'Ayse Yilmaz',
      followUpDate: '2026-11-20',
      status: 'ACIK',
    })
  })

  it('aksiyon secilince risk seviyesi alani kalkar ve gonderilmez', async () => {
    const fetchMock = setup({
      'POST /api/projects/1/tracking-items': ({ body }) => ({ status: 201, body: { ...ITEMS[0], ...(body as object) } }),
    })
    const dialog = await openNew()

    await choose(dialog, /Risk seviyesi/, 'Yüksek')
    await userEvent.click(within(dialog).getByRole('radio', { name: 'Aksiyon' }))
    expect(within(dialog).queryByRole('combobox', { name: /Risk seviyesi/ })).not.toBeInTheDocument()

    await userEvent.type(within(dialog).getByRole('textbox', { name: /Başlık/ }), 'Toplanti')
    await userEvent.type(within(dialog).getByRole('textbox', { name: /Sorumlu/ }), 'Ayse')
    setDate(dialog, '2026-11-10')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Ekle' }))

    await waitFor(() => expect(writes(fetchMock)).toHaveLength(1))
    expect(writes(fetchMock)[0].body).toMatchObject({ type: 'AKSIYON', riskLevel: null, followUpDate: '2026-11-10' })
  })

  it('kurallar istek atmadan alanin altinda: risk seviyesi, sorumlu, takip tarihi, gecmis tarih', async () => {
    const fetchMock = setup()
    const dialog = await openNew()

    await userEvent.type(within(dialog).getByRole('textbox', { name: /Başlık/ }), 'Eksik kayit')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Ekle' }))
    expect(await within(dialog).findByText('Risk ve engel kayıtlarında risk seviyesi zorunludur.')).toBeInTheDocument()
    expect(within(dialog).getByText('Kapanmamış kaydın sorumlusu zorunludur.')).toBeInTheDocument()
    expect(within(dialog).getByText('Kapanmamış kaydın takip tarihi zorunludur.')).toBeInTheDocument()

    setDate(dialog, '2026-11-09')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Ekle' }))
    expect(await within(dialog).findByText('Takip tarihi bugünden önce olamaz.')).toBeInTheDocument()
    expect(writes(fetchMock)).toEqual([])
  })

  it('kapali kayit sorumlusuz ve tarihsiz eklenebilir', async () => {
    const fetchMock = setup({
      'POST /api/projects/1/tracking-items': ({ body }) => ({ status: 201, body: { ...ITEMS[3], ...(body as object) } }),
    })
    const dialog = await openNew()

    await userEvent.click(within(dialog).getByRole('radio', { name: 'Karar' }))
    await userEvent.type(within(dialog).getByRole('textbox', { name: /Başlık/ }), 'Verilmis karar')
    await choose(dialog, /Durum/, 'Karar verildi')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Ekle' }))

    await waitFor(() => expect(writes(fetchMock)).toHaveLength(1))
    expect(writes(fetchMock)[0].body).toMatchObject({ type: 'KARAR', status: 'KAPANDI', assignee: null, followUpDate: null })
  })

  it('gecikmis kaydin degismeyen eski tarihi korunur; PUT ile gonderilir', async () => {
    const fetchMock = setup({ 'PUT /api/tracking-items/1': ({ body }) => ({ body: { ...ITEMS[0], ...(body as object) } }) })
    await userEvent.click(await screen.findByRole('button', { name: 'Kapsam toplantisi düzenle' }))
    const dialog = await screen.findByRole('dialog', { name: 'Kaydı düzenle' })

    await choose(dialog, /Durum/, 'Devam ediyor')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Kaydet' }))

    await waitFor(() => expect(writes(fetchMock)).toHaveLength(1))
    expect(writes(fetchMock)[0]).toMatchObject({
      method: 'PUT',
      url: expect.objectContaining({ pathname: '/api/tracking-items/1' }),
      body: { status: 'DEVAM_EDIYOR', followUpDate: '2026-11-08' },
    })
  })

  it('backend alan hatasini pencerede alanin altinda gosterir', async () => {
    setup({
      'POST /api/projects/1/tracking-items': () =>
        validationError([{ field: 'followUpDate', message: 'Takip tarihi bugünden önce olamaz.' }]),
    })
    const dialog = await openNew()

    await userEvent.click(within(dialog).getByRole('radio', { name: 'Aksiyon' }))
    await userEvent.type(within(dialog).getByRole('textbox', { name: /Başlık/ }), 'x')
    await userEvent.type(within(dialog).getByRole('textbox', { name: /Sorumlu/ }), 'y')
    setDate(dialog, '2026-11-12')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Ekle' }))

    await waitFor(() =>
      expect(within(dialog).getByLabelText(/Takip tarihi/)).toHaveAccessibleDescription('Takip tarihi bugünden önce olamaz.'),
    )
  })

  it('silme onayindan sonra DELETE gonderir', async () => {
    const fetchMock = setup({ 'DELETE /api/tracking-items/2': () => ({ status: 204 }) })
    await userEvent.click(await screen.findByRole('button', { name: 'API gec gelebilir sil' }))
    const dialog = await screen.findByRole('dialog', { name: 'Kaydı sil' })

    await userEvent.click(within(dialog).getByRole('button', { name: 'Sil' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(writes(fetchMock).map((r) => `${r.method} ${r.url.pathname}`)).toEqual(['DELETE /api/tracking-items/2'])
  })

  it('kayit yoksa yazabilen kullaniciya nasil ekleyecegini soyler', async () => {
    mockApi({ '/api/projects/1/tracking-items': [] })
    renderWithProviders(<TrackingItemsSection projectId={1} />)

    expect(await screen.findByText(/Eklemek için "Kayıt ekle"yi kullanın/)).toBeInTheDocument()
  })

  it('CTO kayitlari gorur ama ekleyemez, duzenleyemez, silemez', async () => {
    signIn(CTO)
    setup()

    const row = (await screen.findByText('API gec gelebilir')).closest('tr')!
    expect(within(row).queryByRole('button')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Kayıt ekle' })).not.toBeInTheDocument()
    expect(screen.getAllByRole('columnheader')).toHaveLength(6)
  })

  it('proje yoneticisi kayit ekleyip duzenleyebilir', async () => {
    signIn(PROJECT_MANAGER)
    setup()

    expect(await screen.findByRole('button', { name: 'API gec gelebilir düzenle' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Kayıt ekle' })).toBeInTheDocument()
  })
})
