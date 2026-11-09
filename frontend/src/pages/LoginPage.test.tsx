import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes, useLocation } from 'react-router'
import { describe, expect, it } from 'vitest'

import { endSession, getSession } from '../auth/session'
import { RequireAuth } from '../auth/RequireAuth'
import { mockApi, requests, validationError } from '../test/mockApi'
import { renderWithProviders } from '../test/render'
import { ADMIN, loginResponse, signIn } from '../test/session'
import { LoginPage } from './LoginPage'

function CurrentPage({ name }: { name: string }) {
  const location = useLocation()
  return <p>{`${name} ${location.pathname}${location.search}`}</p>
}

/** Uygulamadaki gibi: /login acik, diger sayfalar oturum ister. */
function renderApp(route: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="*"
        element={
          <RequireAuth>
            <CurrentPage name="korunan sayfa" />
          </RequireAuth>
        }
      />
    </Routes>,
    { route },
  )
}

async function fillAndSubmit(email: string, password: string) {
  const user = userEvent.setup()
  if (email) await user.type(screen.getByLabelText(/E-posta/), email)
  if (password) await user.type(screen.getByLabelText(/Parola/), password)
  await user.click(screen.getByRole('button', { name: 'Giriş yap' }))
  return user
}

describe('LoginPage', () => {
  it('oturum yokken korunan sayfa girise yonlendirir; giristen sonra ayni adrese doner', async () => {
    const fetchMock = mockApi({ 'POST /api/auth/login': loginResponse() })
    const { queryClient } = renderApp('/projects?page=2')
    queryClient.setQueryData(['projects'], ['onceki oturumun verisi'])

    await fillAndSubmit('  admin@example.com ', 'Demo1234!')

    expect(await screen.findByText('korunan sayfa /projects?page=2')).toBeInTheDocument()
    expect(requests(fetchMock)[0].body).toEqual({ email: 'admin@example.com', password: 'Demo1234!' })
    expect(getSession()?.user).toEqual(ADMIN)
    // Onceki kullanicinin onbellekteki verisi yeni oturuma kalmaz.
    expect(queryClient.getQueryData(['projects'])).toBeUndefined()
  })

  it('bilerek cikis yapildiysa sonraki giris kaldigi sayfaya degil ana sayfaya gider', async () => {
    signIn()
    endSession('logout')
    mockApi({ 'POST /api/auth/login': loginResponse() })
    renderApp('/projects/2')

    await fillAndSubmit('ayse@example.com', 'Demo1234!')

    expect(await screen.findByText('korunan sayfa /')).toBeInTheDocument()
  })

  it('hatali giriste backend mesajini gosterir, parolayi temizler ve sayfada kalir', async () => {
    mockApi({
      'POST /api/auth/login': () => ({
        status: 401,
        body: { status: 401, code: 'INVALID_CREDENTIALS', message: 'E-posta ya da parola hatali.' },
      }),
    })
    renderApp('/login')

    await fillAndSubmit('admin@example.com', 'yanlis')

    expect(await screen.findByText('E-posta ya da parola hatali.')).toBeInTheDocument()
    expect(screen.getByLabelText(/Parola/)).toHaveValue('')
    expect(getSession()).toBeNull()
  })

  it('bos alanlarda istek gondermez', async () => {
    const fetchMock = mockApi({})
    renderApp('/login')

    await fillAndSubmit('', '')

    expect(screen.getByText('E-posta zorunludur.')).toBeInTheDocument()
    expect(screen.getByText('Parola zorunludur.')).toBeInTheDocument()
    expect(requests(fetchMock)).toHaveLength(0)
  })

  it('backend alan hatalarini alanlarin altinda gosterir', async () => {
    mockApi({
      'POST /api/auth/login': () =>
        validationError([{ field: 'password', message: 'Parola en fazla 72 karakter olabilir.' }]),
    })
    renderApp('/login')

    await fillAndSubmit('admin@example.com', 'x')

    expect(await screen.findByText('Parola en fazla 72 karakter olabilir.')).toBeInTheDocument()
    expect(screen.queryByText('Giriş yapılamadı')).not.toBeInTheDocument()
  })

  it('backend kapaliyken ag hatasini gosterir', async () => {
    renderApp('/login')

    await fillAndSubmit('admin@example.com', 'Demo1234!')

    expect(await screen.findByText(/Sunucuya ulaşılamadı/)).toBeInTheDocument()
  })

  it('oturum aciksa giris sayfasi ana sayfaya yonlendirir', () => {
    signIn()
    renderApp('/login')

    expect(screen.getByText('korunan sayfa /')).toBeInTheDocument()
  })

  it('suresi dolan oturumdan sonra bir not gosterir', () => {
    signIn(ADMIN, -1000)
    renderApp('/reports')

    expect(screen.getByText('Oturum sona erdi')).toBeInTheDocument()
  })
})
