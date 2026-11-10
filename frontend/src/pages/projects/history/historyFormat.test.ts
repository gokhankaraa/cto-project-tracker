import { describe, expect, it } from 'vitest'

import { fieldLabel, formatValue } from './historyFormat'

describe('fieldLabel', () => {
  it('formdaki etiketi verir; ture ozel etiket once gelir', () => {
    expect(fieldLabel('TAKIP_KAYDI', 'riskLevel')).toBe('Risk seviyesi')
    expect(fieldLabel('RAPOR', 'riskLevel')).toBe('Risk')
    expect(fieldLabel('PROJE', 'name')).toBe('Proje adı')
    expect(fieldLabel('PROJE', 'owner')).toBe('Sorumlu proje yöneticisi')
    expect(fieldLabel('RAPOR', 'done')).toBe('Bu hafta yapılanlar')
  })

  it('bilinmeyen alan adiyla gorunur', () => {
    expect(fieldLabel('PROJE', 'budget')).toBe('budget')
  })
})

describe('formatValue', () => {
  it('durum kaydin turune gore cevrilir', () => {
    expect(formatValue('PROJE', 'status', 'DEVAM_EDIYOR')).toBe('Devam ediyor')
    expect(formatValue('IS_KALEMI', 'status', 'TESTTE')).toBe('Testte')
    expect(formatValue('TAKIP_KAYDI', 'status', 'KAPANDI')).toBe('Kapandı')
  })

  it('enum alanlari etiketle, tarihler gun.ay.yil olarak; diger degerler aynen', () => {
    expect(formatValue('TAKIP_KAYDI', 'riskLevel', 'YUKSEK')).toBe('Yüksek')
    expect(formatValue('RAPOR', 'progressStage', 'GELISTIRME')).toBe('Geliştirme')
    expect(formatValue('TAKIP_KAYDI', 'type', 'ENGEL')).toBe('Engel')
    expect(formatValue('TAKIP_KAYDI', 'followUpDate', '2026-11-13')).toBe('13.11.2026')
    expect(formatValue('RAPOR', 'weekNumber', '45')).toBe('45')
    expect(formatValue('PROJE', 'owner', 'Mehmet Kaya')).toBe('Mehmet Kaya')
  })

  it('bos deger null; etiketi olmayan enum degeri ham haliyle', () => {
    expect(formatValue('TAKIP_KAYDI', 'closedDate', null)).toBeNull()
    expect(formatValue('TAKIP_KAYDI', 'riskLevel', 'KRITIK')).toBe('KRITIK')
  })
})
