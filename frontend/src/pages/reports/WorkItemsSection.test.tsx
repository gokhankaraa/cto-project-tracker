import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { report, workItem } from '../../test/fixtures'
import { mockApi, requests, validationError, type MockReply, type MockRequest } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { WorkItemsSection } from './WorkItemsSection'

const REPORT = report(4, 31)
const ITEMS = [workItem(10, { title: 'Rapor ekrani entegrasyonu', delayed: true }), workItem(11, { title: 'Regresyon testi' })]

type Handler = (request: MockRequest) => MockReply

function setup(handlers: Record<string, Handler> = {}) {
  const fetchMock = mockApi({ '/api/reports/4/work-items': ITEMS, ...handlers })
  renderWithProviders(<WorkItemsSection report={REPORT} />)
  return fetchMock
}

const writes = (fetchMock: ReturnType<typeof mockApi>) => requests(fetchMock).filter((r) => r.method !== 'GET')

describe('WorkItemsSection', () => {
  it('raporun is kalemlerini listeler', async () => {
    setup()

    const row = (await screen.findByText('Rapor ekrani entegrasyonu')).closest('tr')!
    expect(within(row).getByText('Gecikmede')).toBeInTheDocument()
    expect(within(row).getByText('30.07.2026')).toBeInTheDocument()
  })

  it('pencereden yeni is kalemi ekler', async () => {
    const fetchMock = setup({
      'POST /api/reports/4/work-items': ({ body }) => ({ status: 201, body: { ...ITEMS[0], ...(body as object), id: 12 } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'İş kalemi ekle' }))
    const dialog = await screen.findByRole('dialog', { name: 'Yeni iş kalemi' })

    await userEvent.type(within(dialog).getByRole('textbox', { name: /Başlık/ }), 'Canliya alma')
    await userEvent.type(within(dialog).getByRole('textbox', { name: 'Sorumlu' }), 'Ayse Yilmaz')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Ekle' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(writes(fetchMock)[0].body).toEqual({
      title: 'Canliya alma',
      description: null,
      assignee: 'Ayse Yilmaz',
      status: 'PLANLANDI',
      plannedDate: null,
      completedDate: null,
    })
  })

  it('bos baslikla istek atmaz; backend alan hatasini pencerede gosterir', async () => {
    const fetchMock = setup({
      'POST /api/reports/4/work-items': () => validationError([{ field: 'title', message: 'Başlık zorunludur.' }]),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'İş kalemi ekle' }))
    const dialog = await screen.findByRole('dialog', { name: 'Yeni iş kalemi' })

    await userEvent.click(within(dialog).getByRole('button', { name: 'Ekle' }))
    expect(await within(dialog).findByText('Başlık zorunludur.')).toBeInTheDocument()
    expect(writes(fetchMock)).toEqual([])

    await userEvent.type(within(dialog).getByRole('textbox', { name: /Başlık/ }), ' x ')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Ekle' }))
    await waitFor(() => expect(writes(fetchMock)).toHaveLength(1))
    expect(within(dialog).getByRole('textbox', { name: /Başlık/ })).toHaveAccessibleDescription('Başlık zorunludur.')
  })

  it('mevcut kalemi duzenler ve PUT ile gonderir', async () => {
    const fetchMock = setup({ 'PUT /api/work-items/11': ({ body }) => ({ body: { ...ITEMS[1], ...(body as object) } }) })
    await userEvent.click(await screen.findByRole('button', { name: 'Regresyon testi düzenle' }))
    const dialog = await screen.findByRole('dialog', { name: 'İş kalemini düzenle' })
    expect(within(dialog).getByRole('textbox', { name: /Başlık/ })).toHaveValue('Regresyon testi')

    await userEvent.click(within(dialog).getByRole('combobox', { name: /Durum/ }))
    await userEvent.click(await screen.findByRole('option', { name: 'Tamamlandı' }))
    await userEvent.click(within(dialog).getByRole('button', { name: 'Kaydet' }))

    await waitFor(() => expect(writes(fetchMock)).toHaveLength(1))
    expect(writes(fetchMock)[0]).toMatchObject({
      method: 'PUT',
      url: expect.objectContaining({ pathname: '/api/work-items/11' }),
      body: { title: 'Regresyon testi', status: 'TAMAMLANDI', plannedDate: '2026-07-30' },
    })
  })

  it('silme onayindan sonra DELETE gonderir', async () => {
    const fetchMock = setup({ 'DELETE /api/work-items/10': () => ({ status: 204 }) })
    await userEvent.click(await screen.findByRole('button', { name: 'Rapor ekrani entegrasyonu sil' }))
    const dialog = await screen.findByRole('dialog', { name: 'İş kalemini sil' })

    await userEvent.click(within(dialog).getByRole('button', { name: 'Sil' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(writes(fetchMock).map((r) => `${r.method} ${r.url.pathname}`)).toEqual(['DELETE /api/work-items/10'])
  })
})
