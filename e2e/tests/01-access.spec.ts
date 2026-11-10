import { expect, firstCells, login, test } from './fixtures'

// Salt okur: giris ve yetkiler.

test('hatali parola: giris yapilamaz, mesaj gosterilir', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('E-posta').fill('cto@example.com')
  await page.getByLabel('Parola').fill('yanlis-parola')
  await page.getByRole('button', { name: 'Giriş yap' }).click()

  await expect(page.getByRole('alert').filter({ hasText: 'Giriş yapılamadı' })).toBeVisible()
  await expect(page).toHaveURL(/\/login$/)
})

test('CTO okur ama yazamaz: duzenleme dugmesi yok, form adresi "Yetkiniz yok"', async ({ page }) => {
  await login(page, 'cto')
  await page.goto('/projects/1')
  await expect(page.getByRole('heading', { name: 'Haftalık raporlar' })).toBeVisible()
  for (const name of ['Düzenle', 'Sil', 'Yeni rapor', 'Kayıt ekle']) {
    await expect(page.getByRole('button', { name, exact: true })).toHaveCount(0)
    await expect(page.getByRole('link', { name, exact: true })).toHaveCount(0)
  }

  await page.goto('/projects/new')
  await expect(page.getByRole('heading', { name: 'Yetkiniz yok' })).toBeVisible()
})

test('proje yoneticisi yalnizca kendi projelerini gorur; baskasinin raporu "Kayıt bulunamadı"', async ({ page }) => {
  await login(page, 'mehmet')
  await page.goto('/projects')
  expect(await firstCells(page)).toEqual(['e-Belge', 'Orion Mobil'])

  // Rapor 4: Ayse'nin Atlas IK raporu. Olmayan kayitla ayni cevap (varligi sizmaz).
  await page.goto('/reports/4')
  await expect(page.getByRole('heading', { name: 'Kayıt bulunamadı' })).toBeVisible()
})
