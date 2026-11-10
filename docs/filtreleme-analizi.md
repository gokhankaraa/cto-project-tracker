# Filtreleme ve Durum Zenginleştirme: Kısa Analiz

**Hazırlayan:** Gökhan Kara
**Tarih:** 22 Ekim 2026

> **Hedef:** Proje, hafta, durum, risk, sorumlu ve gecikme filtrelerini eklemek;
> boş/yükleniyor/hata durumlarını iyileştirmek.

Bu not, filtrelerin *ne anlama geldiğini* ve *ne zaman doğru çalışmış sayılacağını* tanımlar.
Parametre adları, sayfalama ve sıralama gibi teknik ayrıntılar [filtre sözleşmesinde](filtre-sozlesmesi.md)
netleşir.

---

## 1. Amaç

MVP'de CTO bütün portföyü tek ekranda görebiliyor, ama listeyi daraltamıyor. Proje sayısı
arttıkça CTO'nun asıl sorduğu sorular şunlar oluyor:

- "Hangi projeler riskli ya da gecikmede?"
- "Ayşe'nin projeleri ne durumda?"
- "Geçen hafta portföy nasıl görünüyordu?"

Filtreleme bu soruların her birine tek bir istekle cevap verebilmeyi hedefliyor.

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
| **Gecikme** | Bkz. bölüm 5, KF-01 | Dashboard, iş kalemi listesi |

## 4. Kapsam dışı (bu çalışmada yapılmayacaklar)

- **Rol bazlı kısıt:** proje yöneticisinin yalnızca kendi projelerini görmesi kimlik doğrulama ve
  rol bazlı yetki adımının işi. Filtreler bu kısıtın sonradan tek bir ek koşulla eklenebileceği
  şekilde kurulur.
- Rapor metinlerinde (yapılanlar, riskler vb.) serbest metin araması.
- Kayıtlı filtreler ve filtre şablonları.
- Takvim uyumu (`scheduleStatus`): bu alan MVP'de hiç eklenmedi (ön analiz, karar 4).
- Filtrenin URL'de saklanması ve yükleniyor göstergesi: frontend'in konusu.

## 5. Kararlar

