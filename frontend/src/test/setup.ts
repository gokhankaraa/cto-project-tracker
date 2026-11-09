import '@testing-library/jest-dom/vitest'

import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'

import { resetSession } from '../auth/session'

// jsdom'da olmayan, Mantine'in kullandigi tarayici API'leri (Mantine test kurulumu onerisi).
// Node ortaminda calisan testlerde (or. api/client.test.ts) window yoktur, bu kisim atlanir.
if (typeof window !== 'undefined') {
  const { getComputedStyle } = window
  window.getComputedStyle = (element) => getComputedStyle(element)
  window.HTMLElement.prototype.scrollIntoView = () => {}

  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }),
  })

  class ResizeObserverStub {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
  window.ResizeObserver = ResizeObserverStub

  // Textarea autosize, yazi tipleri yuklenince yuksekligi yeniden olcmek icin document.fonts'u dinler.
  Object.defineProperty(document, 'fonts', {
    configurable: true,
    value: { addEventListener: () => {}, removeEventListener: () => {}, ready: Promise.resolve() },
  })

  // Bilesen testleri gercek backend'e istek atmaz: mockApi() cagirmayan testte her istek
  // ag hatasi olur (ust bardaki baglanti gostergesi "Bağlantı yok" gosterir).
  beforeEach(() => {
    vi.stubGlobal('fetch', () => Promise.reject(new TypeError('fetch bu testte mocklanmadi')))
  })
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  // Bir testte acilan oturum sonrakine tasinmaz.
  resetSession()
})
