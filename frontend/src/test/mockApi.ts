import { vi } from 'vitest'

/**
 * fetch'i sahte bir backend ile degistirir: yol (pathname) -> JSON govde. Tanimsiz yollar 404 doner.
 * Donen mock ile hangi adreslerin hangi parametrelerle istendigi kontrol edilir (bkz. {@link requestedUrls}).
 */
export function mockApi(routes: Record<string, unknown>) {
  const fetchMock = vi.fn(async (input: string | URL | Request) => {
    const url = new URL(input instanceof Request ? input.url : input)
    const json = (status: number, body: unknown) =>
      new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

    if (url.pathname in routes) return json(200, routes[url.pathname])
    return json(404, { status: 404, code: 'RESOURCE_NOT_FOUND', message: `Mock yok: ${url.pathname}` })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** Mock'a gelen isteklerden verilen yola ait olanlarin adresleri (sirasiyla). */
export function requestedUrls(fetchMock: ReturnType<typeof mockApi>, pathname: string): URL[] {
  return fetchMock.mock.calls.map(([input]) => new URL(String(input))).filter((url) => url.pathname === pathname)
}
