import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { AuditEvent, PageResponse } from '../../../api/types'
import { mockApi, requestedUrls, type MockRequest } from '../../../test/mockApi'
import { renderWithProviders } from '../../../test/render'
import { CTO, signIn } from '../../../test/session'
import { ProjectHistorySection } from './ProjectHistorySection'

const PATH = '/api/projects/2/history'

function event(id: number, overrides: Partial<AuditEvent> = {}): AuditEvent {
  return {
    id,
    occurredAt: '2026-11-10T11:32:05Z',
    actorId: 2,
    actorName: 'Mehmet Kaya',
    action: 'GUNCELLENDI',
    entityType: 'TAKIP_KAYDI',
    entityId: 4,
    summary: 'Test ortamina erisim yok',
    changes: [{ field: 'riskLevel', oldValue: 'ORTA', newValue: 'YUKSEK' }],
    ...overrides,
  }
}

function page(content: AuditEvent[], pageNumber = 0, totalElements = content.length): PageResponse<AuditEvent> {
  const size = 20
  return { content, page: pageNumber, size, totalElements, totalPages: Math.ceil(totalElements / size) }
}

const EVENTS = [
  event(3),
  event(2, {
    action: 'OLUSTURULDU',
    entityType: 'IS_KALEMI',
    entityId: 9,
    summary: 'VPN testi',
    occurredAt: '2026-11-09T07:05:00Z',
    actorName: 'Sistem Admin',
    changes: [
      { field: 'title', oldValue: null, newValue: 'VPN testi' },
      { field: 'status', oldValue: null, newValue: 'PLANLANDI' },
      { field: 'plannedDate', oldValue: null, newValue: '2026-11-20' },
    ],
  }),
  event(1, { action: 'SILINDI', entityType: 'RAPOR', entityId: 5, summary: '2026 / 30. hafta', changes: [] }),
]

function setup(handler: (request: MockRequest) => PageResponse<AuditEvent>) {
  const fetchMock = mockApi({ [PATH]: (request: MockRequest) => ({ body: handler(request) }) })
  renderWithProviders(<ProjectHistorySection projectId={2} />)
  return fetchMock
}

describe('ProjectHistorySection', () => {
  beforeEach(() => signIn(CTO))
  afterEach(() => vi.unstubAllGlobals())

  it('olaylar: islem rozeti, kayit, yazan kisi ve Turkiye saati; degisen alanlar etiketleriyle', async () => {
    setup(() => page(EVENTS))

    const items = await screen.findAllByRole('listitem')
    expect(items).toHaveLength(3)

    const update = items[0]
    expect(within(update).getByText('Güncellendi')).toBeInTheDocument()
    expect(within(update).getByText('Risk / aksiyon: Test ortamina erisim yok')).toBeInTheDocument()
    expect(within(update).getByText(/Mehmet Kaya/)).toHaveTextContent('Mehmet Kaya, 10.11.2026 14:32')
    // Eski deger ustu cizili, yeni deger kalin; ekran okuyucu "yeni değer" duyar.
    expect(within(update).getByText('Risk seviyesi:')).toBeInTheDocument()
    expect(within(update).getByText('Orta')).toHaveStyle({ textDecoration: 'line-through' })
    expect(within(update).getByText('Yüksek')).toBeInTheDocument()
    expect(within(update).getByText(', yeni değer:')).toBeInTheDocument()

    const create = items[1]
    expect(within(create).getByText('Eklendi')).toBeInTheDocument()
    expect(within(create).getByText('İş kalemi: VPN testi')).toBeInTheDocument()
    expect(within(create).getByText('Planlandı')).toBeInTheDocument()
    expect(within(create).getByText('20.11.2026')).toBeInTheDocument()
    expect(within(create).queryByText('boş')).not.toBeInTheDocument() // eklemede eski deger gosterilmez

    const deletion = items[2]
    expect(within(deletion).getByText('Silindi')).toBeInTheDocument()
    expect(within(deletion).getByText('Haftalık rapor: 2026 / 30. hafta')).toBeInTheDocument()

    expect(screen.getByText('3 / 3 değişiklik gösteriliyor')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Daha fazla göster' })).not.toBeInTheDocument()
  })

  it('bosaltilan alan "boş" yazar', async () => {
    setup(() => page([event(1, { changes: [{ field: 'closedDate', oldValue: '2026-11-05', newValue: null }] })]))

    expect(await screen.findByText('05.11.2026')).toBeInTheDocument()
    expect(screen.getByText('boş')).toBeInTheDocument()
  })

  it('bos gecmis: ne zaman dolacagini ve demo verisinin yazilmadigini soyler', async () => {
    setup(() => page([]))

    expect(await screen.findByText(/henüz değişiklik kaydı yok/)).toHaveTextContent('Demo verisi geçmişe yazılmaz.')
  })

  it('kayit turu filtresi istege gider; o turde degisiklik yoksa ona gore yazar', async () => {
    const fetchMock = setup(({ url }) => (url.searchParams.get('entityType') === 'RAPOR' ? page([]) : page(EVENTS)))
    await screen.findAllByRole('listitem')

    await userEvent.click(screen.getByRole('combobox', { name: 'Kayıt türü' }))
    await userEvent.click(await screen.findByRole('option', { name: 'Haftalık rapor' }))

    expect(await screen.findByText('Bu projede "Haftalık rapor" türünde değişiklik yok.')).toBeInTheDocument()
    const urls = requestedUrls(fetchMock, PATH)
    expect(urls[0].searchParams.has('entityType')).toBe(false)
    expect(urls.at(-1)?.searchParams.get('entityType')).toBe('RAPOR')
    expect(urls.at(-1)?.searchParams.get('page')).toBe('0')
  })

  it('"Daha fazla göster" sonraki sayfayi listenin sonuna ekler', async () => {
    const first = Array.from({ length: 20 }, (_, i) => event(30 - i))
    const fetchMock = setup(({ url }) =>
      url.searchParams.get('page') === '1' ? page([event(10, { summary: 'En eski' })], 1, 21) : page(first, 0, 21),
    )

    expect(await screen.findByText('20 / 21 değişiklik gösteriliyor')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Daha fazla göster' }))

    expect(await screen.findByText('21 / 21 değişiklik gösteriliyor')).toBeInTheDocument()
    expect(screen.getAllByRole('listitem').at(-1)).toHaveTextContent('Risk / aksiyon: En eski')
    expect(screen.queryByRole('button', { name: 'Daha fazla göster' })).not.toBeInTheDocument()
    expect(requestedUrls(fetchMock, PATH).map((url) => url.searchParams.get('page'))).toEqual(['0', '1'])
  })
})
