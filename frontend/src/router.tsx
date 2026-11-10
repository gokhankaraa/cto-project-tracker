import { createBrowserRouter, Outlet } from 'react-router'

import { RequireAuth } from './auth/RequireAuth'
import { RequirePermission } from './auth/RequirePermission'
import { AppLayout } from './layout/AppLayout'
import { NotFoundPage } from './pages/NotFoundPage'

/**
 * Uygulamanin sayfalari. Her sayfa ilk acildiginda ayri bir parca olarak yuklenir (lazy): ilk acilista
 * yalnizca cerceve ve acilan sayfa indirilir, tablolar / formlar gibi bilesenler gerektikce gelir.
 * Giris ekrani disindaki her sayfa oturum ister ({@link RequireAuth}).
 */
export const router = createBrowserRouter([
  {
    path: '/login',
    lazy: () => import('./pages/LoginPage').then((m) => ({ Component: m.LoginPage })),
  },
  {
    path: '/',
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
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
        path: 'projects/:id',
        lazy: () => import('./pages/projects/ProjectDetailPage').then((m) => ({ Component: m.ProjectDetailPage })),
      },
      {
        path: 'reports',
        lazy: () => import('./pages/reports/ReportListPage').then((m) => ({ Component: m.ReportListPage })),
      },
      {
        path: 'reports/:id',
        lazy: () => import('./pages/reports/ReportDetailPage').then((m) => ({ Component: m.ReportDetailPage })),
      },
      {
        path: 'work-items',
        lazy: () => import('./pages/workItems/WorkItemListPage').then((m) => ({ Component: m.WorkItemListPage })),
      },
      {
        path: 'tracking-items',
        lazy: () =>
          import('./pages/tracking/TrackingItemListPage').then((m) => ({ Component: m.TrackingItemListPage })),
      },
      {
        // Yazma formlari: rolu uymayan, adresi elle yazsa da formu degil "yetkiniz yok" gorur.
        element: (
          <RequirePermission permission="manageProjects">
            <Outlet />
          </RequirePermission>
        ),
        children: [
          {
            path: 'projects/new',
            lazy: () => import('./pages/projects/ProjectFormPage').then((m) => ({ Component: m.NewProjectPage })),
          },
          {
            path: 'projects/:id/edit',
            lazy: () => import('./pages/projects/ProjectFormPage').then((m) => ({ Component: m.EditProjectPage })),
          },
        ],
      },
      {
        element: (
          <RequirePermission permission="writeReports">
            <Outlet />
          </RequirePermission>
        ),
        children: [
          {
            path: 'projects/:id/reports/new',
            lazy: () => import('./pages/reports/ReportFormPage').then((m) => ({ Component: m.NewReportPage })),
          },
          {
            path: 'reports/:id/edit',
            lazy: () => import('./pages/reports/ReportFormPage').then((m) => ({ Component: m.EditReportPage })),
          },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
