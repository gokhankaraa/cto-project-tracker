// @vitest-environment node
// API istemcisi tarayici bilesi kullanmaz; Node'un kendi fetch/Response'u ile test edilir.
import { afterEach, describe, expect, it, vi } from 'vitest'

import { getSession, sessionEndReason } from '../auth/session'
import { signIn } from '../test/session'
import { ApiRequestError, NETWORK_ERROR, api, apiDownload, buildUrl, fileNameFrom } from './client'

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

describe('buildUrl, bos VITE_API_BASE_URL', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
    vi.resetModules()
  })

  it('istegi sayfanin geldigi adrese yapar (Docker: nginx /api yi backend e iletir)', async () => {
    vi.stubEnv('VITE_API_BASE_URL', '')
    vi.stubGlobal('location', { origin: 'http://localhost:3000' })
    vi.resetModules()
    const client = await import('./client')

    expect(client.buildUrl('/api/projects', { year: 2026 })).toBe('http://localhost:3000/api/projects?year=2026')
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

describe('apiDownload', () => {
  it("dosyayi ve backend'in onerdigi adi doner; her turu kabul eder, token'i gonderir", async () => {
    signIn()
    const fetchMock = mockFetch(
      new Response(new Uint8Array([80, 75, 3, 4]), {
        status: 200,
        headers: {
          'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          'Content-Disposition': `attachment; filename="cto-ozeti.xlsx"; filename*=UTF-8''cto-%C3%B6zeti-2026-11-10.xlsx`,
        },
      }),
    )

    const file = await apiDownload('/api/dashboard/export', { riskLevel: ['YUKSEK'], year: null }, 'yedek.xlsx')

    expect(file.fileName).toBe('cto-özeti-2026-11-10.xlsx')
    expect(file.blob.size).toBe(4)
    const [url, init] = fetchMock.mock.calls[0]
    expect(String(url)).toBe('http://localhost:8080/api/dashboard/export?riskLevel=YUKSEK')
    expect(init.headers).toMatchObject({ Accept: '*/*', Authorization: 'Bearer token-4' })
  })

  it('hata JSON istekleriyle ayni: ApiError govdesi ApiRequestError olur, 401 oturumu kapatir', async () => {
    mockFetch(json(400, { status: 400, code: 'VALIDATION_ERROR', message: 'Hatali.', fieldErrors: [{ field: 'weekNumber', message: 'Yil gerekli.' }] }))
    await expect(apiDownload('/api/dashboard/export', {}, 'x.xlsx')).rejects.toMatchObject({
      status: 400,
      code: 'VALIDATION_ERROR',
    })

    signIn()
    mockFetch(json(401, { status: 401, code: 'UNAUTHORIZED', message: 'Gecersiz token.' }))
    await expect(apiDownload('/api/dashboard/export', {}, 'x.xlsx')).rejects.toBeInstanceOf(ApiRequestError)
    expect(sessionEndReason()).toBe('expired')
  })

  it('baslikta ad yoksa yedek ad kullanilir', async () => {
    mockFetch(new Response('veri', { status: 200 }))
    expect((await apiDownload('/api/dashboard/export', {}, 'cto-ozeti.xlsx')).fileName).toBe('cto-ozeti.xlsx')
  })
})

describe('fileNameFrom', () => {
  it.each([
    [`attachment; filename="a.xlsx"; filename*=UTF-8''%C3%B6zet.xlsx`, 'özet.xlsx'],
    ['attachment; filename="a b.xlsx"', 'a b.xlsx'],
    ['attachment; filename=plain.xlsx', 'plain.xlsx'],
    [`attachment; filename="yedek.xlsx"; filename*=UTF-8''%E0%A4%A`, 'yedek.xlsx'],
    ['inline', null],
    [null, null],
  ])('%s -> %s', (header, expected) => {
    expect(fileNameFrom(header)).toBe(expected)
  })
})
