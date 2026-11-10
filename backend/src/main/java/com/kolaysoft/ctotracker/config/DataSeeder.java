package com.kolaysoft.ctotracker.config;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.TrackingItem;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
import com.kolaysoft.ctotracker.repository.TrackingItemRepository;
import com.kolaysoft.ctotracker.repository.UserRepository;
import com.kolaysoft.ctotracker.repository.WeeklyReportRepository;
import com.kolaysoft.ctotracker.repository.WorkItemRepository;

/**
 * Gelistirme/demo icin ornek veri yukler: birkac kullanici, proje, haftalik rapor ve is kalemi.
 * Idempotent: veritabaninda kullanici varsa hicbir sey eklemez (veritabani kalici oldugu icin
 * yalnizca ilk acilista, bos veritabaninda calisir).
 *
 * <p>Yalnizca {@code demo} profilinde calisir. Hicbir profil secilmezse {@code demo} varsayilandir
 * (application.yml, {@code spring.profiles.default}); baska bir profille calistirilan ortamda
 * (or. {@code SPRING_PROFILES_ACTIVE=prod}) ornek veri yuklenmez. Testler varsayilan olarak
 * bos veritabaniyla baslar; ornek veriye dayanan test {@code @ActiveProfiles("demo")} ile ister.
 *
 * <p>Veriler kurgusaldir (proje, musteri ve kisi adlari uydurma, e-postalar {@code example.com}); repository
 * herkese acik oldugu icin gercek sirket/musteri verisi kullanilmaz.
 * Testler ve README demo senaryosu kayit id'lerine dayandigi icin yeni kayitlar yalnizca SONA eklenir.
 */
