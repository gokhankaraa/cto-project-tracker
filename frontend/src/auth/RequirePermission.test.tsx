import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { renderWithProviders } from '../test/render'
import { ADMIN, CTO, PROJECT_MANAGER, signIn } from '../test/session'
import { hasPermission } from './permissions'
import { RequirePermission } from './RequirePermission'

describe('hasPermission', () => {
  // Backend'deki security/Permissions tablosu: iki taraf ayni kalmali.
  it.each([
    ['ADMIN', true, true],
    ['CTO', false, false],
    ['PROJECT_MANAGER', false, true],
  ] as const)('%s: proje yonetimi %s, rapor yazma %s', (role, manageProjects, writeReports) => {
    expect(hasPermission(role, 'manageProjects')).toBe(manageProjects)
    expect(hasPermission(role, 'writeReports')).toBe(writeReports)
  })

  it('oturum yoksa hicbir izin yok', () => {
    expect(hasPermission(undefined, 'writeReports')).toBe(false)
  })
})

describe('RequirePermission', () => {
  const form = (
    <RequirePermission permission="manageProjects">
      <p>Proje formu</p>
    </RequirePermission>
  )

  it('izni olana sayfayi gosterir', () => {
    signIn(ADMIN)
    renderWithProviders(form)

    expect(screen.getByText('Proje formu')).toBeInTheDocument()
  })

  it.each([CTO, PROJECT_MANAGER])('$role adresi elle yazsa da formu degil "yetkiniz yok" gorur', (user) => {
    signIn(user)
    renderWithProviders(form)

    expect(screen.queryByText('Proje formu')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Yetkiniz yok' })).toBeInTheDocument()
  })
})
