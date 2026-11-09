import { createBrowserRouter } from 'react-router'

import { AppLayout } from './layout/AppLayout'
import { DashboardPage } from './pages/dashboard/DashboardPage'
import { NotFoundPage } from './pages/NotFoundPage'

/** Uygulamanin sayfalari. Faz 1'in kalan ekranlari (projeler, raporlar, is kalemleri) buraya eklenecek. */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
