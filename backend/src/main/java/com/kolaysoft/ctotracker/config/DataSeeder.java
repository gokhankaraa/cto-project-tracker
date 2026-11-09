package com.kolaysoft.ctotracker.config;

import java.time.LocalDate;
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
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
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
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, ProjectRepository projectRepository,
                      WeeklyReportRepository weeklyReportRepository,
                      WorkItemRepository workItemRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.workItemRepository = workItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            assignMissingDemoPasswords();
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

        log.info("Ornek veri yuklendi: {} kullanici, {} proje, {} rapor, {} is kalemi",
                userRepository.count(), projectRepository.count(),
                weeklyReportRepository.count(), workItemRepository.count());
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
