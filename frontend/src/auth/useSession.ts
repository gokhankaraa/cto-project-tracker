import { useSyncExternalStore } from 'react'

import { getSession, subscribeSession, type Session } from './session'

/** Gecerli oturum (yoksa null). Giris, cikis ve 401 ile oturum degisince bilesen yeniden render olur. */
export function useSession(): Session | null {
  return useSyncExternalStore(subscribeSession, getSession, () => null)
}
