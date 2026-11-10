import { defineConfig, devices } from '@playwright/test'

/**
 * Tarayicida uctan uca testler. Calisan bir demo kurulumuna karsi kosar (docker compose up -d --build; temiz
 * veritabani: docker compose down -v). Testler ayni demo verisini degistirdigi icin tek worker ile ve dosya
 * sirasiyla calisir: 01-02 yalnizca okur, 03-04 yazar, 05 yine okur.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    locale: 'tr-TR',
    // Backend'in "bugun"u Turkiye saatidir; tarayici da ayni gunu gorsun.
    timezoneId: 'Europe/Istanbul',
    viewport: { width: 1280, height: 900 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    acceptDownloads: true,
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 900 } } }],
})
