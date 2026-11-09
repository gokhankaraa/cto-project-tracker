import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes, useParams } from 'react-router'
import { describe, expect, it } from 'vitest'

import { mockApi, requests, validationError } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { EditProjectPage, NewProjectPage } from './ProjectFormPage'
import { ATLAS, USERS } from './testData'

function DetailStub() {
  return <p>Proje detayı {useParams().id}</p>
}

function renderForm(route: string) {
  renderWithProviders(
    <Routes>
      <Route path="/projects/new" element={<NewProjectPage />} />
      <Route path="/projects/:id/edit" element={<EditProjectPage />} />
      <Route path="/projects/:id" element={<DetailStub />} />
    </Routes>,
    { route },
  )
}

const writes = (fetchMock: ReturnType<typeof mockApi>) => requests(fetchMock).filter((r) => r.method !== 'GET')

async function chooseOwner(name: string) {
  await userEvent.click(screen.getByRole('combobox', { name: /Sorumlu proje yöneticisi/ }))
  await userEvent.click(await screen.findByRole('option', { name }))
}

describe('NewProjectPage', () => {
  it('zorunlu alanlar bossa istek atmadan hatalari gosterir', async () => {
    const fetchMock = mockApi({ '/api/users': USERS })
    renderForm('/projects/new')

    await userEvent.click(screen.getByRole('button', { name: 'Oluştur' }))

    expect(await screen.findByText('Proje adı zorunludur.')).toBeInTheDocument()
    expect(screen.getByText('Sorumlu proje yöneticisi zorunludur.')).toBeInTheDocument()
    expect(writes(fetchMock)).toEqual([])
  })

  it('projeyi olusturur ve detayina gider; bos istege bagli alanlar null gider', async () => {
    const fetchMock = mockApi({
      '/api/users': USERS,
      'POST /api/projects': ({ body }) => ({ status: 201, body: { ...ATLAS, ...(body as object), id: 7 } }),
    })
    renderForm('/projects/new')

    await userEvent.type(screen.getByRole('textbox', { name: /Proje adı/ }), '  Yeni Portal  ')
    await chooseOwner('Mehmet Kaya')
    await userEvent.click(screen.getByRole('button', { name: 'Oluştur' }))

    expect(await screen.findByText('Proje detayı 7')).toBeInTheDocument()
    expect(screen.getByText('Proje oluşturuldu.')).toBeInTheDocument()
    expect(writes(fetchMock)[0].body).toEqual({
      name: 'Yeni Portal',
      customer: null,
      description: null,
      status: 'PLANLANDI',
      ownerId: 2,
    })
  })

  it("backend'in alan hatalarini ilgili alanin altina yazar", async () => {
    mockApi({
      '/api/users': USERS,
      'POST /api/projects': () => validationError([{ field: 'name', message: 'Proje adi zorunludur.' }]),
    })
    renderForm('/projects/new')

    await userEvent.type(screen.getByRole('textbox', { name: /Proje adı/ }), 'X')
    await chooseOwner('Ayse Yilmaz')
    await userEvent.click(screen.getByRole('button', { name: 'Oluştur' }))

    const nameInput = screen.getByRole('textbox', { name: /Proje adı/ })
    await waitFor(() => expect(nameInput).toHaveAccessibleDescription('Proje adi zorunludur.'))
    // Alan hatasi varken ayrica genel hata kutusu gosterilmez.
    expect(screen.queryByText('Proje kaydedilemedi')).not.toBeInTheDocument()
  })

  it('alan hatasi olmayan hatayi formun ustunde gosterir', async () => {
    mockApi({
      '/api/users': USERS,
      'POST /api/projects': () => ({
        status: 404,
        body: { status: 404, code: 'RESOURCE_NOT_FOUND', message: 'Kullanici bulunamadi: 2' },
      }),
    })
    renderForm('/projects/new')

    await userEvent.type(screen.getByRole('textbox', { name: /Proje adı/ }), 'X')
    await chooseOwner('Mehmet Kaya')
    await userEvent.click(screen.getByRole('button', { name: 'Oluştur' }))

    expect(await screen.findByText('Kullanici bulunamadi: 2')).toBeInTheDocument()
  })
})

describe('EditProjectPage', () => {
  it('mevcut degerlerle acilir ve degisikligi PUT ile gonderir', async () => {
    const fetchMock = mockApi({
      '/api/users': USERS,
      '/api/projects/1': ATLAS,
      'PUT /api/projects/1': ({ body }) => ({ body: { ...ATLAS, ...(body as object) } }),
    })
    renderForm('/projects/1/edit')

    const name = await screen.findByRole('textbox', { name: /Proje adı/ })
    expect(name).toHaveValue('Atlas IK')
    await userEvent.clear(name)
    await userEvent.type(name, 'Atlas IK v2')
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }))

    expect(await screen.findByText('Proje detayı 1')).toBeInTheDocument()
    expect(writes(fetchMock)[0]).toMatchObject({
      method: 'PUT',
      body: { name: 'Atlas IK v2', customer: 'Demo Musteri A.S.', status: 'DEVAM_EDIYOR', ownerId: 1 },
    })
  })
})
