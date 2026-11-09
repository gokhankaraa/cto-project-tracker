// @vitest-environment node
import { describe, expect, it } from 'vitest'

import { parseBoolean, parseEnumList, parseIntInRange, toSearchString } from './searchParams'

const LEVELS = ['DUSUK', 'ORTA', 'YUKSEK'] as const

describe('parseEnumList', () => {
  it('tanimsiz ve tekrar eden degerleri atar, enum sirasina koyar', () => {
    expect(parseEnumList('YUKSEK,FOO,ORTA,YUKSEK', LEVELS)).toEqual(['ORTA', 'YUKSEK'])
  })

  it('parametre yoksa bos liste', () => {
    expect(parseEnumList(null, LEVELS)).toEqual([])
  })
})

describe('parseIntInRange', () => {
  it.each([
    ['2026', 2026],
    ['1999', null],
    ['abc', null],
    ['-5', null],
    ['20.5', null],
    [null, null],
  ])('%s -> %s', (raw, expected) => {
    expect(parseIntInRange(raw, 2000, 2100)).toBe(expected)
  })
})

describe('parseBoolean', () => {
  it('yalnizca true/false okunur', () => {
    expect(parseBoolean('true')).toBe(true)
    expect(parseBoolean('false')).toBe(false)
    expect(parseBoolean('yes')).toBeNull()
  })
})

describe('toSearchString', () => {
  it('bos degerleri yazmaz, listeyi virgulle birlestirir', () => {
    expect(toSearchString({ riskLevel: ['ORTA', 'YUKSEK'], year: 2026, ownerId: null, statuses: [], delayed: false })).toBe(
      '?riskLevel=ORTA,YUKSEK&year=2026&delayed=false',
    )
  })

  it('filtre yoksa bos metin', () => {
    expect(toSearchString({ year: null, riskLevel: [] })).toBe('')
  })
})
