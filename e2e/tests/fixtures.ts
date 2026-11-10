import { test as base, expect, type Page } from '@playwright/test'

/** Demo kullanicilari (backend DataSeeder); hepsinin parolasi ayni. */
export const USERS = {
  admin: 'admin@example.com',
  cto: 'cto@example.com',
  ayse: 'ayse@example.com',
  mehmet: 'mehmet@example.com',
} as const
export const DEMO_PASSWORD = 'Demo1234!'

/**
 * Beklenen hata yanitlari (yetkisiz 401/403, bulunamadi 404, cakisma 409) tarayici konsoluna "Failed to load
 * resource" olarak duser; bunlar senaryonun parcasidir. Geri kalan her konsol hatasi ve yakalanmamis istisna testi
 * dusurur.
 */
const EXPECTED_STATUS = /status of (401|403|404|409)/

export const test = base.extend<{ consoleErrors: string[] }>({
  consoleErrors: [
    async ({ page }, use) => {
      const errors: string[] = []
      page.on('console', (message) => {
        if (message.type() === 'error' && !EXPECTED_STATUS.test(message.text())) errors.push(message.text())
      })
      page.on('pageerror', (error) => errors.push(`pageerror: ${error.message}`))
      await use(errors)
      expect(errors, 'beklenmeyen konsol hatalari').toEqual([])
    },
    { auto: true },
  ],
})

export { expect }

export async function login(page: Page, user: keyof typeof USERS, password = DEMO_PASSWORD) {
  await page.goto('/login')
  await page.getByLabel('E-posta').fill(USERS[user])
  await page.getByLabel('Parola').fill(password)
  await page.getByRole('button', { name: 'Giriş yap' }).click()
  await expect(page.getByRole('button', { name: 'Çıkış' })).toBeVisible()
}

/** Bir tablonun satirlarinin ilk hucreleri (proje adi gibi). */
export async function firstCells(page: Page) {
  const rows = page.locator('table tbody tr')
  await expect(rows.first()).toBeVisible()
  return (await rows.locator('td:first-child').allInnerTexts()).map((text) => text.split('\n')[0].trim())
}

/** Mantine Select: etiketi verilen kutuyu acar ve secenegi secer. */
export async function choose(page: Page, label: string | RegExp, option: string) {
  await page.getByRole('combobox', { name: label }).first().click()
  await page.getByRole('option', { name: option, exact: true }).click()
}
