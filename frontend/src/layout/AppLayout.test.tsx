import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { getSession, sessionEndReason } from '../auth/session'
import { renderWithProviders } from '../test/render'
import { signIn } from '../test/session'
import { AppLayout } from './AppLayout'

describe('AppLayout', () => {
  it('tum ekranlar menude baglanti; bulunulan sayfanin ogesi aktif', () => {
    renderWithProviders(<AppLayout />, { route: '/' })
    const menu = within(screen.getByLabelText('Ana menü'))

    expect(menu.getAllByRole('link').map((link) => link.getAttribute('href'))).toEqual([
      '/',
      '/projects',
      '/reports',
      '/work-items',
    ])
    expect(menu.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('data-active')
    expect(menu.getByRole('link', { name: 'Projeler' })).not.toHaveAttribute('data-active')
  })

  it.each([
    ['/projects/3', 'Projeler'],
    ['/reports/4', 'Raporlar'],
    ['/work-items?page=2', 'İş kalemleri'],
  ])('alt sayfada (%s) ust menu ogesi aktif', (route, active) => {
    renderWithProviders(<AppLayout />, { route })
    const menu = within(screen.getByLabelText('Ana menü'))

    expect(menu.getByRole('link', { name: active })).toHaveAttribute('data-active')
    expect(menu.getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('data-active')
  })

  it('ust barda kullanicinin adi ve rolu; cikis oturumu kapatir ve onbellegi temizler', async () => {
    signIn({ id: 3, fullName: 'Zeynep Demir', role: 'CTO' })
    const { queryClient } = renderWithProviders(<AppLayout />, { route: '/' })
    queryClient.setQueryData(['projects'], ['CTO nun gordugu veri'])

    expect(screen.getByText('Zeynep Demir')).toBeInTheDocument()
    expect(screen.getByText('CTO')).toBeInTheDocument()

    await userEvent.setup().click(screen.getByRole('button', { name: 'Çıkış' }))

    expect(getSession()).toBeNull()
    expect(sessionEndReason()).toBe('logout')
    expect(queryClient.getQueryData(['projects'])).toBeUndefined()
  })
})
