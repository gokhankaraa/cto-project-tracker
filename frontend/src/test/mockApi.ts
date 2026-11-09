import { vi } from 'vitest'

/** Mock'a gelen bir istek. body, JSON govde varsa cozulmus halidir. */
export interface MockRequest {
  method: string
  url: URL
  body: unknown
}

/** Bir isleyicinin yaniti; status verilmezse 200. */
export interface MockReply {
  status?: number
  body?: unknown
}

// unknown kullanilmaz: unknown | Fonksiyon birlesimi unknown'a iner ve isleyicinin parametresi tip almaz.
type Route = ((request: MockRequest) => MockReply) | object | string | number | boolean | null

/**
 * fetch'i sahte bir backend ile degistirir. Anahtar "METOT /yol" ya da yalnizca "/yol" (GET) olur;
 * deger ya dogrudan 200 ile donecek JSON govde ya da istegi alip {status, body} donduren bir fonksiyondur:
 *
 *   mockApi({
 *     '/api/projects': PROJECTS,
 *     'POST /api/projects': ({ body }) => ({ status: 201, body: { id: 9, ...body } }),
 *   })
 *
 * Tanimsiz yollar 404 doner. Gelen istekler {@link requests} ile incelenir.
 */
export function mockApi(routes: Record<string, Route>) {
  const fetchMock = vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
    const url = new URL(input instanceof Request ? input.url : input)
    const method = (init?.method ?? 'GET').toUpperCase()
    const body = typeof init?.body === 'string' ? JSON.parse(init.body) : undefined

    const key = `${method} ${url.pathname}`
    const route = key in routes ? routes[key] : method === 'GET' ? routes[url.pathname] : undefined
    if (route === undefined) {
      return json(404, { status: 404, code: 'RESOURCE_NOT_FOUND', message: `Mock yok: ${key}` })
    }
    if (typeof route === 'function') {
      const reply = (route as (request: MockRequest) => MockReply)({ method, url, body })
      return json(reply.status ?? 200, reply.body)
    }
    return json(200, route)
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function json(status: number, body: unknown) {
  if (status === 204 || body === undefined) return new Response(null, { status })
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

/** Mock'a gelen tum istekler (sirasiyla). */
export function requests(fetchMock: ReturnType<typeof mockApi>): MockRequest[] {
  return fetchMock.mock.calls.map(([input, init]) => ({
    method: (init?.method ?? 'GET').toUpperCase(),
    url: new URL(String(input)),
    body: typeof init?.body === 'string' ? JSON.parse(init.body) : undefined,
  }))
}

/** Verilen yola yapilan GET isteklerinin adresleri (sirasiyla). */
export function requestedUrls(fetchMock: ReturnType<typeof mockApi>, pathname: string): URL[] {
  return requests(fetchMock)
    .filter((request) => request.method === 'GET' && request.url.pathname === pathname)
    .map((request) => request.url)
}

/** Backend'in dogrulama hatasi govdesi (400 VALIDATION_ERROR). */
export function validationError(fieldErrors: { field: string; message: string }[]): MockReply {
  return {
    status: 400,
    body: { status: 400, code: 'VALIDATION_ERROR', message: 'Istek dogrulanamadi.', path: '', fieldErrors },
  }
}
