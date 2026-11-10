import { readFileSync } from 'node:fs'

import { expect, firstCells, login, test } from './fixtures'

// Salt okur: CTO dashboard'u, filtreler (adreste tutulur), haftalik trend ve Excel ciktisi.

test.beforeEach(async ({ page }) => {
  await login(page, 'cto')
})

test('dashboard tum projeleri listeler; risk ve gecikme filtreleri', async ({ page }) => {
  expect(await firstCells(page)).toEqual(['Atlas IK', 'e-Belge', 'Nova POS', 'Orion Mobil'])

  await page.goto('/?riskLevel=YUKSEK')
  await expect(page.locator('table tbody tr')).toHaveCount(1)
  expect(await firstCells(page)).toEqual(['e-Belge'])

  // Atlas IK "Yolunda" ama hafta 31'de planlanan tarihi gecmis, bitmemis bir is kalemi var.
  await page.goto('/?delayed=true')
  await expect(page.locator('table tbody tr')).toHaveCount(1)
  expect(await firstCells(page)).toEqual(['Atlas IK'])
})

test('haftalik trend: grafikler cizilir; 31. haftanin sayilari tablo gorunumunde', async ({ page }) => {
  await expect(page.getByText('Son 12 hafta')).toBeVisible()
  await expect(page.locator('.recharts-wrapper')).toHaveCount(2)

  await page.goto('/?year=2026&weekNumber=31')
  await page.getByRole('button', { name: 'Tablo olarak göster' }).click()
  const row = page.getByRole('table', { name: 'Son 12 haftanın sayıları' }).locator('tr', { hasText: '2026 / 31. hafta' })
  // Raporlu, Yolunda, Riskli, Gecikmeli, Bloke, Rapor yok: Atlas IK yolunda, e-Belge riskli; Nova POS ve Orion raporsuz.
  const cells = (await row.locator('td').allInnerTexts()).slice(1, 7)
  expect(cells).toEqual(['2', '1', '1', '0', '0', '2'])
})

test("Excel'e aktar: gunun tarihli .xlsx dosyasi iner", async ({ page }) => {
  await page.goto('/?riskLevel=YUKSEK')
  const downloadPromise = page.waitForEvent('download')
  await page.getByRole('button', { name: "Excel'e aktar" }).click()
  const download = await downloadPromise

  expect(download.suggestedFilename()).toMatch(/^cto-ozeti-\d{4}-\d{2}-\d{2}\.xlsx$/)
  // .xlsx bir ZIP arsividir: ilk iki bayt "PK".
  const bytes = readFileSync(await download.path())
  expect(bytes.subarray(0, 2).toString()).toBe('PK')
  expect(bytes.length).toBeGreaterThan(1000)
})
