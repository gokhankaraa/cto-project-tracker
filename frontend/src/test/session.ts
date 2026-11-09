import type { LoginResponse, User } from '../api/types'
import { saveSession } from '../auth/session'

export const ADMIN: User = { id: 4, fullName: 'Sistem Admin', role: 'ADMIN' }
export const CTO: User = { id: 3, fullName: 'Zeynep Demir', role: 'CTO' }
/** fixtures'taki Atlas IK ve Nova POS'un sorumlusu. */
export const PROJECT_MANAGER: User = { id: 1, fullName: 'Ayse Yilmaz', role: 'PROJECT_MANAGER' }

/** Backend'in giris yaniti; token'in suresi verilmezse bir saat sonra dolar. */
export function loginResponse(user: User = ADMIN, expiresInMs = 60 * 60 * 1000): LoginResponse {
  return { token: `token-${user.id}`, expiresAt: new Date(Date.now() + expiresInMs).toISOString(), user }
}

/** Testi oturum acilmis olarak baslatir (her testten sonra setup.ts sifirlar). */
export function signIn(user: User = ADMIN, expiresInMs?: number) {
  saveSession(loginResponse(user, expiresInMs))
}
