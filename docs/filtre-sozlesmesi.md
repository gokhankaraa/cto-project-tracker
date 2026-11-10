# Filtre Sözleşmesi (API)

**Hazırlayan:** Gökhan Kara
**Tarih:** 22 Ekim 2026
**Dayandığı doküman:** [filtreleme-analizi.md](filtreleme-analizi.md) (KF-xx kararları ve KK-xx kabul kriterleri oradadır)
**Güncelleme (6 Kasım 2026):** Raporlara yıl eklendi (Flyway V2). Yıl istekte gönderilmez; rapor
tarihinin ISO hafta-yılından türetilir. Yanıtlara `year` / `lastYear` alanları eklendi ve "en son rapor"
artık önce yıla, sonra haftaya göre seçilir.
**Güncelleme (9 Kasım 2026):** Üç endpoint'e `year` parametresi eklendi. Hafta parametreleri
(`weekNumber`, `weekFrom`, `weekTo`) artık yalnızca `year` ile birlikte verilebilir; yılsız hafta
`400 VALIDATION_ERROR` döner (bölüm 1 ve 5).
**Güncelleme (9 Kasım 2026, yetkilendirme):** Üç endpoint giriş ister. Proje yöneticisi yalnızca
sorumlusu olduğu projelerin verisini görür; koşul filtrelerle VE ile birleşir (bölüm 8). Bu
dokümandaki örnekler CTO ve Admin için geçerlidir.
**Güncelleme (9 Kasım 2026, Flyway V3):** Raporun yılı artık rapor tarihinin değil, **anlattığı
haftanın** ISO yılıdır: hafta numarası ve rapor tarihinden türetilir (o haftanın rapor tarihine en
yakın olduğu yıl). Rapor, anlattığı haftanın içinde ya da ertesi haftasında yazılır; aksi halde rapor
oluşturma/güncelleme `400 VALIDATION_ERROR` döner. Seed verisinde yıl değişmedi; filtre parametreleri ve
bu dokümandaki örnekler aynen geçerlidir. Değişen tek durum yıl sınırıdır: 53. haftanın 4 Ocak 2027'de
yazılan raporu artık 2027 değil 2026 yılındadır.

**Güncelleme (10 Kasım 2026, risk / engel / karar / aksiyon):** Dördüncü liste endpoint'i eklendi:
`GET /api/tracking-items` (bölüm 4a). Dashboard yanıtına takip kayıtlarının sayaçları ve proje satırı
alanları eklendi (bölüm 2). Bu kayıtlar haftaya bağlı olmadığı için yıl / hafta referansı onlara
uygulanmaz; gecikme bugünün tarihine göredir.

**Güncelleme (10 Kasım 2026, haftalık trend):** `GET /api/dashboard/trend` eklendi (bölüm 2a). Dashboard'un
proje filtrelerini ve hafta referansı kuralını her haftaya ayrı ayrı uygular. Dashboard'un Excel çıktısı
`GET /api/dashboard/export` da bölüm 2'nin parametrelerini kullanır (bölüm 2a'nın sonu).

Bu doküman dört liste endpoint'inin filtre parametrelerini, doğrulama kurallarını, yanıt yapısını ve hata
davranışını tanımlar. Geliştirme ve testler bu sözleşmeye göre yapıldı.

---

## 1. Ortak kurallar

| Kural | Açıklama |
|---|---|
| Birleştirme | Farklı parametreler **VE** ile birleşir. Çok değerli parametrelerde değerler **VEYA** ile birleşir (KF-02). |
| Çok değerli parametre | Virgülle (`riskLevel=ORTA,YUKSEK`) veya tekrarla (`riskLevel=ORTA&riskLevel=YUKSEK`) verilebilir. Tabloda "çoklu" olarak işaretlenmiştir. |
| Verilmeyen parametre | Filtre uygulanmaz. Hiç parametre verilmezse mevcut davranış korunur (KF-03). |
| Boş değer | `?ownerId=` gibi boş verilen parametre, verilmemiş sayılır. |
| Enum değerleri | Büyük harf ve koddaki adıyla yazılır (`YUKSEK`, `DEVAM_EDIYOR`). |
| Tarih biçimi | ISO 8601, `yyyy-MM-dd` (ör. `2026-07-27`) |
| Hafta numarası | 1–53 (rapor oluştururken kullanılan kuralla aynı). Yıl olmadan anlamsız olduğu için hafta parametreleri yalnızca `year` ile birlikte verilebilir. |
| Yıl | 2000–2100. Raporun yılı, anlattığı haftanın ISO yılıdır (ör. 53. hafta, rapor tarihi 2027-01-04 → 2026). |
| Var olmayan id | Hata dönmez, boş sonuç döner (KF-07). |
| Gecikme | Analiz notundaki KF-01 tanımı kullanılır. Hesaplama tek bir yerde yapılır, üç endpoint de aynı kuralı kullanır. |

