# Haftalık Proje Durum Raporlama ve CTO Takip Sistemi — Ön Analiz

**Hazırlayan:** Gökhan Kara
**Tarih:** 20 Temmuz 2026

---

## 1. Problemi nasıl anlıyorum

Şirkette proje yöneticileri her hafta sorumlu oldukları projelerin durumunu dağınık kanallarla (PowerPoint, e-posta, toplantı notları) raporluyor. Bu yüzden CTO bütün projelerin güncel durumunu tek bir yerden göremiyor; bilgi hem dağınık hem de zaman geçtikçe eskiyor.

Ben bu projede, iş akışının son adımı olan haftalık raporlamayı dijitalleştiren bir backend geliştireceğim. Amacım, PM'lerin haftalık durum raporlarını tek bir yerden girebildiği, CTO'nun ise tüm proje portföyünü tek bir dashboard üzerinden izleyebildiği bir sistem kurmak.

Burada dikkat ettiğim önemli bir kapsam noktası var: bu sistem işi *yaptırmaz*, işin *durumunu* toplar. Yani bir proje yönetim aracı geliştirmiyorum; her hafta "bu proje nerede, ne kadar ilerledi, riski ne" bilgisini toplayan ve raporlayan bir araç kuruyorum.

---

## 2. Şirket iş akışında sistemin yeri

Şirketteki iş akışını şöyle anlıyorum: ihtiyaç doğar → PM değerlendirir → analiz yapılır → task'lar dağıtılır → geliştirme → kod review → test → düzeltme → deploy → haftalık raporlama.

Geliştireceğim sistem bu akışın son adımını, yani haftalık raporlamayı dijitalleştiriyor. Diğer adımları (geliştirme, test, deploy) sistemin kapsamına almıyorum; sistem yalnızca bu adımların çıktısı olan proje durumunu topluyor ve raporluyor.

---

## 3. Roller

Sisteme giren gerçek aktörleri üç tane olarak belirledim:

| Rol | Sistemdeki işlevi |
|---|---|
| Proje Yöneticisi (PM) | Ana veri giren kullanıcı. Sorumlu olduğu projeler için her hafta rapor girer. |
| CTO | Ana izleyen kullanıcı. Hiçbir projenin sahibi değildir ama tümünü görür; dashboard'dan portföyü izler. |
| Admin | Kullanıcı ve proje tanımlarını yönetir. |

Kapsam dışı bıraktığım roller: Analiz, Test/QA, Tasarım, Sistem ve Backend/Frontend ekipleri şirketin gerçek rolleri, ancak bu MVP sisteminde birer kullanıcı rolü olarak tutmuyorum. Bu roller iş akışının içinde yer alıyor; sistem yalnızca onların çıktısı olan proje durumunu raporluyor. İş kalemlerinin "sorumlu" alanında yalnızca isim olarak geçebilirler, sisteme login olan aktörler değiller.

---

## 4. Veri varlıkları ve alanları

MVP için dört çekirdek varlık tanımladım.

