package com.kolaysoft.ctotracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MVP uctan uca entegrasyon testleri.
 *
 * <p>Gercek uygulamayi ayaga kaldirip HTTP katmanindan (MockMvc) test eder: proje -> rapor ->
 * is kalemi -> dashboard akisi ve kritik negatif senaryolar. Her test {@code @Transactional}
 * ile izole edilir; olusturulan veriler test sonunda geri alinir (seed verisi korunur).
 *
 * <p>Seed verisine dayanir (DataSeeder), bu yuzden {@code demo} profilini acikca ister:
 * kullanici id 1 = Ayse (PM), proje id 1 = Atlas IK (hafta 29, 30 ve 31 raporlu),
 * proje id 3 = Nova POS (raporsuz).
 */
@SpringBootTest
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// Butun istekler giris ister; bu testler yetki degil is kurallarini sinar, Admin olarak calisir.
@WithMockUser(roles = "ADMIN")
class MvpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private long idOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("Happy path: raporsuz projeye rapor + is kalemi eklenir, dashboard'a yansir")
    void endToEndHappyPath() throws Exception {
        // Proje 3 (Nova POS) baslangicta raporsuz -> dashboard'da hasReport=false
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[?(@.projectId==3)].hasReport").value(org.hamcrest.Matchers.hasItem(false)));

        // Rapora ekle (hafta 31, ANALIZ; rapor ertesi pazartesi yazilir)
        String reportBody = """
                { "weekNumber": 31, "reportDate": "2026-08-03", "progressStage": "ANALIZ",
                  "overallStatus": "YOLUNDA", "riskLevel": "DUSUK" }
                """;
        MvcResult reportRes = mockMvc.perform(post("/api/projects/3/reports")
                        .contentType(MediaType.APPLICATION_JSON).content(reportBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progressPercentage").value(25))
                .andExpect(jsonPath("$.liveTaskCount").value(0))
                .andReturn();
        long reportId = idOf(reportRes);

        // Is kalemi ekle (DEVAM_EDIYOR)
        String itemBody = """
                { "title": "Analiz gorusmesi", "status": "DEVAM_EDIYOR", "assignee": "Ayse" }
                """;
        mockMvc.perform(post("/api/reports/" + reportId + "/work-items")
                        .contentType(MediaType.APPLICATION_JSON).content(itemBody))
                .andExpect(status().isCreated());

        // Rapor artik 1 canli task gostermeli
        mockMvc.perform(get("/api/reports/" + reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liveTaskCount").value(1));

        // Dashboard'da proje 3 artik raporlu ve dogru kademe
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[?(@.projectId==3)].hasReport").value(org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$.projects[?(@.projectId==3)].progressPercentage").value(org.hamcrest.Matchers.hasItem(25)));
    }

    @Test
    @DisplayName("Negatif: olmayan proje 404")
    void missingProjectReturns404() throws Exception {
        mockMvc.perform(get("/api/projects/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Validasyon: zorunlu alanlar eksik proje 400")
    void invalidProjectReturns400() throws Exception {
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    @DisplayName("Validasyon: cok uzun proje adi 400")
    void tooLongNameReturns400() throws Exception {
        String longName = "P".repeat(300);
        String body = """
                { "name": "%s", "status": "PLANLANDI", "ownerId": 1 }
                """.formatted(longName);
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Is kurali: ayni proje+hafta ikinci rapor 409")
    void duplicateReportReturns409() throws Exception {
        // Proje 1 (Atlas IK) hafta 29 seed'de mevcut
        String body = """
                { "weekNumber": 29, "reportDate": "2026-07-20", "progressStage": "ANALIZ",
                  "overallStatus": "YOLUNDA", "riskLevel": "DUSUK" }
                """;
        mockMvc.perform(post("/api/projects/1/reports")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value("Bu proje için 2026 yılının 29. haftasına ait rapor zaten mevcut."));
    }

    @Test
    @DisplayName("Is kurali: ayni hafta numarasi farkli yilda yeni rapor olarak kabul edilir (V2)")
    void sameWeekInAnotherYearIsAllowed() throws Exception {
        // Proje 1 (Atlas IK) 2026 hafta 29 seed'de mevcut; 2027 hafta 29 ayri bir rapordur
        String body = """
                { "weekNumber": 29, "reportDate": "2027-07-19", "progressStage": "ANALIZ",
                  "overallStatus": "YOLUNDA", "riskLevel": "DUSUK" }
                """;
        mockMvc.perform(post("/api/projects/1/reports")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.year").value(2027))
                .andExpect(jsonPath("$.weekNumber").value(29));

        // Dashboard'da en son rapor artik 2027 hafta 29 (2026 hafta 31'den daha yeni)
        mockMvc.perform(get("/api/dashboard/summary").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[0].lastYear").value(2027))
                .andExpect(jsonPath("$.projects[0].lastWeekNumber").value(29));
    }

    @Test
    @DisplayName("Validasyon: rapor tarihi anlattigi haftaya uymuyorsa 400, hata reportDate alaninda")
    void reportDateOutsideWeekReturns400() throws Exception {
        // 29. hafta 13-19 Temmuz; rapor en gec 26 Temmuz yazilabilir.
        String body = """
                { "weekNumber": 29, "reportDate": "2026-08-03", "progressStage": "ANALIZ",
                  "overallStatus": "YOLUNDA", "riskLevel": "DUSUK" }
                """;
        mockMvc.perform(post("/api/projects/3/reports")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reportDate"));
    }

    @Test
    @DisplayName("Is kurali: kademe atlama 400")
    void stageSkipReturns400() throws Exception {
        // Yeni rapor olustur (ANALIZ), sonra TAMAMLANDI'ya atlamayi dene
        String reportBody = """
                { "weekNumber": 32, "reportDate": "2026-08-03", "progressStage": "ANALIZ",
                  "overallStatus": "YOLUNDA", "riskLevel": "DUSUK" }
                """;
        MvcResult res = mockMvc.perform(post("/api/projects/3/reports")
                        .contentType(MediaType.APPLICATION_JSON).content(reportBody))
                .andExpect(status().isCreated()).andReturn();
        long reportId = idOf(res);

        String skipBody = """
                { "weekNumber": 32, "reportDate": "2026-08-03", "progressStage": "TAMAMLANDI",
                  "overallStatus": "YOLUNDA", "riskLevel": "DUSUK" }
                """;
        mockMvc.perform(put("/api/reports/" + reportId)
                        .contentType(MediaType.APPLICATION_JSON).content(skipBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("Dashboard: gecersiz risk parametresi 400")
    void dashboardInvalidRiskReturns400() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").param("riskLevel", "COK_YUKSEK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Dashboard: risk filtresi yalnizca eslesen projeleri doner")
    void dashboardRiskFilterWorks() throws Exception {
        // Seed: e-Belge (proje 2) son raporu YUKSEK risk
        mockMvc.perform(get("/api/dashboard/summary").param("riskLevel", "YUKSEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[*].riskLevel")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("YUKSEK"))));
    }
}
