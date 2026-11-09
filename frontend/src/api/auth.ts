import { useMutation } from '@tanstack/react-query'

import { api } from './client'
import type { LoginResponse } from './types'

/** dto/LoginRequest.java */
export interface LoginRequest {
  email: string
  password: string
}

/** POST /api/auth/login. Basarisiz giris 401 INVALID_CREDENTIALS, bos alan 400 VALIDATION_ERROR doner. */
export function useLogin() {
  return useMutation({
    mutationFn: (request: LoginRequest) => api.post<LoginResponse>('/api/auth/login', request),
  })
}
