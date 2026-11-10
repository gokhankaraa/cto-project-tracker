import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    // Bilesen testleri tarayici benzeri ortamda (jsdom) calisir.
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // Mantine'in CSS dosyalari testte gerekmez.
    css: false,
    // Form ve liste testleri tek basina 1-2,5 sn surer (jsdom'da Mantine render'i); dosyalar paralel calisirken makine
    // yukluyse 5 sn'lik varsayilani asabiliyordu. Sinir takilan testi yakalamak icindir: 15 sn bunu yine yakalar.
    testTimeout: 15_000,
  },
})
