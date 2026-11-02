# T13 — Filtreleme ve Durum Zenginleştirme: Kısa Analiz

**Staj yönü:** Backend (Java + Spring Boot)
**Task:** T13 (21–24. gün), ortak görev satırı
**Hazırlayan:** Gökhan Kara
**Tarih:** 22 Ekim 2026

> **Ortak görev (yönetmelik):** Proje, hafta, durum, risk, sorumlu ve gecikme filtrelerini ekleyin;
> boş/yükleniyor/hata durumlarını iyileştirin.

Bu not, filtrelerin *ne anlama geldiğini* ve *ne zaman doğru çalışmış sayılacağını* tanımlar.
Parametre adları, sayfalama ve sıralama gibi teknik ayrıntılar backend satırındaki filtre
sözleşmesinde (21. gün) netleşecek.

---

## 1. Amaç

MVP'de CTO bütün portföyü tek ekranda görebiliyor, ama listeyi daraltamıyor. Proje sayısı
arttıkça CTO'nun asıl sorduğu sorular şunlar oluyor:

- "Hangi projeler riskli ya da gecikmede?"
- "Ayşe'nin projeleri ne durumda?"
- "Geçen hafta portföy nasıl görünüyordu?"

T13 bu soruların her birine tek bir istekle cevap verebilmeyi hedefliyor.

## 2. Mevcut durum

| Konu | Bugün | Eksik |
|---|---|---|
| CTO dashboard | Sadece `riskLevel` filtresi var | Proje, hafta, durum, sorumlu, gecikme |
| Rapor listesi | Yalnızca tek projenin altından (`/api/projects/{id}/reports`) | Projeler arası filtreli rapor geçmişi |
| İş kalemi listesi | Yalnızca tek raporun altından | Sorumluya ve gecikmeye göre arama |
| Sayfalama / sıralama | Yok | Rapor ve iş kalemi listelerinde gerekli |
| Örnek veri | İş kalemlerinde planlanan tarih yok | Gecikme filtresini gösterecek demo verisi |

## 3. Kapsam: altı filtre boyutu ve anlamları

