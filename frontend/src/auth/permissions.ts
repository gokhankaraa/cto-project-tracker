import type { Role } from '../api/types'
import { useSession } from './useSession'

/**
 * Yazma islemlerinin rol kurallari; backend'deki security/Permissions ile ayni tablo (README "Permissions"):
 *
 *                                         Admin   CTO   Proje yoneticisi
 *   Proje olustur / duzenle / sil           ✓      ✗     ✗
 *   Rapor ve is kalemi yaz / sil            ✓      ✗     ✓ (kendi projelerinde)
 *
 * Sahiplik ayrica kontrol edilmez: proje yoneticisi baskasinin projesini, raporunu ya da is kalemini
 * zaten acamaz (backend 404 doner), ekranda gordugu her kayit kendisinindir. Arayuz yalnizca
 * yapilamayacak islemi gostermez; asil kontrol backend'dedir (403 / 404).
 */
export const PERMISSIONS = {
  manageProjects: ['ADMIN'],
  writeReports: ['ADMIN', 'PROJECT_MANAGER'],
} as const satisfies Record<string, readonly Role[]>

export type Permission = keyof typeof PERMISSIONS

export function hasPermission(role: Role | undefined, permission: Permission): boolean {
  return role !== undefined && (PERMISSIONS[permission] as readonly Role[]).includes(role)
}

/** Oturumdaki kullanicinin izinleri; oturum yoksa hicbiri. */
export function usePermissions(): Record<Permission, boolean> {
  const role = useSession()?.user.role
  return {
    manageProjects: hasPermission(role, 'manageProjects'),
    writeReports: hasPermission(role, 'writeReports'),
  }
}
