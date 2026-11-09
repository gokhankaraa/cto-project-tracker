import { createBrowserRouter } from 'react-router'

import { AppLayout } from './layout/AppLayout'
import { NotFoundPage } from './pages/NotFoundPage'

/**
 * Uygulamanin sayfalari. Her sayfa ilk acildiginda ayri bir parca olarak yuklenir (lazy): ilk acilista
 * yalnizca cerceve ve acilan sayfa indirilir, tablolar / formlar gibi bilesenler gerektikce gelir.
 * Faz 1'in kalan ekrani (rapor listesi) buraya eklenecek.
 */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        lazy: () => import('./pages/dashboard/DashboardPage').then((m) => ({ Component: m.DashboardPage })),
      },
      {
        path: 'projects',
        lazy: () => import('./pages/projects/ProjectListPage').then((m) => ({ Component: m.ProjectListPage })),
      },
      {
        path: 'projects/new',
        lazy: () => import('./pages/projects/ProjectFormPage').then((m) => ({ Component: m.NewProjectPage })),
      },
      {
        path: 'projects/:id',
        lazy: () => import('./pages/projects/ProjectDetailPage').then((m) => ({ Component: m.ProjectDetailPage })),
      },
      {
        path: 'projects/:id/edit',
        lazy: () => import('./pages/projects/ProjectFormPage').then((m) => ({ Component: m.EditProjectPage })),
      },
      {
        path: 'projects/:id/reports/new',
        lazy: () => import('./pages/reports/ReportFormPage').then((m) => ({ Component: m.NewReportPage })),
      },
      {
        path: 'reports/:id',
        lazy: () => import('./pages/reports/ReportDetailPage').then((m) => ({ Component: m.ReportDetailPage })),
      },
      {
        path: 'reports/:id/edit',
        lazy: () => import('./pages/reports/ReportFormPage').then((m) => ({ Component: m.EditReportPage })),
      },
      {
        path: 'work-items',
        lazy: () => import('./pages/workItems/WorkItemListPage').then((m) => ({ Component: m.WorkItemListPage })),
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
