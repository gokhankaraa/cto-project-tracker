import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { renderWithProviders } from '../test/render'
import { AppLayout } from './AppLayout'

describe('AppLayout', () => {
  it('bulunulan sayfanin menu ogesi aktif, hazir olmayan ekranlar pasif', () => {
    renderWithProviders(<AppLayout />, { route: '/' })
    const menu = within(screen.getByLabelText('Ana menü'))

    expect(menu.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('data-active')
    // Hazir olmayan ekran gorunur ama tiklanabilir bir baglanti degildir.
    expect(menu.getByRole('link', { name: 'Projeler' })).not.toHaveAttribute('data-active')
    expect(menu.getByText('Raporlar')).toBeInTheDocument()
    expect(menu.queryByRole('link', { name: /Raporlar/ })).not.toBeInTheDocument()
  })

  it('alt sayfada (/projects/3) ust menu ogesi aktif', () => {
    renderWithProviders(<AppLayout />, { route: '/projects/3' })
    const menu = within(screen.getByLabelText('Ana menü'))

    expect(menu.getByRole('link', { name: 'Projeler' })).toHaveAttribute('data-active')
    expect(menu.getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('data-active')
  })
})