### User
- id
- email
- password (hash'lenmiş olarak saklanır, asla düz metin değil)
- fullName
- role (enum: PROJECT_MANAGER / CTO / ADMIN)

### Project
- id
- name
- customer
- description
- status (enum: PLANLANDI / DEVAM_EDIYOR / TAMAMLANDI / RISKLI / BLOKE)
- owner (User'a foreign key — projeden sorumlu PM)

### WeeklyReport
- id
- project (foreign key)
- reportDate
- weekNumber
- progressStage (enum: ANALIZ / GELISTIRME / TEST / TAMAMLANDI — her kademe bir yüzdeye karşılık gelir: %25 / %50 / %75 / %100)
- overallStatus (enum)
- riskLevel (enum: DUSUK / ORTA / YUKSEK)
- done (o hafta yapılanlar, metin)
- planned (gelecek hafta yapılacaklar, metin)
- risks (riskler / engeller, metin)
- note (genel durum notu, metin)

### WorkItem
- id
- weeklyReport (foreign key)
- title
- description
- assignee (sorumlu — MVP'de basit metin alanı)
- status (enum: PLANLANDI / DEVAM_EDIYOR / TESTTE / TAMAMLANDI / GECIKTI / RISKLI / BLOKE)
- plannedDate
- completedDate

### İlişkiler
- Bir User (PM) → birden çok Project
- Bir Project → birden çok WeeklyReport
- Bir WeeklyReport → birden çok WorkItem

CTO ve Admin hiçbir projenin sahibi değil; CTO tüm projeleri okuma yetkisiyle görüyor.

---

## 5. Endpoint taslağı

Her çekirdek varlık için CRUD işlemleri ve bir adet CTO dashboard özet endpoint'i planladım.

| Metot | Yol | Açıklama |
|---|---|---|
| GET / POST | /api/projects | Projeleri listele / oluştur |
| GET / PUT / DELETE | /api/projects/{id} | Proje detay / güncelle / sil |
| GET / POST | /api/projects/{id}/reports | Projeye ait haftalık raporlar |
| GET / PUT / DELETE | /api/reports/{id} | Rapor detay / güncelle / sil |
| GET / POST | /api/reports/{id}/work-items | Rapora ait iş kalemleri |
| GET / PUT / DELETE | /api/work-items/{id} | İş kalemi detay / güncelle / sil |
| GET | /api/dashboard/summary | CTO için tüm projelerin özet durumu |

---

## 6. Teknoloji kararım

**Dil / framework:** Java + Spring Boot seçtim. Kurumsal backend geliştirmede yaygın ve olgun bir yığın; öğrenmek istediğim teknoloji de bu.

**Veritabanı:** Geliştirme aşamasında H2 kullanacağım (gömülü, sıfır kurulum, MVP için fazlasıyla yeterli). Üretim ortamına geçişte PostgreSQL'e taşınabilir.

**API dokümantasyonu:** Swagger / OpenAPI kullanacağım, böylece endpoint'leri kolayca test edebilirim.

---

## 7. Varsayımlarım ve kararlarım

Bazı noktalar ihtiyaçta net tanımlanmadığı için, MVP kapsamını daraltmak ve hızlı ilerleyebilmek adına aşağıdaki makul varsayımları benimsedim. Her kararı gerekçesiyle birlikte yazdım; ihtiyaç halinde ileride revize edebilirim.

1. **Proje + hafta benzersizdir.** Bir proje bir hafta için tek rapor alır. WeeklyReport üzerinde project + weekNumber için unique constraint tanımlayacağım. Gerekçe: "haftalık rapor" kavramıyla en tutarlı yaklaşım bu.

2. **İlerleme, kademeli durum olarak tutulur; yüzde otomatik türer.** Serbest bir yüzde girmek yerine, projenin ilerlemesini tanımlı kademeler üzerinden tutacağım: ANALIZ, GELISTIRME, TEST, TAMAMLANDI. Her kademe sabit bir yüzdeye karşılık gelir (%25, %50, %75, %100). Proje bu kademeler arasında sırayla ilerler ve yalnızca bir sonraki kademeye geçebilir (sıra atlanmaz). Yüzde değeri kademeden otomatik hesaplanır, elle girilmez. Gerekçe: ilerleme herkes için nesnel ve tutarlı olur; dashboard'da hem projenin hangi aşamada olduğu hem de yüzdesi net görünür. 

3. **Canlı task, iş kalemi listesinden türetilir.** Ayrı bir "aktif task sayısı" alanı tutmuyorum; gerektiğinde durumu DEVAM_EDIYOR olan WorkItem'ları sayacağım. Gerekçe: veriyi tek kaynakta tutarak tutarsızlık riskini ortadan kaldırıyorum.

4. **Genel durum, takvim ve risk ayrı alanlardır.** overallStatus ve riskLevel çekirdek alanlarım; scheduleStatus (takvim uyumu) alanını süre kalırsa ekleyeceğim. Gerekçe: bu üçü ayrı kavramlardır. MVP'de takvim alanını ilk sürümde atlayabilirim.

5. **Rapor düzenlenebilir; kilitleme yoktur.** Raporları basit güncelleme (PUT) ile değiştirilebilir yapacağım. 

6. **Durum ve risk seçenekleri enum olarak sabittir.** Bu seçenekleri kodda enum ile tanımlayacağım; admin tarafından dinamik olarak yönetilmeyecek. Gerekçe: MVP için fazlasıyla yeterli ve en hızlı çözüm. İleride admin tanımlı hale getirilebilir.

---

## 8. MVP kapsam dışı (bilinen eksikler)

Aşağıdaki özellikleri bilinçli olarak MVP dışında bırakıyorum ve README'nin "bilinen eksikler" bölümünde belirteceğim:

- Tam yetkilendirme (RBAC) — MVP'de rolü basit enum ile tutuyorum.
- Gelişmiş filtreleme ve arama.
- Rapor kilitleme / hafta kapatma mantığı.
- Admin tanımlı durum/risk seçenekleri.
- Audit log, yorum sistemi, bildirimler.

---

