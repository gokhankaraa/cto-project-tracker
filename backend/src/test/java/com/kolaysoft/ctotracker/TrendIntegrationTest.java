package com.kolaysoft.ctotracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.IsoFields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.config.ClockConfig;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.TrackingItem;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;
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
 * Haftalik trend (filtre sozlesmesi, bolum 2a) HTTP uzerinden, gercek PostgreSQL ile. Seed verisine dayanmaz: test
 * sorumlusunun portfoyu kurulur ve sonuclar ona ({@code ownerId}) gore suzulur. Tarihler sabittir (2026 hafta 8-12).
 *
 * <pre>
 * Alfa : h8 YOLUNDA DUSUK ANALIZ, h10 RISKLI YUKSEK GELISTIRME, h12 YOLUNDA ORTA TEST + gecikmis is kalemi
 * Beta : h11 GECIKMELI ORTA ANALIZ
 * Gamma: rapor yok
 * Alfa kayitlari: engel (h9'da acildi, h11'de kapandi), risk (h10'da acildi),
 *                 karar (UTC'ye gore pazar 21:30 = Turkiye'de pazartesi 00:30, yani h12'de acildi)
 * </pre>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@WithMockUser(roles = "ADMIN")
class TrendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private TrackingItemRepository trackingItemRepository;

    private long ownerId;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("trend-testi@example.com");
        owner.setFullName("Trend Testi");
        owner.setRole(Role.PROJECT_MANAGER);
        ownerId = userRepository.save(owner).getId();

        Project alfa = project("Alfa", owner);
        Project beta = project("Beta", owner);
        project("Gamma", owner);

        // Rapor, anlattigi haftanin ertesi pazartesi yazilir.
        report(alfa, 8, OverallStatus.YOLUNDA, RiskLevel.DUSUK, ProgressStage.ANALIZ);
        report(alfa, 10, OverallStatus.RISKLI, RiskLevel.YUKSEK, ProgressStage.GELISTIRME);
        WeeklyReport alfaW12 = report(alfa, 12, OverallStatus.YOLUNDA, RiskLevel.ORTA, ProgressStage.TEST);
        report(beta, 11, OverallStatus.GECIKMELI, RiskLevel.ORTA, ProgressStage.ANALIZ);

        // Planlanan tarihi raporun tarihinden once, bitmemis: Alfa h12'de gecikmede (KF-01).
        WorkItem late = new WorkItem();
        late.setWeeklyReport(alfaW12);
        late.setTitle("Gec kalan entegrasyon");
        late.setStatus(WorkItemStatus.DEVAM_EDIYOR);
        late.setPlannedDate(LocalDate.of(2026, 3, 20));
        workItemRepository.save(late);

        tracking(alfa, TrackingItemType.ENGEL, RiskLevel.ORTA, Instant.parse("2026-02-25T09:00:00Z"),
                LocalDate.of(2026, 3, 11));
        tracking(alfa, TrackingItemType.RISK, RiskLevel.YUKSEK, Instant.parse("2026-03-03T09:00:00Z"), null);
        tracking(alfa, TrackingItemType.KARAR, null, Instant.parse("2026-03-15T21:30:00Z"), null);
    }

    private Project project(String name, User owner) {
        Project p = new Project();
        p.setName(name);
        p.setStatus(ProjectStatus.DEVAM_EDIYOR);
        p.setOwner(owner);
        return projectRepository.save(p);
    }

    private WeeklyReport report(Project project, int week, OverallStatus status, RiskLevel risk, ProgressStage stage) {
        WeeklyReport r = new WeeklyReport();
        r.setProject(project);
        r.setWeekNumber(week);
        r.setReportDate(WeeklyReport.mondayOf(2026, week).plusWeeks(1));
        r.setProgressStage(stage);
        r.setOverallStatus(status);
        r.setRiskLevel(risk);
        return weeklyReportRepository.save(r);
    }

    private void tracking(Project project, TrackingItemType type, RiskLevel level, Instant createdAt, LocalDate closed) {
        TrackingItem item = new TrackingItem();
        item.setProject(project);
        item.setType(type);
        item.setTitle(type.name());
        item.setRiskLevel(level);
        item.setAssignee("Trend Testi");
        item.setFollowUpDate(LocalDate.of(2026, 4, 30));
        item.setStatus(closed == null ? TrackingItemStatus.ACIK : TrackingItemStatus.KAPANDI);
        item.setClosedDate(closed);
        item.setCreatedAt(createdAt);
        trackingItemRepository.save(item);
    }

    private ResultActions trend(String query) throws Exception {
        return mockMvc.perform(get("/api/dashboard/trend?ownerId=" + ownerId + "&" + query))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Her hafta o haftaya kadarki en son raporla: durum dagilimi, yuksek risk, gecikme, ortalama ilerleme")
    void reportSeries() throws Exception {
        trend("year=2026&weekNumber=12&weeks=5")
                .andExpect(jsonPath("$.weeks.length()").value(5))
                .andExpect(jsonPath("$.weeks[0].weekNumber").value(8))
                .andExpect(jsonPath("$.weeks[0].weekStart").value("2026-02-16"))
                .andExpect(jsonPath("$.weeks[0].totalProjects").value(3))
                .andExpect(jsonPath("$.weeks[0].projectsWithReport").value(1))
                .andExpect(jsonPath("$.weeks[0].projectsByStatus.YOLUNDA").value(1))
                .andExpect(jsonPath("$.weeks[0].projectsByStatus.BLOKE").value(0))
                .andExpect(jsonPath("$.weeks[0].averageProgress").value(25))
                // h9: Alfa'nin h8 raporu gecerli kalir.
                .andExpect(jsonPath("$.weeks[1].projectsByStatus.YOLUNDA").value(1))
                // h10: Alfa RISKLI + YUKSEK
                .andExpect(jsonPath("$.weeks[2].projectsByStatus.RISKLI").value(1))
                .andExpect(jsonPath("$.weeks[2].highRiskProjects").value(1))
                // h11: Beta katilir (GECIKMELI = gecikmede); ortalama (50 + 25) / 2 = 37.5 -> 38
                .andExpect(jsonPath("$.weeks[3].projectsWithReport").value(2))
                .andExpect(jsonPath("$.weeks[3].projectsByStatus.GECIKMELI").value(1))
                .andExpect(jsonPath("$.weeks[3].delayedProjects").value(1))
                .andExpect(jsonPath("$.weeks[3].averageProgress").value(38))
                // h12: Alfa YOLUNDA ama gecikmis is kalemi var; Beta h11 ile devam eder
                .andExpect(jsonPath("$.weeks[4].projectsByStatus.YOLUNDA").value(1))
                .andExpect(jsonPath("$.weeks[4].highRiskProjects").value(0))
                .andExpect(jsonPath("$.weeks[4].delayedProjects").value(2))
                .andExpect(jsonPath("$.weeks[4].averageProgress").value(50));
    }

    @Test
    @DisplayName("Hafta sonunda acik kayitlar olusturulma ve kapanis tarihinden; hafta Turkiye saatiyle baslar")
    void trackingSeries() throws Exception {
        trend("year=2026&weekNumber=12&weeks=5")
                .andExpect(jsonPath("$.weeks[0].openBlockers").value(0))
                .andExpect(jsonPath("$.weeks[1].openBlockers").value(1))
                .andExpect(jsonPath("$.weeks[1].openedTrackingItems").value(1))
                .andExpect(jsonPath("$.weeks[2].openBlockers").value(1))
                .andExpect(jsonPath("$.weeks[2].openRisks").value(1))
                // h11: engel kapandi
                .andExpect(jsonPath("$.weeks[3].openBlockers").value(0))
                .andExpect(jsonPath("$.weeks[3].closedTrackingItems").value(1))
                // Karar UTC'ye gore h11'in pazari, Turkiye'de h12'nin pazartesisi: h11'de yok, h12'de var.
                .andExpect(jsonPath("$.weeks[3].pendingDecisions").value(0))
                .andExpect(jsonPath("$.weeks[4].pendingDecisions").value(1))
                .andExpect(jsonPath("$.weeks[4].openedTrackingItems").value(1))
                .andExpect(jsonPath("$.weeks[4].openRisks").value(1))
                .andExpect(jsonPath("$.weeks[4].openActions").value(0));
    }

    @Test
    @DisplayName("Son hafta, ayni referansla acilan dashboard sayaclariyla ayni")
    void lastWeekMatchesTheDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary?ownerId=" + ownerId + "&year=2026&weekNumber=12"))
                .andExpect(jsonPath("$.totalProjects").value(3))
                .andExpect(jsonPath("$.projectsWithoutReport").value(1))
                .andExpect(jsonPath("$.highRiskProjects").value(0))
                .andExpect(jsonPath("$.delayedProjects").value(2));
        trend("year=2026&weekNumber=12&weeks=1")
                .andExpect(jsonPath("$.weeks[0].totalProjects").value(3))
                .andExpect(jsonPath("$.weeks[0].projectsWithReport").value(2))
                .andExpect(jsonPath("$.weeks[0].highRiskProjects").value(0))
                .andExpect(jsonPath("$.weeks[0].delayedProjects").value(2));
    }

    @Test
    @DisplayName("Pencere yil sinirini gecer; yalnizca yil verilirse o yilin son haftasi (2026: 53)")
    void windowAcrossYears() throws Exception {
        trend("year=2026&weekNumber=2&weeks=4")
                .andExpect(jsonPath("$.weeks[*].weekNumber").value(org.hamcrest.Matchers.contains(51, 52, 1, 2)))
                .andExpect(jsonPath("$.weeks[0].year").value(2025))
                .andExpect(jsonPath("$.weeks[0].weekStart").value("2025-12-15"))
                .andExpect(jsonPath("$.weeks[2].year").value(2026))
                .andExpect(jsonPath("$.weeks[2].weekStart").value("2025-12-29"));
        trend("year=2026&weeks=1")
                .andExpect(jsonPath("$.weeks[0].weekNumber").value(53))
                .andExpect(jsonPath("$.weeks[0].weekStart").value("2026-12-28"));
    }

    @Test
    @DisplayName("Referans yoksa pencere bu haftada biter; varsayilan 12 hafta")
    void defaultWindowEndsThisWeek() throws Exception {
        LocalDate today = LocalDate.now(ClockConfig.ZONE);
        trend("")
                .andExpect(jsonPath("$.weeks.length()").value(12))
                .andExpect(jsonPath("$.weeks[11].year").value(today.get(IsoFields.WEEK_BASED_YEAR)))
                .andExpect(jsonPath("$.weeks[11].weekNumber").value(today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)));
    }

    @Test
    @DisplayName("Filtreye uyan proje yoksa haftalar yine doner, sayilar sifir")
    void emptyPortfolio() throws Exception {
        mockMvc.perform(get("/api/dashboard/trend?ownerId=999999&year=2026&weekNumber=12&weeks=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks.length()").value(2))
                .andExpect(jsonPath("$.weeks[1].totalProjects").value(0))
                .andExpect(jsonPath("$.weeks[1].averageProgress").doesNotExist())
                .andExpect(jsonPath("$.weeks[1].projectsByStatus.YOLUNDA").value(0));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource({
            "weeks=0,                  weeks",
            "weeks=53,                 weeks",
            "weekNumber=12,            weekNumber",
            "year=2026&weekNumber=54,  weekNumber",
            "year=1999,                year"
    })
    @DisplayName("Hatali parametre 400 VALIDATION_ERROR ile alan adini doner")
    void invalidParameters(String query, String field) throws Exception {
        mockMvc.perform(get("/api/dashboard/trend?" + query))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
    }
}
