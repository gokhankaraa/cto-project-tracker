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
  },
})