**Değerlendirilen rapor:** Dashboard ve iş kalemi listesi her proje için tek bir raporu esas alır:

- `year` verilmezse projenin **en son** raporu (en yeni yıl, o yıl içinde en yüksek hafta),
- yalnızca `year=Y` verilirse projenin **Y yılının sonuna kadarki** en son raporu,
- `year=Y&weekNumber=X` verilirse projenin **Y yılının X. haftasına kadarki (dahil)** en son raporu
  (KF-05). Önceki yılların raporları da adaydır: `year=2027&weekNumber=1` için o haftada rapor
  yoksa 2026'nın son raporu esas alınır.

Karşılaştırma yıl ve haftayı birlikte taşıyan tek bir anahtarla yapılır: `year * 100 + weekNumber`
(ör. 2026 hafta 29 → 202629; yalnızca yıl verilirse 202653).

---

## 2. `GET /api/dashboard/summary` (mevcut endpoint, genişletilir)

### Parametreler

| Parametre | Tip | Çoklu | Doğrulama | Neye uygulanır |
|---|---|---|---|---|
| `projectId` | Long | — | — | Proje |
| `projectStatus` | `ProjectStatus` | ✓ | Tanımlı enum | Proje |
| `ownerId` | Long | — | — | Proje (sorumlu PM) |
| `year` | Integer | — | 2000–2100 | Referans yıl (değerlendirilen rapor) |
| `weekNumber` | Integer | — | 1–53, yalnızca `year` ile | Referans hafta (değerlendirilen rapor) |
| `overallStatus` | `OverallStatus` | ✓ | Tanımlı enum | Değerlendirilen rapor |
| `riskLevel` | `RiskLevel` | ✓ | Tanımlı enum | Değerlendirilen rapor (mevcut parametre) |
| `delayed` | Boolean | — | `true` / `false` | Değerlendirilen rapor: `true` yalnızca gecikenler, `false` yalnızca gecikmeyenler |

- Proje parametreleri (`projectId`, `projectStatus`, `ownerId`) raporu olmayan projeleri de döndürür.
  Rapor parametreleri (`overallStatus`, `riskLevel`, `delayed`) verildiğinde bu projeler listelenmez (KF-04).
- `year` / `weekNumber` hiçbir projeyi elemez. O noktaya kadar raporu olmayan projeler `hasReport=false` ile döner.
- Sayfalama yoktur (KF-08). Sıralama bugünkü gibi proje id'sine göre artan.

### Yanıt: mevcut alanlar aynen kalır, yeni alanlar eklenir

| Nesne | Yeni alan | Tip | Açıklama |
|---|---|---|---|
| `DashboardSummaryResponse` | `delayedProjects` | long | Filtrelenmiş kümedeki geciken proje sayısı |
| `DashboardProjectSummary` | `delayed` | boolean | Değerlendirilen rapora göre proje gecikmede mi (raporu yoksa `false`) |
| `DashboardProjectSummary` | `lastYear` | Integer | Değerlendirilen raporun yılı (V2; raporu yoksa `null`) |
| `DashboardSummaryResponse` | `openRisks` | long | Listelenen projelerde kapanmamış risk sayısı (V5) |
| `DashboardSummaryResponse` | `openBlockers` | long | Kapanmamış engel sayısı |
| `DashboardSummaryResponse` | `pendingDecisions` | long | Verilmemiş (kapanmamış) karar sayısı |
| `DashboardSummaryResponse` | `overdueTrackingItems` | long | Takip tarihi geçmiş, kapanmamış kayıt sayısı (dört tür) |
| `DashboardProjectSummary` | `openTrackingItems` | long | Projenin kapanmamış kayıt sayısı |
| `DashboardProjectSummary` | `overdueTrackingItems` | long | Projenin takip tarihi geçmiş kayıt sayısı |
| `DashboardProjectSummary` | `highestOpenRiskLevel` | `RiskLevel` | Kapanmamış risk ve engellerin en yüksek seviyesi; yoksa `null` |