| Filtre | Anlamı | Nerede uygulanır |
|---|---|---|
| **Proje** | Belirli bir proje | Dashboard, rapor listesi, iş kalemi listesi |
| **Hafta** | Dashboard'da "X. hafta itibarıyla portföy": her proje için hafta numarası ≤ X olan en son rapor esas alınır. İş kalemi listesi de aynı referansı kullanır (filtre sözleşmesi, S-05). Rapor listesinde hafta aralığı veya rapor tarihi aralığı. | Dashboard, rapor listesi, iş kalemi listesi |
| **Durum** | Sistemde üç ayrı durum kavramı var, karıştırılmamalı: projenin durumu (`ProjectStatus`), raporun genel durumu (`OverallStatus`), iş kaleminin durumu (`WorkItemStatus`) | Proje durumu ve rapor durumu: dashboard ve rapor listesi. İş kalemi durumu: iş kalemi listesi |
| **Risk** | Raporun risk seviyesi (dashboard'da en son veya seçilen haftanın raporu) | Dashboard, rapor listesi |
| **Sorumlu** | Dashboard ve raporlarda projenin sorumlu proje yöneticisi (`owner`). İş kalemlerinde `assignee` alanı (serbest metin). | Hepsi |
| **Gecikme** | Bkz. bölüm 5, K-T13-01 | Dashboard, iş kalemi listesi |

## 4. Kapsam dışı (T13'te yapılmayacaklar)

- **Rol bazlı kısıt:** proje yöneticisinin yalnızca kendi projelerini görmesi T14'ün işi. T13'te
  filtreler bu kısıtın sonradan tek bir ek koşulla eklenebileceği şekilde kurulur.
- Rapor metinlerinde (yapılanlar, riskler vb.) serbest metin araması.
- Kayıtlı filtreler ve filtre şablonları.
- Takvim uyumu (`scheduleStatus`): bu alan MVP'de hiç eklenmedi (ön analiz, karar 4).
- Filtrenin URL'de saklanması ve yükleniyor göstergesi: frontend satırının konusu.

## 5. Kararlar

| No | Karar | Gerekçe |
|---|---|---|
| K-T13-01 | **Gecikme tanımı.** Bir iş kalemi gecikmiştir: durumu `GECIKTI` ise **veya** planlanan tarihi bağlı olduğu raporun tarihinden önceyse ve durumu `TAMAMLANDI` değilse. Bir proje gecikmededir: değerlendirilen raporunun genel durumu `GECIKMELI` ise **veya** o raporda en az bir gecikmiş iş kalemi varsa. | Rapor haftalık bir anlık görüntü (snapshot) olduğu için referans tarih "bugün" değil raporun kendi tarihi. Böylece sonuç saate bağlı olmaz ve test edilebilir kalır, geçmiş haftalar da kendi haftalarındaki duruma göre değerlendirilir. |
| K-T13-02 | Farklı filtreler **VE** ile birleşir. Aynı parametredeki birden çok değer **VEYA** ile birleşir (ör. `ORTA,YUKSEK` = orta veya yüksek riskli). | CTO'nun en sık sorusu "orta ve yüksek riskliler" gibi bir aralık. |
| K-T13-03 | Filtre verilmezse mevcut davranış aynen korunur. Mevcut `?riskLevel=YUKSEK` kullanımı da çalışmaya devam eder. | Geriye uyumluluk; MVP testleri ve README'deki demo senaryosu bozulmaz. |
| K-T13-04 | Rapora dayalı filtreler (risk, rapor durumu, gecikme) verildiğinde raporu olmayan projeler listelenmez. Projeye dayalı filtrelerde (proje, proje durumu, sorumlu) bu projeler `hasReport=false` ile listelenir. | Bugünkü risk filtresi de bu şekilde çalışıyor; kural tüm filtrelere genellenir. |
| K-T13-05 | Hafta filtresi bir filtreden çok bir **referans noktası**: X. haftaya kadar hiç raporu olmayan projeler de `hasReport=false` ile görünür. | CTO'nun "o hafta kim rapor girmemişti" sorusunu da görebilmesi için. |
| K-T13-06 | Dashboard sayaçları (toplam proje, yüksek riskli, raporsuz, canlı task) filtrelenmiş kümeye göre hesaplanır. | Ekrandaki liste ile sayaçlar her zaman tutarlı kalır. |
| K-T13-07 | Filtre sonucu boşsa yanıt `200` ve boş liste olur, `404` dönmez. Var olmayan bir proje id'siyle filtrelemek de boş liste döndürür. | Filtre bir kayıt arama işlemi değildir; "sonuç yok" bir hata değildir. |
| K-T13-08 | Sayfalama rapor ve iş kalemi listelerinde olur, dashboard'da olmaz. | Rapor ve iş kalemi sayısı zamanla büyür. Proje sayısı küçük kalır ve sayaçlar tüm küme üzerinden hesaplanmalıdır. |
| K-T13-09 | İş kalemi sorumlusu araması büyük/küçük harfe duyarsız ve "içerir" mantığıyla yapılır (`assignee=ayse` → "Ayse Yilmaz"). | `assignee` serbest metin; tam eşleşme kullanışsız olur. |

## 6. Kullanıcı hikâyeleri

- **CTO** olarak portföyü risk seviyesine ve duruma göre süzmek istiyorum; böylece müdahale
  gereken projeleri hemen görürüm.
- **CTO** olarak yalnızca geciken projeleri görmek istiyorum; böylece gecikmeleri geç fark etmem.
- **CTO** olarak portföyü geçmiş bir haftaya göre görmek istiyorum; böylece haftalar arası
  değişimi karşılaştırabilirim.
- **CTO** olarak bir proje yöneticisinin projelerini ayrıca görmek istiyorum; böylece
  sorumluluk bazında takip yapabilirim.
- **Proje yöneticisi** olarak bir projenin rapor geçmişini tarih aralığıyla listelemek
  istiyorum; böylece önceki kararlara ve planlara dönebilirim.
- **Proje yöneticisi / ekip lideri** olarak bir kişiye atanmış ve geciken iş kalemlerini görmek
  istiyorum; böylece takip edilmesi gereken işleri bulurum.

## 7. Boş, yükleniyor ve hata durumları (backend'in payı)

| Durum | Beklenen davranış |
|---|---|
| Sonuç yok | `200`, boş liste, sayaçlar `0` (K-T13-07) |
| Tanımsız enum değeri, sayı yerine metin (`?riskLevel=COK_YUKSEK`, `?projectId=abc`) | `400 INVALID_REQUEST`, mesajda parametre adı geçer (T07'den beri mevcut) |
| Ters aralık (`weekFrom > weekTo`, `dateFrom > dateTo`) | `400 VALIDATION_ERROR`, ilgili alan `fieldErrors` içinde |
| Aralık dışı hafta (`0`, `54`) | `400 VALIDATION_ERROR` |
| Geçersiz sıralama alanı, çok büyük sayfa boyutu | `400` (ayrıntı 23. günde) |
| Herhangi bir filtre kombinasyonu | Hiçbir zaman `500` dönmez |
| Yükleniyor | Arayüzün işi. Backend'in payı listeleri sayfalı ve hızlı döndürmek (bkz. riskler) |

## 8. Kabul kriterleri

Örnekler mevcut seed verisine dayanıyor: Atlas IK (sorumlu Ayse, hafta 29 ORTA, hafta 30 DUSUK ve
hafta 31 ORTA raporları), e-Belge (sorumlu Mehmet, hafta 30 YUKSEK raporu) ve Nova POS (sorumlu Ayse, raporu yok).

1. **KK-01:** Filtresiz `GET /api/dashboard/summary` bugünkü yanıtın aynısını döner ve mevcut 47 test geçer.
2. **KK-02:** `riskLevel=ORTA,YUKSEK` Atlas IK'yı (son raporu hafta 31, ORTA) ve e-Belge'yi döner. Nova POS'un raporu olmadığı için listelenmez.
3. **KK-03:** `weekNumber=29` Atlas IK'yı hafta 29 değerleriyle (ORTA, GELISTIRME) döner. e-Belge ve Nova POS `hasReport=false` olarak görünür.
4. **KK-04:** Sorumlu filtresi Ayse'nin id'siyle Atlas IK ve Nova POS'u döner, sayaçlar bu iki projeye göre hesaplanır.
5. **KK-05:** Birden çok filtre VE ile birleşir (ör. sorumlu Ayse + risk YUKSEK → boş liste, `200`).
6. **KK-06:** Gecikme kuralının üç koşulunun her biri tek başına projeyi "gecikmede" yapar. Planlanan tarihi geçmiş ama durumu `TAMAMLANDI` olan iş kalemi gecikme sayılmaz. Planlanan tarihi olmayan iş kalemi yalnızca durumu `GECIKTI` ise gecikmiş sayılır.
7. **KK-07:** Rapor listesi tüm projelerin raporlarını döner. Proje, hafta/tarih aralığı, durum, risk ve sorumlu ile süzülebilir, varsayılan sıralamada en yeni rapor en üstte olur ve sonuç sayfalıdır.
8. **KK-08:** İş kalemi listesi sorumlu (K-T13-09), durum, proje ve gecikme ile süzülebilir.
9. **KK-09:** Bölüm 7'deki her hata durumu tabloda yazan kodu ve mevcut `ApiError` formatını döner.
10. **KK-10:** Swagger'da her filtre parametresinin açıklaması ve örneği vardır. README'deki "Known gaps" bölümünden T13 maddesi kaldırılır.

## 9. API ve veri etkisi (özet)

- **Değişen endpoint:** `GET /api/dashboard/summary`, yeni filtre parametreleri alır.
- **Yeni endpointler:** `GET /api/reports` (projeler arası rapor listesi) ve `GET /api/work-items`
  (projeler arası iş kalemi listesi).
- **Şema:** yeni tablo veya kolon yok, tüm filtreler mevcut alanlardan çalışır.
- **Örnek veri:** gecikme filtresini gösterebilmek için seed'e planlanan tarihli iş kalemleri
  içeren yeni bir rapor eklenir (Atlas IK hafta 31). Mevcut kayıtlar değişmez.

## 10. Riskler ve açık sorular

Netleştirilecek konular:

1. **Gecikme tanımı (K-T13-01)** iş tarafının beklentisine uyuyor mu? Referans tarih olarak
   raporun tarihi mi, yoksa "bugün" mü kullanılmalı?
2. **Hafta numarasında yıl bilgisi yok.** `(project_id, week_number)` benzersizlik kısıtı yıl
   değişiminde çakışır: bir proje 2027'nin 30. haftası için rapor giremez, hafta filtresi de
   hangi yılın haftası olduğunu bilemez. T13'ün kapsamı dışında, ama staj sonuna kadar
   çözülmesi gereken bir risk. Öneri: rapora yıl bilgisi eklemek veya haftayı rapor tarihinden türetmek.
3. **Yazım hatalı parametreler sessizce yok sayılıyor.** Örneğin `?risk=YUKSEK` hata vermez,
   filtresiz sonuç döner. Bu Spring'in standart davranışı; reddedilmesi gerekir mi, yoksa
   dokümante etmek yeterli mi?
4. **Performans:** dashboard her proje için ayrı sorgu atıyor (en son rapor ve canlı task
   sayısı). Hafta ve gecikme filtreleri buna yeni sorgular ekleyecek. Proje sayısı küçükken
   sorun değil; T17'deki performans kontrolünde ele alınacak.

## 11. Öncelik

| Konu | Öncelik |
|---|---|
| Dashboard: proje, hafta, durum, risk, sorumlu, gecikme filtreleri | T13 (MVP) |
| Projeler arası rapor listesi: filtre, sayfalama, sıralama | T13 (MVP) |
| Projeler arası iş kalemi listesi: sorumlu, durum, gecikme | T13 (MVP) |
| Hata ve boş sonuç davranışları, testler, Swagger ve README | T13 (MVP) |
| Rol bazlı filtre kapsamı | T14 |
| "Bu hafta raporu girilmemiş" göstergesi, haftalık trend | Sonra (T17) |
| Serbest metin araması, kayıtlı filtreler | Opsiyonel |
