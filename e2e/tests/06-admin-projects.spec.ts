import { choose, expect, firstCells, login, test } from './fixtures'

// Yazar: Admin proje yonetimi. Olusturdugu projeyi sonunda siler; diger testlerin verisine dokunmaz.

const NAME = 'E2E Proje'

test('admin proje olusturur, duzenler; sorumlu onu gorur; raporlu proje silinemez, yenisi silinir', async ({ page }) => {
  await login(page, 'admin')

  // Olustur: sorumlu Mehmet.
  await page.goto('/projects/new')
  await page.getByLabel('Proje adı').fill(NAME)
  await page.getByLabel('Müşteri').fill('Deneme A.Ş.')
  await choose(page, /^Durum/, 'Planlandı')
  await choose(page, /^Sorumlu proje yöneticisi/, 'Mehmet Kaya')
  await page.getByRole('button', { name: 'Oluştur' }).click()
  await expect(page.getByRole('heading', { name: NAME })).toBeVisible()
  await expect(page).toHaveURL(/\/projects\/\d+$/)
  const projectUrl = new URL(page.url()).pathname

  // Duzenle: durum degisir, detayda gorunur.
  await page.getByRole('link', { name: 'Düzenle' }).click()
  await choose(page, /^Durum/, 'Devam ediyor')
  await page.getByRole('button', { name: 'Kaydet' }).click()
  await expect(page).toHaveURL(new RegExp(`${projectUrl}$`))
  await expect(page.getByText('Devam ediyor').first()).toBeVisible()

  // Raporu olan proje silinemez: dugme bastan pasif.
  await page.goto('/projects/1')
  await expect(page.getByRole('button', { name: 'Sil', exact: true })).toBeDisabled()

  // Sorumlu proje yoneticisi yeni projeyi kendi listesinde gorur.
  await page.getByRole('button', { name: 'Çıkış' }).click()
  await login(page, 'mehmet')
  await page.goto('/projects')
  expect(await firstCells(page)).toContain(NAME)
  await page.getByRole('button', { name: 'Çıkış' }).click()

  // Raporsuz yeni proje onay penceresiyle silinir ve listeden kalkar.
  await login(page, 'admin')
  await page.goto(projectUrl)
  await page.getByRole('button', { name: 'Sil', exact: true }).click()
  await page.getByRole('dialog', { name: 'Projeyi sil' }).getByRole('button', { name: 'Sil' }).click()
  await expect(page).toHaveURL(/\/projects$/)
  await expect(page.getByText(`"${NAME}" silindi.`)).toBeVisible()
  expect(await firstCells(page)).not.toContain(NAME)
})
