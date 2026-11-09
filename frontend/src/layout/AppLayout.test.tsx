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
    expect(menu.getByText('Projeler')).toBeInTheDocument()
    expect(menu.queryByRole('link', { name: /Projeler/ })).not.toBeInTheDocument()
  })
})
