import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'

import { useUsers } from '../api/users'
import { LoginPage } from '../pages/LoginPage'
import { mockApi } from '../test/mockApi'
import { renderWithProviders } from '../test/render'
import { signIn } from '../test/session'
import { RequireAuth } from './RequireAuth'

function UsersPage() {
  const users = useUsers()
  return <p>{users.isSuccess ? `${users.data.length} kullanici` : 'yukleniyor'}</p>
}

function renderApp() {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/users"
        element={
          <RequireAuth>
            <UsersPage />
          </RequireAuth>
        }
      />
    </Routes>,
    { route: '/users' },
  )
}

describe('RequireAuth', () => {
  it('oturum varken sayfayi gosterir', async () => {
    signIn()
    mockApi({ '/api/users': [] })
    renderApp()

    expect(await screen.findByText('0 kullanici')).toBeInTheDocument()
  })

  it("sayfanin istegi 401 alirsa (token gecersiz) girise doner ve notu gosterir", async () => {
    signIn()
    mockApi({
      '/api/users': () => ({
        status: 401,
        body: { status: 401, code: 'UNAUTHORIZED', message: 'Gecersiz token.' },
      }),
    })
    renderApp()

    expect(await screen.findByRole('button', { name: 'Giriş yap' })).toBeInTheDocument()
    expect(screen.getByText('Oturum sona erdi')).toBeInTheDocument()
  })
})