@Component
@Profile("demo")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /**
     * Tum demo kullanicilarin parolasi (README'de yazilidir). Yalnizca demo verisi icindir; demo profili
     * disinda bu sinif hic calismaz.
     */
    public static final String DEMO_PASSWORD = "Demo1234!";

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final WorkItemRepository workItemRepository;
    private final TrackingItemRepository trackingItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DataSeeder(UserRepository userRepository, ProjectRepository projectRepository,
                      WeeklyReportRepository weeklyReportRepository,
                      WorkItemRepository workItemRepository, TrackingItemRepository trackingItemRepository,
                      PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.workItemRepository = workItemRepository;
        this.trackingItemRepository = trackingItemRepository;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            assignMissingDemoPasswords();
            seedMissingTrackingItems();
            return;
        }

        User ayse = saveUser("ayse@example.com", "Ayse Yilmaz", Role.PROJECT_MANAGER);
        User mehmet = saveUser("mehmet@example.com", "Mehmet Kaya", Role.PROJECT_MANAGER);
        saveUser("cto@example.com", "Zeynep Demir", Role.CTO);
        saveUser("admin@example.com", "Sistem Admin", Role.ADMIN);

        Project atlas = saveProject("Atlas IK", "Demo Musteri A.S.", "Insan kaynaklari surecleri urunu",
                ProjectStatus.DEVAM_EDIYOR, ayse);
        Project ebelge = saveProject("e-Belge", "Demo Musteri A.S.", "e-Fatura / e-Arsiv cozumleri",
                ProjectStatus.DEVAM_EDIYOR, mehmet);
        // Nova POS'a takip kaydi eklenmez: raporsuz ve kayitsiz proje silinebilir kalir (yetki testi onu siler).
        saveProject("Nova POS", "Demo Musteri A.S.", "Perakende magazalar icin POS ve yazilim cozumu",
                ProjectStatus.PLANLANDI, ayse);

        WeeklyReport atlasW29 = saveReport(atlas, 29, LocalDate.of(2026, 7, 20), ProgressStage.GELISTIRME,
                OverallStatus.YOLUNDA, RiskLevel.ORTA,
                "Rapor formu API'si gelistirildi.", "Dashboard ozet endpoint'i.",
                "Zaman kisiti.", "Genel gidisat iyi.");
        saveReport(atlas, 30, LocalDate.of(2026, 7, 27), ProgressStage.TEST,
                OverallStatus.YOLUNDA, RiskLevel.DUSUK,
                "Testler yazildi.", "Hata duzeltme.", "-", "Test asamasinda.");
        WeeklyReport ebelgeW30 = saveReport(ebelge, 30, LocalDate.of(2026, 7, 27), ProgressStage.ANALIZ,
                OverallStatus.RISKLI, RiskLevel.YUKSEK,
                "Mevzuat analizi.", "Veri modeli.", "Mevzuat belirsizligi.", "Analiz suruyor.");

        saveWorkItem(atlasW29, "Rapor CRUD API'si", "Project/WeeklyReport endpoint'leri",
                "Ayse Yilmaz", WorkItemStatus.TAMAMLANDI);
        saveWorkItem(atlasW29, "Is kalemi modeli", "WorkItem entity ve CRUD",
                "Ayse Yilmaz", WorkItemStatus.DEVAM_EDIYOR);
        saveWorkItem(atlasW29, "Dashboard ozeti", "CTO ozet endpoint'i",
                "Mehmet Kaya", WorkItemStatus.PLANLANDI);
        saveWorkItem(ebelgeW30, "Mevzuat analizi", "e-Fatura kurallari",
                "Mehmet Kaya", WorkItemStatus.DEVAM_EDIYOR);

        // Filtre demo verisi: Atlas IK icin 3. hafta (hafta filtresi ve sayfalama gorunsun) ve gecikme ornekleri.
        // Rapor durumu YOLUNDA olsa da planlanan tarihi rapordan once olup bitmemis is kalemi projeyi
        // gecikmeye dusurur; tarihi gecmis ama tamamlanmis is kalemi gecikme sayilmaz (filtreleme analizi KF-01).
        WeeklyReport atlasW31 = saveReport(atlas, 31, LocalDate.of(2026, 8, 3), ProgressStage.TEST,
                OverallStatus.YOLUNDA, RiskLevel.ORTA,
                "Regresyon testleri tamamlandi.", "Rapor ekrani entegrasyonu.",
                "Entegrasyon gecikmesi.", "Testler bitti, entegrasyon suruyor.");
        saveWorkItem(atlasW31, "Rapor ekrani entegrasyonu", "Rapor formunun API'ye baglanmasi",
                "Ayse Yilmaz", WorkItemStatus.DEVAM_EDIYOR, LocalDate.of(2026, 7, 31), null);
        saveWorkItem(atlasW31, "Test senaryolari", "Regresyon senaryolarinin yazilmasi",
                "Mehmet Kaya", WorkItemStatus.TAMAMLANDI, LocalDate.of(2026, 7, 30), LocalDate.of(2026, 7, 31));

        seedTrackingItems(atlas, ebelge);

        log.info("Ornek veri yuklendi: {} kullanici, {} proje, {} rapor, {} is kalemi, {} takip kaydi",
                userRepository.count(), projectRepository.count(),
                weeklyReportRepository.count(), workItemRepository.count(), trackingItemRepository.count());
    }

    private User saveUser(String email, String fullName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        return userRepository.save(user);
    }

    /**
     * Parola kolonu (V4) eklenmeden once doldurulmus bir demo veritabaninda kullanicilarin parolasi yoktur;
     * giris yapabilmeleri icin demo parolasi atanir. Parolasi olan kullaniciya dokunulmaz.
     */
    private void assignMissingDemoPasswords() {
        List<User> withoutPassword = userRepository.findAll().stream()
                .filter(user -> user.getPasswordHash() == null)
                .toList();
        if (withoutPassword.isEmpty()) {
            return;
        }
        withoutPassword.forEach(user -> user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD)));
        userRepository.saveAll(withoutPassword);
        log.info("Parolasi olmayan {} demo kullanicisina demo parolasi atandi", withoutPassword.size());
    }

    /**
     * Takip kayitlari (V5) eklenmeden once doldurulmus demo veritabanina ornek kayitlari ekler: tablo bossa
     * ve demo projeleri yerindeyse. Kullanici kayitlarin hepsini silerse sonraki acilista ornekler geri gelir;
     * bu yalnizca demo profilinde olur.
     */
    private void seedMissingTrackingItems() {
        if (trackingItemRepository.count() > 0) {
            return;
        }
        List<Project> projects = projectRepository.findAll();
        Project atlas = projects.stream().filter(p -> p.getName().equals("Atlas IK")).findFirst().orElse(null);
        Project ebelge = projects.stream().filter(p -> p.getName().equals("e-Belge")).findFirst().orElse(null);
        if (atlas == null || ebelge == null) {
            return;
        }
        seedTrackingItems(atlas, ebelge);
        log.info("Demo projelerine {} takip kaydi eklendi", trackingItemRepository.count());
    }

    /**
     * Her turden ve durumdan ornek: acik yuksek risk, takibi gecikmis aksiyon, verilmis karar, devam eden engel,
     * bekleyen karar. Tarihler bugune goredir; boylece demo her gun ayni gorunur (bir kayit hep gecikmis).
     */
    private void seedTrackingItems(Project atlas, Project ebelge) {
        LocalDate today = LocalDate.now(clock);
        saveTrackingItem(atlas, TrackingItemType.RISK, "Entegrasyon API'si gec teslim edilebilir", RiskLevel.YUKSEK,
                "Rapor ekrani entegrasyonu gecikir", "Musteriyle haftalik teslim takvimi yapilacak",
                "Ayse Yilmaz", today.plusDays(7), TrackingItemStatus.ACIK, 12);
        saveTrackingItem(atlas, TrackingItemType.AKSIYON, "Musteriyle kapsam toplantisi", null,
                null, "Rapor ekranlarinin kapsami netlestirilecek",
                "Ayse Yilmaz", today.minusDays(2), TrackingItemStatus.ACIK, 9);
        saveTrackingItem(atlas, TrackingItemType.KARAR, "Raporlama kutuphanesi secimi", null,
                "Disa aktarma ozelligini belirler", "Iki kutuphane karsilastirildi, hafif olan secildi",
                "Zeynep Demir", null, TrackingItemStatus.KAPANDI, 20);
        saveTrackingItem(ebelge, TrackingItemType.ENGEL, "Test ortamina erisim yok", RiskLevel.ORTA,
                "Entegrasyon testleri baslayamiyor", "Musteri BT ekibinden VPN erisimi istendi",
                "Mehmet Kaya", today.plusDays(3), TrackingItemStatus.DEVAM_EDIYOR, 5);
        saveTrackingItem(ebelge, TrackingItemType.KARAR, "e-Arsiv saglayicisi secimi", null,
                "Maliyet ve teslim tarihini etkiler", "Iki teklif CTO'ya sunulacak",
                "Zeynep Demir", today.plusDays(10), TrackingItemStatus.ACIK, 3);
    }

    private void saveTrackingItem(Project project, TrackingItemType type, String title, RiskLevel riskLevel,
                                  String impact, String actionPlan, String assignee, LocalDate followUpDate,
                                  TrackingItemStatus status, int ageInDays) {
        TrackingItem item = new TrackingItem();
        item.setProject(project);
        item.setType(type);
        item.setTitle(title);
        item.setRiskLevel(riskLevel);
        item.setImpact(impact);
        item.setActionPlan(actionPlan);
        item.setAssignee(assignee);
        item.setFollowUpDate(followUpDate);
        item.setStatus(status);
        item.setCreatedAt(Instant.now(clock).minus(ageInDays, ChronoUnit.DAYS));
        if (!status.isOpen()) {
            item.setClosedDate(LocalDate.now(clock).minusDays(ageInDays / 2));
        }
        trackingItemRepository.save(item);
    }

    private Project saveProject(String name, String customer, String description,
                                ProjectStatus status, User owner) {
        Project project = new Project();
        project.setName(name);
        project.setCustomer(customer);
        project.setDescription(description);
        project.setStatus(status);
        project.setOwner(owner);
        return projectRepository.save(project);
    }

    private WeeklyReport saveReport(Project project, int weekNumber, LocalDate reportDate, ProgressStage stage,
                                    OverallStatus overallStatus, RiskLevel riskLevel,
                                    String done, String planned, String risks, String note) {
        WeeklyReport report = new WeeklyReport();
        report.setProject(project);
        report.setWeekNumber(weekNumber);
        report.setReportDate(reportDate);
        report.setProgressStage(stage);
        report.setOverallStatus(overallStatus);
        report.setRiskLevel(riskLevel);
        report.setDone(done);
        report.setPlanned(planned);
        report.setRisks(risks);
        report.setNote(note);
        return weeklyReportRepository.save(report);
    }

    private void saveWorkItem(WeeklyReport report, String title, String description,
                              String assignee, WorkItemStatus status) {
        saveWorkItem(report, title, description, assignee, status, null, null);
    }

    private void saveWorkItem(WeeklyReport report, String title, String description, String assignee,
                              WorkItemStatus status, LocalDate plannedDate, LocalDate completedDate) {
        WorkItem workItem = new WorkItem();
        workItem.setWeeklyReport(report);
        workItem.setTitle(title);
        workItem.setDescription(description);
        workItem.setAssignee(assignee);
        workItem.setStatus(status);
        workItem.setPlannedDate(plannedDate);
        workItem.setCompletedDate(completedDate);
        workItemRepository.save(workItem);
    }
}
