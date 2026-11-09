import { MantineProvider } from '@mantine/core'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, type RenderOptions } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'
import { MemoryRouter } from 'react-router'

import { theme } from '../theme'

interface Options extends Omit<RenderOptions, 'wrapper'> {
  /** Sayfanin acildigi adres (or. '/?year=2026'). */
  route?: string
}

/**
 * Bilesenleri uygulamadaki saglayicilarla (Mantine, TanStack Query, router) render eder.
 * Her test kendi QueryClient'ini alir; testler arasi onbellek paylasilmaz ve hata testleri tekrar denenmez.
 */
export function renderWithProviders(ui: ReactElement, { route = '/', ...options }: Options = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <MantineProvider theme={theme}>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
        </QueryClientProvider>
      </MantineProvider>
    )
  }
  return { queryClient, ...render(ui, { wrapper: Wrapper, ...options }) }
}