| No | Karar | Gerekçe |
|---|---|---|
| KF-01 | **Gecikme tanımı.** Bir iş kalemi gecikmiştir: durumu `GECIKTI` ise **veya** planlanan tarihi bağlı olduğu raporun tarihinden önceyse ve durumu `TAMAMLANDI` değilse. Bir proje gecikmededir: değerlendirilen raporunun genel durumu `GECIKMELI` ise **veya** o raporda en az bir gecikmiş iş kalemi varsa. | Rapor haftalık bir anlık görüntü (snapshot) olduğu için referans tarih "bugün" değil raporun kendi tarihi. Böylece sonuç saate bağlı olmaz ve test edilebilir kalır, geçmiş haftalar da kendi haftalarındaki duruma göre değerlendirilir. |
| KF-02 | Farklı filtreler **VE** ile birleşir. Aynı parametredeki birden çok değer **VEYA** ile birleşir (ör. `ORTA,YUKSEK` = orta veya yüksek riskli). | CTO'nun en sık sorusu "orta ve yüksek riskliler" gibi bir aralık. |
| KF-03 | Filtre verilmezse mevcut davranış aynen korunur. Mevcut `?riskLevel=YUKSEK` kullanımı da çalışmaya devam eder. | Geriye uyumluluk; MVP testleri ve README'deki demo senaryosu bozulmaz. |
| KF-04 | Rapora dayalı filtreler (risk, rapor durumu, gecikme) verildiğinde raporu olmayan projeler listelenmez. Projeye dayalı filtrelerde (proje, proje durumu, sorumlu) bu projeler `hasReport=false` ile listelenir. | Bugünkü risk filtresi de bu şekilde çalışıyor; kural tüm filtrelere genellenir. |
| KF-05 | Hafta filtresi bir filtreden çok bir **referans noktası**: X. haftaya kadar hiç raporu olmayan projeler de `hasReport=false` ile görünür. | CTO'nun "o hafta kim rapor girmemişti" sorusunu da görebilmesi için. |
| KF-06 | Dashboard sayaçları (toplam proje, yüksek riskli, raporsuz, canlı task) filtrelenmiş kümeye göre hesaplanır. | Ekrandaki liste ile sayaçlar her zaman tutarlı kalır. |
| KF-07 | Filtre sonucu boşsa yanıt `200` ve boş liste olur, `404` dönmez. Var olmayan bir proje id'siyle filtrelemek de boş liste döndürür. | Filtre bir kayıt arama işlemi değildir; "sonuç yok" bir hata değildir. |
| KF-08 | Sayfalama rapor ve iş kalemi listelerinde olur, dashboard'da olmaz. | Rapor ve iş kalemi sayısı zamanla büyür. Proje sayısı küçük kalır ve sayaçlar tüm küme üzerinden hesaplanmalıdır. |
| KF-09 | İş kalemi sorumlusu araması büyük/küçük harfe duyarsız ve "içerir" mantığıyla yapılır (`assignee=ayse` → "Ayşe Yılmaz"). | `assignee` serbest metin; tam eşleşme kullanışsız olur. |

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
| Sonuç yok | `200`, boş liste, sayaçlar `0` (KF-07) |
| Tanımsız enum değeri, sayı yerine metin (`?riskLevel=COK_YUKSEK`, `?projectId=abc`) | `400 INVALID_REQUEST`, mesajda parametre adı geçer (zaten mevcut) |
| Ters aralık (`weekFrom > weekTo`, `dateFrom > dateTo`) | `400 VALIDATION_ERROR`, ilgili alan `fieldErrors` içinde |
| Aralık dışı hafta (`0`, `54`) | `400 VALIDATION_ERROR` |
| Geçersiz sıralama alanı, çok büyük sayfa boyutu | `400` (ayrıntı filtre sözleşmesinde) |
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
8. **KK-08:** İş kalemi listesi sorumlu (KF-09), durum, proje ve gecikme ile süzülebilir.
9. **KK-09:** Bölüm 7'deki her hata durumu tabloda yazan kodu ve mevcut `ApiError` formatını döner.
10. **KK-10:** Swagger'da her filtre parametresinin açıklaması ve örneği vardır. README'deki "Known gaps" bölümünden filtreleme maddesi kaldırılır.

## 9. API ve veri etkisi (özet)

- **Değişen endpoint:** `GET /api/dashboard/summary`, yeni filtre parametreleri alır.
- **Yeni endpointler:** `GET /api/reports` (projeler arası rapor listesi) ve `GET /api/work-items`
  (projeler arası iş kalemi listesi).
- **Şema:** yeni tablo veya kolon yok, tüm filtreler mevcut alanlardan çalışır.
- **Örnek veri:** gecikme filtresini gösterebilmek için seed'e planlanan tarihli iş kalemleri
  içeren yeni bir rapor eklenir (Atlas IK hafta 31). Mevcut kayıtlar değişmez.

## 10. Riskler ve açık sorular

