import { expect, login, test } from './fixtures'

// Salt okur: telefon genisliginde sayfalar yana tasmaz.

test.use({ viewport: { width: 390, height: 844 } })

for (const path of ['/', '/projects/1', '/tracking-items']) {
  test(`390 px: ${path} yatay kaydirma istemez`, async ({ page }) => {
    await login(page, 'cto')
    await page.goto(path)
    await page.waitForLoadState('networkidle')
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    )
    expect(overflow).toBeLessThanOrEqual(0)
  })
}
