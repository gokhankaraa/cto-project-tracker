import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'

import { PROJECTS } from '../../test/fixtures'
import { mockApi } from '../../test/mockApi'
import { renderWithProviders } from '../../test/render'
import { CTO, PROJECT_MANAGER, signIn } from '../../test/session'
import { ProjectListPage } from './ProjectListPage'

describe('ProjectListPage', () => {
  // Yazma butonlari role gore gorunur; asagidaki rol testleri disinda testler Admin olarak calisir.
  beforeEach(() => signIn())

  it('projeleri detay baglantisiyla listeler', async () => {
    mockApi({ '/api/projects': PROJECTS })
    renderWithProviders(<ProjectListPage />, { route: '/projects' })

    expect(await screen.findByRole('link', { name: 'Atlas IK' })).toHaveAttribute('href', '/projects/1')
    expect(screen.getByRole('link', { name: 'Nova POS' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Yeni proje' })).toHaveAttribute('href', '/projects/new')
  })

  it('arama Turkce harf ve buyuk/kucuk harf farkini gozetmez', async () => {
    mockApi({ '/api/projects': PROJECTS })
    renderWithProviders(<ProjectListPage />, { route: '/projects' })
    await screen.findByText('Atlas IK')

    await userEvent.type(screen.getByLabelText('Proje ara'), 'ık')

    expect(screen.getByText('Atlas IK')).toBeInTheDocument()
    expect(screen.queryByText('e-Belge')).not.toBeInTheDocument()
  })

  it('adresteki arama uygulanir; sonuc yoksa mesaj gosterir', async () => {
    mockApi({ '/api/projects': PROJECTS })
    renderWithProviders(<ProjectListPage />, { route: '/projects?q=yok-boyle-proje' })

    expect(await screen.findByText('Aramaya uyan proje yok.')).toBeInTheDocument()
    expect(screen.getByLabelText('Proje ara')).toHaveValue('yok-boyle-proje')
  })

  it.each([
    ['CTO', CTO],
    ['proje yoneticisi', PROJECT_MANAGER],
  ])('%s "Yeni proje" butonunu gormez', async (_, user) => {
    signIn(user)
    mockApi({ '/api/projects': PROJECTS })
    renderWithProviders(<ProjectListPage />, { route: '/projects' })

    expect(await screen.findByRole('link', { name: 'Atlas IK' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Yeni proje' })).not.toBeInTheDocument()
  })
})