Netleştirilecek konular ve kararlar (kararlar 5 Kasım 2026'da verildi):

1. **Gecikme tanımı (KF-01)** iş tarafının beklentisine uyuyor mu? Referans tarih olarak
   raporun tarihi mi, yoksa "bugün" mü kullanılmalı?
   **Karar:** Raporun tarihi kalır. Rapor o haftanın anlık görüntüsüdür, sonuç saate bağlı olmaz
   ve geçmiş haftalar kendi tarihlerine göre değerlendirilir. Bilinen sınır: yeni rapor girilmezse
   sonradan oluşan gecikme görünmez. Bunu sonraki bir adımda eklenecek "bu hafta raporu girilmemiş"
   göstergesi karşılar.
2. **Hafta numarasında yıl bilgisi yok.** `(project_id, week_number)` benzersizlik kısıtı yıl
   değişiminde çakışır: bir proje 2027'nin 30. haftası için rapor giremez, hafta filtresi de
   hangi yılın haftası olduğunu bilemez. Bu çalışmanın kapsamı dışında, ama proje sonuna kadar
   çözülmesi gereken bir risk. Öneri: rapora yıl bilgisi eklemek veya haftayı rapor tarihinden türetmek.
   **Karar:** PostgreSQL geçişinden hemen sonra ayrı bir adımda rapora yıl kolonu eklenir (Flyway
   V2). Benzersizlik kısıtı `(project_id, year, week_number)` olur, hafta filtrelerine yıl eklenir.
3. **Yazım hatalı parametreler sessizce yok sayılıyor.** Örneğin `?risk=YUKSEK` hata vermez,
   filtresiz sonuç döner. Bu Spring'in standart davranışı; reddedilmesi gerekir mi, yoksa
   dokümante etmek yeterli mi?
   **Karar:** Dokümante etmek yeterli. Davranış README'de yazılı.
4. **Performans:** dashboard her proje için ayrı sorgu atıyor (en son rapor ve canlı task
   sayısı). Hafta ve gecikme filtreleri buna yeni sorgular ekleyecek. Proje sayısı küçükken
   sorun değil; sonraki bir performans kontrolünde ele alınacak.
   **Karar:** Sonraki bir adımda ölçülüp ele alınır.

## 11. Öncelik

| Konu | Öncelik |
|---|---|
| Dashboard: proje, hafta, durum, risk, sorumlu, gecikme filtreleri | Bu çalışma |
| Projeler arası rapor listesi: filtre, sayfalama, sıralama | Bu çalışma |
| Projeler arası iş kalemi listesi: sorumlu, durum, gecikme | Bu çalışma |
| Hata ve boş sonuç davranışları, testler, Swagger ve README | Bu çalışma |
| Rol bazlı filtre kapsamı | Kimlik doğrulama ve rol bazlı yetki adımı |
| "Bu hafta raporu girilmemiş" göstergesi, haftalık trend | Sonra |
| Serbest metin araması, kayıtlı filtreler | Opsiyonel |

## 12. Kapanış kontrolü (5 Kasım 2026)

Kanıt kaynakları: `./mvnw test` (100 test, 0 hata), çalışan uygulamaya karşı elle denenen
istekler ve Swagger şeması (`/v3/api-docs`).

| Kriter | Durum | Kanıt |
|---|---|---|
| KK-01 | Sağlandı | MVP'den kalan testler (`MvpIntegrationTest` dahil) değişmeden geçiyor. Filtresiz yanıta yalnızca yeni alan eklendi (S-06). |
| KK-02 | Sağlandı | `DashboardServiceTest` |
| KK-03 | Sağlandı | `FilterIntegrationTest.dashboardWeekReference` |
| KK-04 | Sağlandı | `FilterIntegrationTest.dashboardOwnerFilterAndCounters` |
| KK-05 | Sağlandı | `DashboardServiceTest` (filtrelerin VE ile birleşmesi) |
| KK-06 | Sağlandı | `WorkItemQueryTest` (`delayedConditionCoversAllCases`, `reportLevelDelay`, `queryConditionMatchesDelayPolicy`), `DashboardServiceTest.delayedFilterAndCounter` |
| KK-07 | Sağlandı | `WeeklyReportQueryTest`, `FilterIntegrationTest` |
| KK-08 | Sağlandı | `WorkItemQueryTest.filtersCombine` |
| KK-09 | Sağlandı | `FilterIntegrationTest.invalidRequestsReturn400` (17 hatalı istek). Hiçbir durumda `500` dönmüyor. |
| KK-10 | Sağlandı | Üç endpoint'teki 27 parametrenin hepsinde Swagger açıklaması ve örneği var. README'deki "Known gaps" bölümünden filtreleme maddesi kaldırıldı. |

Sonuç: filtreli dashboard ile rapor ve iş kalemi listeleri, otomatik testler ve güncel README.
Filtreleme çalışması bu haliyle kapanır.
