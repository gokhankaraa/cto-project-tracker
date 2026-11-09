import { createBrowserRouter } from 'react-router'

import { AppLayout } from './layout/AppLayout'
import { HomePage } from './pages/HomePage'
import { NotFoundPage } from './pages/NotFoundPage'

/** Uygulamanin sayfalari. Faz 1 ekranlari (projeler, raporlar, dashboard) buraya eklenecek. */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
