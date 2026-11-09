import { useQuery } from '@tanstack/react-query'

import { api } from './client'
import type { Role, User } from './types'

/** Kullanici listesi (ada gore sirali). Sorumlu secimi icin role=PROJECT_MANAGER verilir. */
export function useUsers(role?: Role) {
  return useQuery({
    queryKey: ['users', role ?? 'all'],
    queryFn: ({ signal }) => api.get<User[]>('/api/users', { role }, signal),
    // Kullanici listesi nadiren degisir; sayfalar arasi geziste yeniden istenmesin.
    staleTime: 5 * 60 * 1000,
  })
}
