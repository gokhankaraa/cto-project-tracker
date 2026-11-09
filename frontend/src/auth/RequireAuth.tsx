import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'

import { sessionEndReason } from './session'
import { useSession } from './useSession'

/** Giris ekranina gecerken gidilmek istenen adres; giristen sonra oraya donulur. */
export interface LoginLocationState {
  from?: string
}

/**
 * Oturum gerektiren sayfalarin korumasi. Oturum yoksa (hic giris yapilmamis, cikis yapilmis ya da token
 * gecersiz oldugu icin API 401 donmus) /login'e yonlendirir ve acilmak istenen adresi hatirlar.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const session = useSession()
  const location = useLocation()

  if (!session) {
    // Bilerek cikis yapildiysa sayfa hatirlanmaz: sonraki giris baska bir kullanicinin olabilir ve o sayfayi
    // goremeyebilir (proje yoneticisi baskasinin projesinde 404 alir). Suresi dolan oturum kaldigi yere doner.
    const state: LoginLocationState =
      sessionEndReason() === 'logout' ? {} : { from: location.pathname + location.search }
    return <Navigate to="/login" replace state={state} />
  }
  return children
}
