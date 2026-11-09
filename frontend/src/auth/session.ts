import type { LoginResponse } from '../api/types'

/** Giris yapan kullanicinin oturumu: backend'in giris yaniti ({@link LoginResponse}) oldugu gibi saklanir. */
export type Session = LoginResponse

/** Oturumun neden bittigi; giris ekrani buna gore bir not gosterir. */
export type SessionEndReason = 'logout' | 'expired'

const STORAGE_KEY = 'ctotracker.session'

/**
 * Oturum deposu. Kaynak bu moduldeki degiskendir; localStorage yalnizca sayfa yenilenince ve baska
 * sekmede giris / cikis yapilinca okunur. React bilesenleri {@link useSession} ile, API istemcisi
 * dogrudan {@link getSession} ile okur.
 */
let current: Session | null = load()
let endReason: SessionEndReason | null = null
const listeners = new Set<() => void>()

function storage(): Storage | null {
  try {
    return typeof localStorage === 'undefined' ? null : localStorage
  } catch {
    // Tarayici depolamayi engellemis olabilir (gizli pencere, site verisi kapali): oturum sekmeyle sinirli kalir.
    return null
  }
}

function isLive(session: Session): boolean {
  return Date.parse(session.expiresAt) > Date.now()
}

function load(): Session | null {
  try {
    const raw = storage()?.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<Session>
    if (typeof parsed?.token === 'string' && typeof parsed.expiresAt === 'string' && parsed.user) {
      return parsed as Session
    }
  } catch {
    // Bozuk kayit: yok sayilir, kullanici yeniden giris yapar.
  }
  return null
}

function persist(session: Session | null) {
  try {
    if (session) storage()?.setItem(STORAGE_KEY, JSON.stringify(session))
    else storage()?.removeItem(STORAGE_KEY)
  } catch {
    // Kota dolu ya da depolama kapali: oturum bellekte devam eder.
  }
}

function notify() {
  for (const listener of listeners) listener()
}

/**
 * Gecerli oturum; yoksa ya da token'in suresi dolduysa null. Ayni oturum icin hep ayni nesneyi doner
 * (useSyncExternalStore gereksiz yeniden render yapmasin).
 */
export function getSession(): Session | null {
  return current && isLive(current) ? current : null
}

/** Son oturumun neden bittigi; token'i suresi dolmus bir oturum da "expired" sayilir. Giriste sifirlanir. */
export function sessionEndReason(): SessionEndReason | null {
  if (endReason) return endReason
  return current && !isLive(current) ? 'expired' : null
}

export function saveSession(session: Session) {
  current = session
  endReason = null
  persist(session)
  notify()
}

/** Oturumu kapatir. Oturum zaten yoksa bir sey yapmaz (ilk nedeni korur: cikis, ardindan gelen 401 ile "suresi doldu" olmaz). */
export function endSession(reason: SessionEndReason) {
  if (!current) return
  current = null
  endReason = reason
  persist(null)
  notify()
}

export function subscribeSession(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/** Testler arasinda oturumu bildirim gondermeden sifirlar. */
export function resetSession() {
  current = null
  endReason = null
  persist(null)
}

// Baska sekmede giris / cikis yapilinca bu sekme de ayni oturuma gecer.
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    if (event.key !== STORAGE_KEY) return
    current = load()
    endReason = current ? null : 'logout'
    notify()
  })
}
