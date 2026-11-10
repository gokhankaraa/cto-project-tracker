import { choose, expect, login, test } from './fixtures'

// Yazar: Ayse, raporsuz Nova POS'a gecen haftanin raporunu ve bir is kalemi ekler. Temiz veritabani ister.

test('rapor ve is kalemi eklenir; ayni hafta ikinci rapor 409; dashboard guncellenir', async ({ page }) => {
  await login(page, 'ayse')

  await page.goto('/projects/3')
  await page.getByRole('link', { name: 'Yeni rapor' }).click()
  await expect(page.getByRole('heading', { name: 'Yeni haftalık rapor' })).toBeVisible()
  // Form gecen haftadan ve bugunun tarihinden baslar; durum ve risk secilir.
  await choose(page, /^Genel durum/, 'Yolunda')
  await choose(page, /^Risk/, 'Düşük')
  await page.getByRole('button', { name: 'Oluştur' }).click()
  await expect(page).toHaveURL(/\/reports\/\d+$/)

  await page.getByRole('button', { name: 'İş kalemi ekle' }).click()
  const dialog = page.getByRole('dialog', { name: 'Yeni iş kalemi' })
  await dialog.getByLabel('Başlık').fill('Kickoff')
  await dialog.getByLabel('Sorumlu').fill('Ayşe')
  await dialog.getByRole('combobox', { name: /^Durum/ }).click()
  await page.getByRole('option', { name: 'Devam ediyor', exact: true }).click()
  await dialog.getByRole('button', { name: 'Ekle' }).click()
  await expect(page.locator('table tbody tr', { hasText: 'Kickoff' })).toBeVisible()

  // Ayni hafta: backend 409, mesaj formda.
  await page.goto('/projects/3/reports/new')
  await choose(page, /^Genel durum/, 'Yolunda')
  await choose(page, /^Risk/, 'Düşük')
  await page.getByRole('button', { name: 'Oluştur' }).click()
  await expect(page.getByText(/Bu proje için .* haftasına ait rapor zaten mevcut\./)).toBeVisible()

  await page.getByRole('link', { name: 'Dashboard' }).click()
  await expect(page.locator('table tbody tr', { hasText: 'Nova POS' })).toContainText(/Yolunda/i)
})
