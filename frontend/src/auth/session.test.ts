import { describe, expect, it, vi } from 'vitest'

import { ADMIN, loginResponse, signIn } from '../test/session'
import { endSession, getSession, saveSession, sessionEndReason, subscribeSession } from './session'

const STORAGE_KEY = 'ctotracker.session'

describe('session', () => {
  it('kaydedilen oturum okunur, localStorage a yazilir ve dinleyicilere bildirilir', () => {
    const listener = vi.fn()
    const unsubscribe = subscribeSession(listener)
    const response = loginResponse()

    saveSession(response)

    expect(getSession()).toBe(response)
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY)!)).toEqual(response)
    expect(listener).toHaveBeenCalledTimes(1)
    unsubscribe()
  })

  it('suresi dolmus token oturum sayilmaz; bitis nedeni "expired"', () => {
    signIn(ADMIN, -1000)

    expect(getSession()).toBeNull()
    expect(sessionEndReason()).toBe('expired')
  })

  it('cikis oturumu ve kaydi siler; ardindan gelen 401 nedeni degistirmez', () => {
    signIn()

    endSession('logout')
    endSession('expired')

    expect(getSession()).toBeNull()
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
    expect(sessionEndReason()).toBe('logout')
  })

  it('yeni giris onceki bitis nedenini temizler', () => {
    signIn()
    endSession('expired')

    signIn()

    expect(sessionEndReason()).toBeNull()
  })

  it('baska sekmedeki giris ve cikis bu sekmeye yansir', () => {
    const response = loginResponse()
    localStorage.setItem(STORAGE_KEY, JSON.stringify(response))
    window.dispatchEvent(new StorageEvent('storage', { key: STORAGE_KEY }))
    expect(getSession()).toEqual(response)

    localStorage.removeItem(STORAGE_KEY)
    window.dispatchEvent(new StorageEvent('storage', { key: STORAGE_KEY }))
    expect(getSession()).toBeNull()
    expect(sessionEndReason()).toBe('logout')
  })
})
