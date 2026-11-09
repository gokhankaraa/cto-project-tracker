import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { renderWithProviders } from '../test/render'
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
})
