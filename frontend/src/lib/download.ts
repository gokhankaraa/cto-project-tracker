/**
 * Bellekteki dosyayi tarayiciya kaydettirir: gecici bir adres olusturulur, gorunmez bir baglantiyla indirilir ve
 * adres serbest birakilir (yoksa dosya sayfa acik kaldikca bellekte kalir). Serbest birakma bir sonraki tura
 * ertelenir: hemen yapilirsa bazi tarayicilar indirmeyi baslamadan iptal eder.
 */
export function saveFile(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.append(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 0)
}
