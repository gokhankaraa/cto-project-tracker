import { useQuery } from '@tanstack/react-query'

import { api } from './client'
import type { HealthResponse } from './types'

/** Backend ve veritabani durumu (GET /api/health). */
export function useHealth() {
  return useQuery({
    queryKey: ['health'],
    queryFn: ({ signal }) => api.get<HealthResponse>('/api/health', undefined, signal),
    // Ust bardaki gosterge dakikada bir tazelenir; backend dusup kalkarsa kendiliginden duzelir.
    refetchInterval: 60_000,
  })
}
