// @vitest-environment node
// API istemcisi tarayici bilesi kullanmaz; Node'un kendi fetch/Response'u ile test edilir.
import { afterEach, describe, expect, it, vi } from 'vitest'

import { getSession, sessionEndReason } from '../auth/session'
import { signIn } from '../test/session'
import { ApiRequestError, NETWORK_ERROR, api, buildUrl } from './client'

function mockFetch(response: Response | Error) {
  const fn = response instanceof Error ? vi.fn().mockRejectedValue(response) : vi.fn().mockResolvedValue(response)
  vi.stubGlobal('fetch', fn)
  return fn
}

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('buildUrl', () => {
  it('bos parametreleri gondermez, cok degerli parametreleri virgulle birlestirir', () => {
    const url = new URL(
      buildUrl('/api/reports', {
        year: 2026,
        riskLevel: ['ORTA', 'YUKSEK'],
        ownerId: undefined,
        projectId: null,
        assignee: '',
        status: [],
      }),
    )

    expect(url.pathname).toBe('/api/reports')
    expect([...url.searchParams.keys()]).toEqual(['year', 'riskLevel'])
    expect(url.searchParams.get('riskLevel')).toBe('ORTA,YUKSEK')
  })
})

describe('apiRequest', () => {
  it('basarili yaniti JSON olarak doner', async () => {
    const fetchMock = mockFetch(json(200, { status: 'UP' }))

    await expect(api.get('/api/health')).resolves.toEqual({ status: 'UP' })
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/health',
      expect.objectContaining({ method: 'GET' }),
    )
  })

  it("backend'in ApiError govdesini kod, mesaj ve alan hatalariyla tasir", async () => {
    mockFetch(
      json(400, {
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Gonderilen alanlar dogrulama kurallarina uymuyor.',
        path: '/api/reports',
        fieldErrors: [
          { field: 'weekNumber', message: 'Hafta (weekNumber) yil (year) ile birlikte verilmelidir.' },
          { field: 'weekNumber', message: 'ikinci mesaj' },
        ],
      }),
    )

    const error = await api.get('/api/reports', { weekNumber: 29 }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiRequestError)
    const apiError = error as ApiRequestError
    expect(apiError.status).toBe(400)
    expect(apiError.code).toBe('VALIDATION_ERROR')
    expect(apiError.fieldErrorMap()).toEqual({
      weekNumber: 'Hafta (weekNumber) yil (year) ile birlikte verilmelidir.',
    })
  })

  it('JSON olmayan hata yanitini genel bir hataya cevirir', async () => {
    mockFetch(new Response('<html>Bad Gateway</html>', { status: 502 }))

    await expect(api.get('/api/health')).rejects.toMatchObject({ status: 502, code: 'UNKNOWN_ERROR' })
  })

  it('sunucuya ulasilamazsa NETWORK_ERROR verir', async () => {
    mockFetch(new TypeError('Failed to fetch'))

    await expect(api.get('/api/health')).rejects.toMatchObject({ status: 0, code: NETWORK_ERROR })
  })

  it('govdeli isteklerde JSON gonderir, 204 yanitinda bos doner', async () => {
    const fetchMock = mockFetch(new Response(null, { status: 204 }))

    await expect(api.put('/api/reports/1', { weekNumber: 30 })).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/reports/1',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({ weekNumber: 30 }),
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      }),
    )
  })
})

describe('oturum', () => {
  function sentHeaders(fetchMock: ReturnType<typeof mockFetch>): Record<string, string> {
    return fetchMock.mock.calls[0][1].headers
  }

  it("oturum varken token'i Authorization basliginda gonderir, yokken gondermez", async () => {
    const withoutSession = mockFetch(json(200, {}))
    await api.get('/api/health')
    expect(sentHeaders(withoutSession)).not.toHaveProperty('Authorization')

    signIn()
    const withSession = mockFetch(json(200, {}))
    await api.get('/api/projects')
    expect(sentHeaders(withSession).Authorization).toBe('Bearer token-4')
  })

  it("token'la gidip 401 donen istek oturumu 'suresi doldu' diye kapatir", async () => {
    signIn()
    mockFetch(json(401, { status: 401, code: 'UNAUTHORIZED', message: 'Gecersiz token.' }))

    await expect(api.get('/api/projects')).rejects.toMatchObject({ status: 401, code: 'UNAUTHORIZED' })
    expect(getSession()).toBeNull()
    expect(sessionEndReason()).toBe('expired')
  })

  it('403 oturumu kapatmaz', async () => {
    signIn()
    mockFetch(json(403, { status: 403, code: 'FORBIDDEN', message: 'Yetki yok.' }))

    await expect(api.delete('/api/projects/1')).rejects.toMatchObject({ status: 403 })
    expect(getSession()).not.toBeNull()
  })
})
