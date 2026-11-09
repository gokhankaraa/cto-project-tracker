import '@testing-library/jest-dom/vitest'

import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

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
}

afterEach(() => {
  cleanup()
})