Mevcut alanlarda değişiklik yok: `lastYear` / `lastWeekNumber`, `year` / `weekNumber` verildiğinde
değerlendirilen raporun yılını ve haftasını gösterir. Sayaçlar filtrelenmiş kümeye göre hesaplanır (KF-06).

Takip kaydı alanları (V5) yalnızca listelenen projelerin kayıtlarından hesaplanır: proje ve rapor
filtreleri onları da daraltır. `year` / `weekNumber` ise bu alanları **değiştirmez**. Kayıtlar haftaya
bağlı olmadığından her zaman bugünkü durum gösterilir.

### Örnekler (seed verisi)

| İstek | Sonuç |
|---|---|
| `GET /api/dashboard/summary` | 3 proje, `totalLiveTasks: 2`, `delayedProjects: 1` (KK-01) |
| `?riskLevel=YUKSEK` | Yalnızca e-Belge (mevcut davranış) |
| `?riskLevel=ORTA,YUKSEK` | Atlas IK (son raporu hafta 31, `ORTA`) ve e-Belge (KK-02) |
| `?year=2026&weekNumber=29` | Atlas IK hafta 29 değerleriyle (`ORTA`, `GELISTIRME`); e-Belge ve Nova POS `hasReport=false` (KK-03) |
| `?year=2025` | 3 proje de `hasReport=false`, `projectsWithoutReport: 3` (2025'te rapor yok) |
| `?year=2027&weekNumber=1` | En son rapor davranışıyla aynı: Atlas IK hafta 31, e-Belge hafta 30 (önceki yıl dahil) |
| `?weekNumber=29` | `400 VALIDATION_ERROR`, `fieldErrors[0].field: weekNumber` (yıl yok) |
| `?ownerId=1` | Atlas IK ve Nova POS, `totalProjects: 2` (KK-04) |
| `?ownerId=1&riskLevel=YUKSEK` | Boş liste, tüm sayaçlar 0, `200` (KK-05) |
| `?delayed=true` | Yalnızca Atlas IK: hafta 31'de planlanan tarihi geçmiş, bitmemiş bir iş kalemi var |
| `GET /api/dashboard/summary` (takip alanları) | `openRisks: 1`, `openBlockers: 1`, `pendingDecisions: 1`, `overdueTrackingItems: 1`. Atlas IK 2 açık kayıt (1 gecikmiş, en yüksek `YUKSEK`), e-Belge 2 açık kayıt (`ORTA`), Nova POS 0 (`null`) |
| `?projectId=2` | Takip sayaçları yalnızca e-Belge'den: `openBlockers: 1`, `openRisks: 0` |

---

## 2a. `GET /api/dashboard/trend` (haftalık trend)

Referans haftada biten bir penceredeki her hafta için portföyün **o haftaki** halini döner. Her hafta için
bölüm 2'deki hafta referansı kuralı uygulanır: her proje o haftaya kadarki (dahil) en son raporuyla
değerlendirilir. Bu yüzden son hafta, aynı `year` / `weekNumber` ile açılan dashboard'un sayaçlarıyla aynıdır.

### Parametreler

| Parametre | Tip | Çoklu | Doğrulama | Açıklama |
|---|---|---|---|---|
| `projectId` | Long | — | — | Bölüm 2'deki gibi |
| `projectStatus` | `ProjectStatus` | ✓ | Tanımlı enum | Bölüm 2'deki gibi |
| `ownerId` | Long | — | — | Bölüm 2'deki gibi |
| `year` | Integer | — | 2000–2100 | Pencerenin bittiği yıl. Yalnızca yıl verilirse o yılın son haftası (52 ya da 53) |
| `weekNumber` | Integer | — | 1–53, yalnızca `year` ile | Pencerenin bittiği hafta |
| `weeks` | Integer | — | 1–52, varsayılan `12` | Pencere uzunluğu |

- `year` verilmezse pencere bugünün haftasında biter. Pencere yıl sınırını geçebilir (2026 h2'de biten
  4 hafta: 2025 h51, 2025 h52, 2026 h1, 2026 h2).
- Rapora dayalı filtreler (`overallStatus`, `riskLevel`, `delayed`) yoktur: trend bunların haftadan haftaya
  dağılımını gösterir.

### Yanıt

`{ weeks: TrendWeek[] }`, eskiden yeniye. Her `TrendWeek`:

| Alan | Açıklama |
|---|---|
| `year`, `weekNumber`, `weekStart` | ISO yılı, haftası ve haftanın pazartesisi |
| `totalProjects`, `projectsWithReport` | Filtreye uyan proje sayısı; o haftaya kadar raporu olanlar |
| `projectsByStatus` | Genel duruma göre proje sayıları; dört durum da her zaman yer alır |
| `highRiskProjects`, `delayedProjects` | Bölüm 2'deki sayaçlarla aynı kural (gecikme: KF-01) |
| `averageProgress` | Raporu olan projelerin ortalama ilerlemesi (yuvarlanmış); raporlu proje yoksa `null` |
| `openRisks`, `openBlockers`, `pendingDecisions`, `openActions` | Hafta sonunda açık kayıtlar: haftanın sonundan önce oluşturulmuş ve o güne kadar kapanmamış |
| `openedTrackingItems`, `closedTrackingItems` | Hafta içinde açılan ve kapanan kayıtlar |

- Hafta, Türkiye saatiyle pazartesi 00:00'da başlar (`Europe/Istanbul`).
- Kayıtların değişiklik geçmişi tutulmadığı için risk seviyesinin ya da takip tarihinin haftalar içindeki
  değişimi izlenemez. Bu yüzden trendde seviye kırılımı ve "gecikmiş takip" yoktur.

### Örnekler (seed verisi)

| İstek | Sonuç |
|---|---|
| `?year=2026&weekNumber=31&weeks=4` | Hafta 28: raporlu proje yok. Hafta 29: Atlas IK (`ORTA`, %50). Hafta 30: Atlas IK `DUSUK` %75, e-Belge `RISKLI` `YUKSEK` %25 (ortalama %50). Hafta 31: Atlas IK gecikmede (`delayedProjects: 1`) |
| `?weekNumber=31` | `400 VALIDATION_ERROR`, `fieldErrors[0].field: weekNumber` |
| `?weeks=53` | `400 VALIDATION_ERROR`, `fieldErrors[0].field: weeks` |

### Excel çıktısı: `GET /api/dashboard/export`

Bölüm 2'deki parametrelerin hepsini alır ve `cto-ozeti-<bugün>.xlsx` dosyası döner (Content-Type
`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `Content-Disposition: attachment`).
Sayfalar: *Özet* (uygulanan filtreler ve sayaçlar), *Projeler* (bölüm 2'deki satırlar), *Riskler ve
aksiyonlar* (listelenen projelerin kapanmamış kayıtları), *Trend* (aynı referansla biten 12 hafta).
Hatalı parametre bölüm 5'teki gibi JSON `400` döner; `Accept` başlığı sonucu değiştirmez.

---

## 3. `GET /api/reports` (yeni endpoint)

Tüm projelerin haftalık raporlarını filtreli ve sayfalı listeler. Projeye bağlı mevcut
`GET /api/projects/{projectId}/reports` endpoint'i değişmez.

### Parametreler

| Parametre | Tip | Çoklu | Doğrulama |
|---|---|---|---|
| `projectId` | Long | — | — |
| `ownerId` | Long | — | — |
| `year` | Integer | — | 2000–2100; raporun yılına eşitlik |
| `weekFrom` | Integer | — | 1–53, yalnızca `year` ile |
| `weekTo` | Integer | — | 1–53, yalnızca `year` ile, `weekFrom` ≤ `weekTo` |
| `dateFrom` | LocalDate | — | ISO tarih |
| `dateTo` | LocalDate | — | ISO tarih, `dateFrom` ≤ `dateTo` |
| `overallStatus` | `OverallStatus` | ✓ | Tanımlı enum |
| `riskLevel` | `RiskLevel` | ✓ | Tanımlı enum |
| `progressStage` | `ProgressStage` | ✓ | Tanımlı enum |
| `page` | Integer | — | ≥ 0, varsayılan `0` |
| `size` | Integer | — | 1–100, varsayılan `20` |
| `sort` | String | — | `alan` veya `alan,asc` / `alan,desc`. Varsayılan `reportDate,desc` |

- Aralık uçları dahildir: `year=2026&weekFrom=29&weekTo=30` → 2026'nın 29 ve 30. haftaları.
- Hafta aralığı `year` ile verilen yılın içinde uygulanır; yıllar arası bir aralık gerekiyorsa
  `dateFrom` / `dateTo` kullanılır.
- Sıralanabilir alanlar: `reportDate`, `weekNumber`, `projectName`. Sayfalar arası kayma olmaması
  için sıralamaya her zaman ikinci anahtar olarak `id` eklenir.

### Yanıt: sayfa yapısı

```json
{
  "content": [ { "...": "WeeklyReportResponse" } ],
  "page": 0,
  "size": 20,
  "totalElements": 4,
  "totalPages": 1
}
```

`WeeklyReportResponse` aynen kullanılır, ek alanlar `projectName` (String) ve `year` (Integer, V2).
`projectName`, projeler arası listede raporun hangi projeye ait olduğu id'ye bakmadan görünsün diye
eklenir. Ek alanlar diğer rapor endpoint'lerinin yanıtında da yer alır.

### Örnekler (seed verisi)

| İstek | Sonuç |
|---|---|
| `GET /api/reports` | 4 rapor, en yeni üstte: Atlas IK hafta 31, aynı tarihli hafta 30 raporları (id azalan: e-Belge, Atlas IK), sonra Atlas IK hafta 29 |
| `?projectId=1&year=2026&weekFrom=29&weekTo=29` | Atlas IK hafta 29 |
| `?year=2026` | 4 rapor (seed verisinin hepsi 2026'da) |
| `?year=2027` | `content: []`, `totalElements: 0` (`200`) |
| `?riskLevel=YUKSEK&ownerId=2` | e-Belge hafta 30 |
| `?size=2` | 2 kayıt, `totalElements: 4`, `totalPages: 2` |
| `?page=5` | `content: []`, `totalElements: 4` (`200`) |

---

## 4. `GET /api/work-items` (yeni endpoint)

İş kalemlerini projeler arası filtreli ve sayfalı listeler. Varsayılan olarak her projenin
**değerlendirilen raporundaki** iş kalemleri listelenir (bkz. S-05). Rapora bağlı mevcut
`GET /api/reports/{reportId}/work-items` endpoint'i değişmez.

### Parametreler

| Parametre | Tip | Çoklu | Doğrulama |
|---|---|---|---|
| `projectId` | Long | — | — |
| `year` | Integer | — | 2000–2100 (değerlendirilen rapor; bkz. bölüm 1) |
| `weekNumber` | Integer | — | 1–53, yalnızca `year` ile (değerlendirilen rapor; bkz. bölüm 1) |
| `assignee` | String | — | En fazla 255 karakter. Büyük/küçük harfe duyarsız, "içerir" araması (KF-09) |
| `status` | `WorkItemStatus` | ✓ | Tanımlı enum |
| `delayed` | Boolean | — | `true` / `false` |
| `page` | Integer | — | ≥ 0, varsayılan `0` |
| `size` | Integer | — | 1–100, varsayılan `20` |
| `sort` | String | — | Varsayılan `plannedDate,asc` (planlanan tarihi olmayanlar sonda) |

- Sıralanabilir alanlar: `plannedDate`, `title`, `projectName`. İkinci anahtar her zaman `id`.

### Yanıt

Bölüm 3'teki sayfa yapısı kullanılır, `content` elemanları `WorkItemResponse` olur. Eklenen alanlar:

| Yeni alan | Tip | Açıklama |
|---|---|---|
| `projectId` | Long | İş kaleminin projesi |
| `projectName` | String | Proje adı |
| `year` | Integer | Bağlı olduğu raporun yılı (V2) |
| `weekNumber` | Integer | Bağlı olduğu raporun haftası |
| `delayed` | boolean | KF-01'e göre gecikmiş mi |

### Örnekler (seed verisi)

| İstek | Sonuç |
|---|---|
| `GET /api/work-items` | 3 iş kalemi: Atlas IK hafta 31'in 2 kalemi, sonra planlanan tarihi olmayan e-Belge "Mevzuat analizi". Atlas IK'nın eski haftaları listelenmez (S-05), Nova POS'un raporu yok. |
| `?year=2026&weekNumber=29` | Atlas IK hafta 29'un 3 iş kalemi |
| `?year=2026&weekNumber=29&assignee=mehmet` | "Dashboard ozeti" |
| `?year=2026` | `GET /api/work-items` ile aynı 3 iş kalemi (2026 sonuna kadarki en son raporlar) |
| `?status=DEVAM_EDIYOR` | "Rapor ekrani entegrasyonu" ve "Mevzuat analizi" |
| `?delayed=true` | "Rapor ekrani entegrasyonu" (`delayed: true`) |

## 4a. `GET /api/tracking-items` (V5)

Risk, engel, karar ve aksiyon kayıtlarını projeler arası filtreli ve sayfalı listeler. Projeye bağlı
`GET /api/projects/{projectId}/tracking-items` listesi sayfasızdır; kapanmamış kayıtları takip tarihine
göre üstte, kapalıları en altta verir.

### Parametreler

| Parametre | Tip | Çoklu | Doğrulama |
|---|---|---|---|
| `projectId` | Long | — | — |
| `type` | `TrackingItemType` (`RISK`, `ENGEL`, `KARAR`, `AKSIYON`) | ✓ | Tanımlı enum |
| `status` | `TrackingItemStatus` (`ACIK`, `DEVAM_EDIYOR`, `KAPANDI`) | ✓ | Tanımlı enum |
| `riskLevel` | `RiskLevel` | ✓ | Tanımlı enum. Yalnızca risk ve engel kayıtlarında değer vardır |
| `overdue` | Boolean | — | `true`: kapanmamış ve takip tarihi bugünden önce; `false`: geri kalanlar (takip tarihi olmayan kapalı kayıtlar dahil) |
| `assignee` | String | — | En fazla 255 karakter. Büyük/küçük harfe duyarsız, "içerir" araması (KF-09) |
| `page` | Integer | — | ≥ 0, varsayılan `0` |
| `size` | Integer | — | 1–100, varsayılan `20` |
| `sort` | String | — | Varsayılan `followUpDate,asc` (takip tarihi olmayanlar sonda) |

- Sıralanabilir alanlar: `followUpDate`, `createdAt`, `title`, `projectName`. İkinci anahtar her zaman `id`.
- Yıl ve hafta parametresi yoktur. "Bugün" Türkiye saatine göredir (`Europe/Istanbul`).

### Yanıt

Bölüm 3'teki sayfa yapısı kullanılır; `content` elemanları `TrackingItemResponse` olur: `projectId`,
`projectName`, `type`, `title`, `description`, `riskLevel`, `impact`, `actionPlan`, `assignee`,
`followUpDate`, `status`, `closedDate`, `createdAt`, `overdue`.

### Örnekler (seed verisi)

Seed kayıtlarının tarihleri yüklendiği güne göredir; aşağıdaki sonuçlar her gün aynıdır.

| İstek | Sonuç |
|---|---|
| `GET /api/tracking-items` | 5 kayıt. İlk sırada takip tarihi geçmiş "Musteriyle kapsam toplantisi" (`overdue: true`), en sonda takip tarihi olmayan kapalı karar "Raporlama kutuphanesi secimi" |
| `?overdue=true` | Yalnızca "Musteriyle kapsam toplantisi" (Atlas IK, aksiyon) |
| `?type=KARAR&status=ACIK,DEVAM_EDIYOR` | Bekleyen karar "e-Arsiv saglayicisi secimi" |
| `?riskLevel=ORTA,YUKSEK` | Atlas IK'nın yüksek riski ve e-Belge'nin orta seviyeli engeli |
| `?assignee=zeynep&projectId=1` | Atlas IK'nın verilmiş kararı |

---

## 5. Hata davranışı

Tüm hatalar mevcut `ApiError` gövdesiyle döner, yeni hata kodu eklenmez.

| Durum | Örnek | HTTP | `code` | `fieldErrors` |
|---|---|---|---|---|
| Tanımsız enum değeri | `riskLevel=COK_YUKSEK` | 400 | `INVALID_REQUEST` | — (mesajda parametre adı geçer) |
| Tip uyuşmazlığı | `projectId=abc`, `year=yirmi`, `delayed=evet`, `dateFrom=27.07.2026` | 400 | `INVALID_REQUEST` | — |
| Aralık dışı değer | `weekNumber=54`, `year=1999`, `size=0`, `size=500`, `page=-1` | 400 | `VALIDATION_ERROR` | ilgili parametre |
| Yılsız hafta | `weekNumber=29`, `weekFrom=29`, `weekTo=30` (`year` olmadan) | 400 | `VALIDATION_ERROR` | ilgili hafta parametresi |
| Ters aralık | `year=2026&weekFrom=30&weekTo=29`, `dateFrom` > `dateTo` | 400 | `VALIDATION_ERROR` | `weekTo` / `dateTo` |
| Tanımsız takip kaydı türü / durumu | `type=SORUN`, `status=BITTI`, `overdue=evet` | 400 | `INVALID_REQUEST` | — |
| Uzun metin | `assignee` > 255 karakter | 400 | `VALIDATION_ERROR` | `assignee` |
| Geçersiz sıralama | `sort=password`, `sort=reportDate,yukari` | 400 | `VALIDATION_ERROR` | `sort` (mesajda izin verilen alanlar listelenir) |
| Sonuç yok | — | 200 | — | — |

**Bilinen davranış:** Tanımsız parametre adları (ör. `?risk=YUKSEK`) Spring'in standart davranışı
gereği yok sayılır ve filtresiz sonuç döner. Bu davranış README'de belirtilecek (analiz notu, açık soru 3).

---

## 6. Sözleşme düzeyindeki teknik kararlar

| No | Karar | Gerekçe |
|---|---|---|
| S-01 | Filtre parametreleri controller'da tek tek `@RequestParam` olarak alınır, servis katmanına bir filtre kaydı (record) olarak geçirilir. | Parametreleri doğrudan bir nesneye bağlamak (`@ModelAttribute`) tip hatalarını farklı bir istisna yolundan geçirir. Bu durumda mevcut testlerdeki `400 INVALID_REQUEST` davranışı değişir ve hata mesajına teknik dönüşüm metni sızabilir. |
| S-02 | Sayfalı yanıt için kendi `PageResponse<T>` kaydımızı kullanırız. Spring'in `Page` nesnesi doğrudan döndürülmez. | `Page` nesnesinin JSON yapısı Spring sürümüne bağlıdır (Spring Data bu kullanım için uyarı da basar). Kendi kaydımızla sözleşme sabit kalır. |
| S-03 | Sıralama alanları bir izin listesiyle sınırlanır ve API alan adı entity yoluna eşlenir (`projectName` → `project.name`). | Entity alan adları dışarı sızmaz. Tanımsız alan, Spring Data'dan istisna olarak çıkıp `500`'e düşmek yerine `400` döner. |
| S-04 | Fazla büyük `size` sessizce kırpılmaz, `400` döner. | Spring'in varsayılan sayfalama çözücüsü büyük değerleri sessizce kırpar. İstemcinin istediğinden az kayıt aldığını fark etmemesi yanıltıcı olur. |
| S-05 | İş kalemi listesi her projenin değerlendirilen raporuna göre çalışır, tüm haftalarınkini birden listelemez. | İş kalemleri haftalık raporun parçası olduğu için devam eden bir iş her hafta yeniden girilebilir. Tüm haftalar listelenirse aynı iş birden çok kez görünür ve 29. haftada gecikip 30. haftada tamamlanan bir iş hâlâ "gecikmiş" olarak çıkar. |
| S-06 | Yanıtlara yalnızca alan eklenir; mevcut alanlar kaldırılmaz, adı ya da anlamı değişmez. | Mevcut istemciler ve testler etkilenmez. |

---

## 7. Uygulama yaklaşımı

- **Proje koşulları** (`projectId`, `projectStatus`, `ownerId`) `Specification<Project>` olarak
  kurulur. Dashboard proje kümesini bu sorguyla alır.
- **Dashboard:** her proje için değerlendirilen rapor bulunur. Rapora dayalı filtreler (risk,
  rapor durumu, gecikme) bugünkü gibi özet satırları üzerinde uygulanır, çünkü proje sayısı küçük.
- **Rapor listesi:** `WeeklyReportRepository` `JpaSpecificationExecutor` ile genişletilir. Her
  parametre ayrı ve küçük bir `Specification` olur, verilen parametreler VE ile birleştirilir.
- **İş kalemi listesi:** `Specification<WorkItem>`. Değerlendirilen rapor koşulu bir alt
  sorguyla kurulur (projenin `year * 100 + hafta` anahtarı referansa eşit ya da küçük olan en yeni
  raporu; V2 öncesinde yalnızca hafta ≤ X idi), gecikme koşulu iş kaleminin
  planlanan tarihi ile raporun tarihi karşılaştırılarak yazılır.
- **Gecikme kuralı** tek bir sınıfta toplanır. Dashboard satırı, iş kalemi yanıtı ve sorgu
  koşulu aynı tanımı kullanır.
- **Örnek veri:** gecikme filtresini gösterebilmek için seed'e planlanan tarihli iş kalemleri
  içeren yeni bir rapor eklenir (Atlas IK hafta 31). Mevcut kayıtlar değişmez.

## 8. Yetkilendirme

Dört endpoint de giriş ister (`Authorization: Bearer <token>`; token yoksa `401`). Rol kısıtı,
kullanıcının filtrelerinden **önce** gelen ve her zaman uygulanan bir koşuldur:

| Rol | Görünürlük koşulu |
|---|---|
| CTO, Admin | Koşul yok (tüm projeler) |
| Proje yöneticisi | `project.owner = oturumdaki kullanıcı` |

Bu koşul dört endpoint'te de proje `Specification`'ına VE ile eklenir; filtre kodu değişmedi.
Sayaçlar ve sayfa toplamları da bu kümeden hesaplanır. Bir proje yöneticisi `?ownerId=` ya da
`?projectId=` ile başkasının projelerini istediğinde sonuç boş döner, hata dönmez (KF-07).

| İstek (seed verisi) | Sonuç |
|---|---|
| `GET /api/dashboard/summary`, Ayşe (PM) | Atlas IK ve Nova POS, `totalProjects: 2` |
| `GET /api/dashboard/summary?ownerId=2`, Ayşe | Boş liste, sayaçlar `0`, `200` |
| `GET /api/reports`, Mehmet (PM) | Yalnızca e-Belge hafta 30, `totalElements: 1` |
| `GET /api/work-items`, Mehmet | Yalnızca e-Belge iş kalemleri |
| `GET /api/tracking-items`, Mehmet | Yalnızca e-Belge'nin 2 kaydı; `?projectId=1` boş sonuç, `200` |
| `GET /api/dashboard/summary`, Mehmet | Takip sayaçları yalnızca e-Belge'den: `openBlockers: 1`, `pendingDecisions: 1` |
| `GET /api/dashboard/trend?year=2026&weekNumber=31&weeks=1`, Ayşe | `totalProjects: 2` (Atlas IK ve Nova POS) |
| Herhangi biri, CTO veya Admin | Bölüm 2–4'teki örneklerle aynı (tüm projeler) |

## 9. Test eşlemesi

| Test | Kapsam |
|---|---|
| `DashboardServiceTest` (genişletilir) | Her filtre tek başına ve birlikte, hafta referansı, gecikme kuralı, sayaçlar |
| `WeeklyReportQueryTest` (yeni, `@DataJpaTest`) | Rapor `Specification`'ları, aralık uçları, sıralama ve ikinci anahtar |
| `WorkItemQueryTest` (yeni, `@DataJpaTest`) | Değerlendirilen rapor alt sorgusu, `assignee` araması, gecikme koşulu |
| `FilterIntegrationTest` (yeni, MockMvc) | Bölüm 2–4'teki örnekler ve bölüm 5'teki her hata satırı |
| `TrackingItemQueryTest` (`@DataJpaTest`) | Bölüm 4a koşulları: gecikme (takip tarihi olmayan kapalı kayıt `false` sorgusunda kalır), çok değerli filtreler, `assignee` araması, proje sahibi |
| `ExportIntegrationTest` (MockMvc) | Excel çıktısı: sayfalar, filtrelerin uygulanması, hücre türleri, hata durumunda JSON 400 |
| `TrendIntegrationTest` (MockMvc) | Bölüm 2a: haftalık seriler, açılan / kapanan kayıtlar, Türkiye saatiyle hafta başlangıcı, yıl sınırı, son haftanın dashboard'la aynı olması, hata durumları |
| `TrackingItemIntegrationTest` (MockMvc) | Bölüm 4a örnekleri, sayfa ve sıralama; bölüm 2'deki takip sayaçları ve hafta referansının onları değiştirmemesi |
