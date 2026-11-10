// @vitest-environment node
import { describe, expect, it } from 'vitest'

import { foldText, formatDate, formatDateTime, formatWeek, matchesSearch, parseId } from './format'

describe('foldText / matchesSearch', () => {
  it('Turkce harfleri ve buyuk/kucuk harfi esitler', () => {
    expect(foldText('ÇAĞRI Şükrü İzmir ılık')).toBe('cagri sukru izmir ilik')
  })

  it.each([
    ['ik', 'Atlas IK', true],
    ['IK', 'Atlas IK', true],
    ['belge', 'e-Belge', true],
    ['musteri', 'Demo Müşteri A.Ş.', true],
    ['nova', 'Atlas IK', false],
    ['  ', 'Atlas IK', true],
  ])('"%s" ~ "%s" -> %s', (query, field, expected) => {
    expect(matchesSearch(query, field)).toBe(expected)
  })

  it('null alanlari atlar', () => {
    expect(matchesSearch('ayse', null, 'Ayse Yilmaz')).toBe(true)
  })
})

describe('formatDate / formatWeek', () => {
  it('ISO tarihi saat dilimine bagli olmadan gun.ay.yil yazar', () => {
    expect(formatDate('2026-07-20')).toBe('20.07.2026')
    expect(formatDate(null)).toBe('–')
  })

  it('yil ve hafta', () => {
    expect(formatWeek(2026, 31)).toBe('2026 / 31. hafta')
    expect(formatWeek(null, null)).toBe('–')
  })
})

describe('formatDateTime', () => {
  it('UTC ani Turkiye saatiyle gosterir (UTC+3), gun donumunde de', () => {
    expect(formatDateTime('2026-11-10T11:32:05Z')).toBe('10.11.2026 14:32')
    expect(formatDateTime('2026-12-31T22:15:00Z')).toBe('01.01.2027 01:15')
    expect(formatDateTime('2026-11-10T11:32:05.123456Z')).toBe('10.11.2026 14:32')
  })

  it('bos ya da gecersiz deger tire olur', () => {
    expect(formatDateTime(null)).toBe('–')
    expect(formatDateTime('dun')).toBe('–')
  })
})

describe('parseId', () => {
  it.each([
    ['3', 3],
    ['0', null],
    ['abc', null],
    ['1.5', null],
    ['99999999999999999999', null],
    [undefined, null],
  ])('%s -> %s', (raw, expected) => {
    expect(parseId(raw)).toBe(expected)
  })
})
