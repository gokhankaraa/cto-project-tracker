package com.kolaysoft.ctotracker;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

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
 * T13 filtre endpoint'lerinin HTTP katmanindan uctan uca testi (filtre sozlesmesi bolum 2-5).
 *
 * <p>Seed verisine dayanmaz ({@code demo} profili acik degil, veritabani bos baslar): her test kendi
 * sorumlusunu, projelerini ve raporlarini olusturur ve sonuclari o sorumluya ({@code ownerId}) gore suzer;
 * boylece baska testlerin kayitlari da sonuclara karismaz. Veriler {@code @Transactional} ile test
 * sonunda geri alinir.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FilterIntegrationTest {

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

    private long ownerId;
    private long gecikenProjeId;

    /**
     * Test sorumlusunun portfoyu:
     * "Geciken Proje" (hafta 20 ORTA, hafta 21 ORTA + gecikmis is kalemi),
     * "Riskli Proje" (hafta 21 YUKSEK), "Raporsuz Proje" (raporu yok).
     */
    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("filtre-testi@example.com");
        owner.setFullName("Filtre Testi");
        owner.setRole(Role.PROJECT_MANAGER);
        ownerId = userRepository.save(owner).getId();

        Project geciken = project("Geciken Proje", owner);
        Project riskli = project("Riskli Proje", owner);
        project("Raporsuz Proje", owner);
        gecikenProjeId = geciken.getId();

        report(geciken, 20, LocalDate.of(2026, 5, 11), RiskLevel.ORTA);
        WeeklyReport gecikenW21 = report(geciken, 21, LocalDate.of(2026, 5, 18), RiskLevel.ORTA);
        report(riskli, 21, LocalDate.of(2026, 5, 18), RiskLevel.YUKSEK);

        item(gecikenW21, "Gec kalan entegrasyon", "Filtre Testi", WorkItemStatus.DEVAM_EDIYOR, LocalDate.of(2026, 5, 15));
        item(gecikenW21, "Biten analiz", "Baska Kisi", WorkItemStatus.TAMAMLANDI, LocalDate.of(2026, 5, 12));
        item(gecikenW21, "Tarihsiz is", "Filtre Testi", WorkItemStatus.PLANLANDI, null);
    }

    private Project project(String name, User owner) {
        Project p = new Project();
        p.setName(name);
        p.setCustomer("Demo Musteri A.S.");
        p.setStatus(ProjectStatus.DEVAM_EDIYOR);
        p.setOwner(owner);
        return projectRepository.save(p);
    }

    private WeeklyReport report(Project project, int week, LocalDate date, RiskLevel risk) {
        WeeklyReport r = new WeeklyReport();
        r.setProject(project);
        r.setWeekNumber(week);
        r.setReportDate(date);
        r.setProgressStage(ProgressStage.GELISTIRME);
        r.setOverallStatus(OverallStatus.YOLUNDA);
        r.setRiskLevel(risk);
        return weeklyReportRepository.save(r);
    }

    private void item(WeeklyReport report, String title, String assignee, WorkItemStatus status, LocalDate planned) {
        WorkItem w = new WorkItem();
        w.setWeeklyReport(report);
        w.setTitle(title);
        w.setAssignee(assignee);
        w.setStatus(status);
        w.setPlannedDate(planned);
        workItemRepository.save(w);
    }

    private ResultActions getOk(String url) throws Exception {
        return mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    // --- Dashboard (sozlesme bolum 2) ---

    @Test
    @DisplayName("Dashboard: sorumlu filtresi kendi projelerini, sayaclari bu kumeden hesaplar")
    void dashboardOwnerFilterAndCounters() throws Exception {
        getOk("/api/dashboard/summary?ownerId=" + ownerId)
                .andExpect(jsonPath("$.totalProjects").value(3))
                .andExpect(jsonPath("$.highRiskProjects").value(1))
                .andExpect(jsonPath("$.projectsWithoutReport").value(1))
                .andExpect(jsonPath("$.delayedProjects").value(1))
                .andExpect(jsonPath("$.projects[*].name").value(contains("Geciken Proje", "Riskli Proje", "Raporsuz Proje")));
    }

    @Test
    @DisplayName("Dashboard: rapor filtreleri raporsuz projeyi eler; gecikme true/false ayrimi")
    void dashboardReportFilters() throws Exception {
        getOk("/api/dashboard/summary?ownerId=" + ownerId + "&riskLevel=ORTA,YUKSEK")
                .andExpect(jsonPath("$.projects[*].name").value(contains("Geciken Proje", "Riskli Proje")));
        getOk("/api/dashboard/summary?ownerId=" + ownerId + "&delayed=true")
                .andExpect(jsonPath("$.projects[*].name").value(contains("Geciken Proje")));
        getOk("/api/dashboard/summary?ownerId=" + ownerId + "&delayed=false")
                .andExpect(jsonPath("$.projects[*].name").value(contains("Riskli Proje")));
    }

    @Test
    @DisplayName("Dashboard: referans yil + hafta gecmis haftayi kendi raporuna gore gosterir")
    void dashboardWeekReference() throws Exception {
        getOk("/api/dashboard/summary?ownerId=" + ownerId + "&year=2026&weekNumber=20")
                .andExpect(jsonPath("$.projectsWithoutReport").value(2))
                .andExpect(jsonPath("$.delayedProjects").value(0))
                .andExpect(jsonPath("$.projects[0].lastWeekNumber").value(20))
                .andExpect(jsonPath("$.projects[0].delayed").value(false));
        // Yalnizca yil: 2025'in sonuna kadar hicbir projenin raporu yok
        getOk("/api/dashboard/summary?ownerId=" + ownerId + "&year=2025")
                .andExpect(jsonPath("$.projectsWithoutReport").value(3));
        // Referans sonraki yilda: onceki yilin en son raporu (2026 h21) esas alinir
        getOk("/api/dashboard/summary?ownerId=" + ownerId + "&year=2027&weekNumber=1")
                .andExpect(jsonPath("$.projects[0].lastYear").value(2026))
                .andExpect(jsonPath("$.projects[0].lastWeekNumber").value(21));
    }

    // --- Rapor listesi (sozlesme bolum 3) ---

    @Test
    @DisplayName("Rapor listesi: filtre, sayfalama ve siralama; yanit sayfa yapisinda ve projectName iceriyor")
    void reportListPagingAndSorting() throws Exception {
        getOk("/api/reports?ownerId=" + ownerId + "&sort=weekNumber,asc&size=2")
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content[0].weekNumber").value(20))
                .andExpect(jsonPath("$.content[0].projectName").value("Geciken Proje"));
        getOk("/api/reports?ownerId=" + ownerId + "&year=2026&weekFrom=21&weekTo=21&riskLevel=YUKSEK")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].projectName").value("Riskli Proje"));
        getOk("/api/reports?ownerId=" + ownerId + "&year=2027")
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // --- Is kalemi listesi (sozlesme bolum 4) ---

    @Test
    @DisplayName("Is kalemi listesi: sorumlu + gecikme filtresi; yanitta proje, hafta ve gecikme bilgisi")
    void workItemListFilters() throws Exception {
        getOk("/api/work-items?projectId=" + gecikenProjeId + "&assignee=filtre&delayed=true")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Gec kalan entegrasyon"))
                .andExpect(jsonPath("$.content[0].projectName").value("Geciken Proje"))
                .andExpect(jsonPath("$.content[0].weekNumber").value(21))
                .andExpect(jsonPath("$.content[0].delayed").value(true));
    }

    @Test
    @DisplayName("Is kalemi listesi: varsayilan siralama planlanan tarihe gore, tarihsiz kayit sonda")
    void workItemDefaultSortPutsNullsLast() throws Exception {
        getOk("/api/work-items?projectId=" + gecikenProjeId)
                .andExpect(jsonPath("$.content[*].title")
                        .value(contains("Biten analiz", "Gec kalan entegrasyon", "Tarihsiz is")));
    }

    // --- Bos sonuc ve hata davranisi (sozlesme bolum 5) ---

    @Test
    @DisplayName("Sonuc yoksa 200 ve bos liste doner, 404 donmez")
    void emptyResultsReturn200() throws Exception {
        getOk("/api/dashboard/summary?projectId=999999")
                .andExpect(jsonPath("$.totalProjects").value(0))
                .andExpect(jsonPath("$.projects").isEmpty());
        getOk("/api/reports?ownerId=999999")
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
        getOk("/api/reports?page=50")
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource({
            "/api/dashboard/summary?riskLevel=COK_YUKSEK,           INVALID_REQUEST,",
            "/api/dashboard/summary?riskLevel=ORTA&riskLevel=BITTI, INVALID_REQUEST,",
            "/api/dashboard/summary?delayed=evet,                   INVALID_REQUEST,",
            "/api/dashboard/summary?weekNumber=54,                  VALIDATION_ERROR, weekNumber",
            "/api/dashboard/summary?weekNumber=29,                  VALIDATION_ERROR, weekNumber",
            "/api/dashboard/summary?year=1999,                      VALIDATION_ERROR, year",
            "/api/dashboard/summary?year=yirmi,                     INVALID_REQUEST,",
            "/api/reports?projectId=abc,                            INVALID_REQUEST,",
            "/api/reports?dateFrom=27.07.2026,                      INVALID_REQUEST,",
            "/api/reports?year=2026&weekFrom=30&weekTo=29,          VALIDATION_ERROR, weekTo",
            "/api/reports?weekFrom=29,                              VALIDATION_ERROR, weekFrom",
            "/api/reports?weekTo=30,                                VALIDATION_ERROR, weekTo",
            "/api/reports?year=2101,                                VALIDATION_ERROR, year",
            "/api/reports?dateFrom=2026-07-31&dateTo=2026-07-01,    VALIDATION_ERROR, dateTo",
            "/api/reports?weekTo=0,                                 VALIDATION_ERROR, weekTo",
            "/api/reports?size=0,                                   VALIDATION_ERROR, size",
            "/api/reports?size=500,                                 VALIDATION_ERROR, size",
            "/api/reports?page=-1,                                  VALIDATION_ERROR, page",
            "/api/reports?sort=password,                            VALIDATION_ERROR, sort",
            "'/api/reports?sort=reportDate,yukari',                 VALIDATION_ERROR, sort",
            "/api/work-items?status=BITTI,                          INVALID_REQUEST,",
            "/api/work-items?weekNumber=54,                         VALIDATION_ERROR, weekNumber",
            "/api/work-items?weekNumber=29,                         VALIDATION_ERROR, weekNumber",
            "/api/work-items?sort=assignee,                         VALIDATION_ERROR, sort"
    })
    @DisplayName("Hatali istekler sozlesmedeki kodla 400 doner; hicbiri 500 degil")
    void invalidRequestsReturn400(String url, String code, String field) throws Exception {
        ResultActions result = mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code));
        if (field == null) {
            result.andExpect(jsonPath("$.fieldErrors").doesNotExist());
        } else {
            result.andExpect(jsonPath("$.fieldErrors[0].field").value(field));
        }
    }

    @Test
    @DisplayName("255 karakterden uzun sorumlu aramasi 400 VALIDATION_ERROR")
    void tooLongAssigneeIsRejected() throws Exception {
        mockMvc.perform(get("/api/work-items").param("assignee", "a".repeat(256)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assignee"));
    }

    @Test
    @DisplayName("Gecersiz siralama mesaji izin verilen alanlari listeler")
    void invalidSortMessageListsAllowedFields() throws Exception {
        mockMvc.perform(get("/api/work-items?sort=assignee"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(containsString("plannedDate, projectName, title")));
    }
}
