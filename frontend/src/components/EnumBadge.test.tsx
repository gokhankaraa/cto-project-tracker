import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { RISK_LEVEL_LABELS } from '../lib/labels'
import { renderWithProviders } from '../test/render'
import { EnumBadge } from './EnumBadge'

describe('EnumBadge', () => {
  it('enum degerini Turkce etiketiyle gosterir', () => {
    renderWithProviders(<EnumBadge value="YUKSEK" labels={RISK_LEVEL_LABELS} />)
    expect(screen.getByText('Yüksek')).toBeInTheDocument()
  })

  it('deger yoksa (rapor yok) tire gosterir', () => {
    renderWithProviders(<EnumBadge value={null} labels={RISK_LEVEL_LABELS} />)
    expect(screen.getByText('–')).toBeInTheDocument()
  })
})
