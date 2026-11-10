import { choose, expect, login, test } from './fixtures'

// Yazar: Ayse Atlas IK'nin riskini pencereden degistirir; degisiklik gecmisi ve gorunurluk. Temiz veritabani ister
// (03 Nova POS'a rapor ve is kalemi ekledi; burada onlar da gecmiste gorunur).

const RISK = "Entegrasyon API'si gec teslim edilebilir"

const history = (page: import('@playwright/test').Page) =>
  page.getByRole('list', { name: 'Değişiklikler' }).getByRole('listitem')

test('risk seviyesi degisikligi gecmise yazan kisiyle, sayfa yenilenmeden duser', async ({ page }) => {
  await login(page, 'ayse')
  await page.goto('/projects/1')
  await expect(page.getByText(/henüz değişiklik kaydı yok/)).toBeVisible()

  await page.getByRole('button', { name: `${RISK} düzenle` }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByRole('combobox', { name: 'Risk seviyesi' }).click()
  await page.getByRole('option', { name: 'Orta', exact: true }).click()
  await dialog.getByRole('button', { name: 'Kaydet' }).click()
  await expect(dialog).toBeHidden()

  await expect(history(page)).toHaveCount(1)
  const entry = history(page).first()
  for (const text of ['Güncellendi', `Risk / aksiyon: ${RISK}`, 'Ayse Yilmaz', 'Risk seviyesi:', 'Yüksek', 'Orta']) {
    await expect(entry).toContainText(text)
  }
})

test('baska proje yoneticisi gecmisi goremez', async ({ page }) => {
  await login(page, 'mehmet')
  await page.goto('/projects/1')
  await expect(page.getByRole('heading', { name: 'Kayıt bulunamadı' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Değişiklik geçmişi' })).toHaveCount(0)
})

test('CTO tum gecmisi okur ve ture gore suzer', async ({ page }) => {
  await login(page, 'cto')

  // 03'teki rapor ve is kalemi: en yeni ustte.
  await page.goto('/projects/3')
  await expect(history(page)).toHaveCount(2)
  await expect(history(page).nth(0)).toContainText('İş kalemi: Kickoff')
  await expect(history(page).nth(1)).toContainText('Haftalık rapor:')

  await page.goto('/projects/1')
  await expect(history(page)).toHaveCount(1)
  await choose(page, 'Kayıt türü', 'Haftalık rapor')
  await expect(page.getByText('Bu projede "Haftalık rapor" türünde değişiklik yok.')).toBeVisible()
  await choose(page, 'Kayıt türü', 'Risk / aksiyon')
  await expect(history(page)).toHaveCount(1)
})
