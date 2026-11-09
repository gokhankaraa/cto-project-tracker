import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { api } from './client'
import type { Project, ProjectStatus, WeeklyReport } from './types'

/** POST / PUT /api/projects govdesi (dto/ProjectRequest.java). */
export interface ProjectRequest {
  name: string
  customer: string | null
  description: string | null
  status: ProjectStatus
  ownerId: number
}

/** Backend'deki @Size sinirlari; form ayni kurallari istekten once uygular. */
export const PROJECT_LIMITS = { name: 255, customer: 255, description: 1000 } as const

export const projectKeys = {
  all: ['projects'] as const,
  detail: (id: number) => ['projects', id] as const,
  reports: (id: number) => ['projects', id, 'reports'] as const,
}

export function useProjects() {
  return useQuery({
    queryKey: projectKeys.all,
    queryFn: ({ signal }) => api.get<Project[]>('/api/projects', undefined, signal),
  })
}

export function useProject(id: number) {
  return useQuery({
    queryKey: projectKeys.detail(id),
    queryFn: ({ signal }) => api.get<Project>(`/api/projects/${id}`, undefined, signal),
  })
}

/** Projenin haftalik raporlari; backend sira garanti etmez, burada en yeni hafta basa alinir. */
export function useProjectReports(id: number) {
  return useQuery({
    queryKey: projectKeys.reports(id),
    queryFn: ({ signal }) => api.get<WeeklyReport[]>(`/api/projects/${id}/reports`, undefined, signal),
    select: (reports) => [...reports].sort((a, b) => b.year - a.year || b.weekNumber - a.weekNumber),
  })
}

/**
 * Proje degisince proje sorgulari ve dashboard eskir (ad, durum, sorumlu orada da gorunur).
 * Ekranda olanlar hemen yeniden cekilir, digerleri bir sonraki acilista.
 */
function useInvalidateProjects() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: projectKeys.all }),
      queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
    ])
}

export function useCreateProject() {
  const invalidate = useInvalidateProjects()
  return useMutation({
    mutationFn: (request: ProjectRequest) => api.post<Project>('/api/projects', request),
    onSuccess: invalidate,
  })
}

export function useUpdateProject(id: number) {
  const invalidate = useInvalidateProjects()
  return useMutation({
    mutationFn: (request: ProjectRequest) => api.put<Project>(`/api/projects/${id}`, request),
    onSuccess: invalidate,
  })
}

/** Raporu olan proje silinemez: backend 409 RESOURCE_IN_USE doner, mesaji ekranda gosterilir. */
export function useDeleteProject(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.delete(`/api/projects/${id}`),
    onSuccess: () => {
      // Yalnizca liste (exact) ve dashboard yenilenir. Silinen projenin detay ve rapor sorgularina
      // dokunulmaz: sayfa listeye gecene kadar (lazy parca yuklenirken) ekranda kalir; o sirada
      // yenilenen ya da onbellekten cikarilan sorgu tekrar istek atar ve 404 alir. Dokunulmayan
      // sorgu sayfa kapaninca gcTime sonunda kendiliginden silinir.
      void queryClient.invalidateQueries({ queryKey: projectKeys.all, exact: true })
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}
