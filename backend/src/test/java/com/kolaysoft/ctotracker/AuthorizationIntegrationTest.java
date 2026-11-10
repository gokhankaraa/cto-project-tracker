package com.kolaysoft.ctotracker;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.kolaysoft.ctotracker.config.ClockConfig;
import com.kolaysoft.ctotracker.config.DataSeeder;

/**
 * Yetki matrisi uctan uca: demo verisindeki kullanicilar gercekten giris yapar, istekler gercek token'la gider.
 *
 * <pre>
 * ayse   (PM)  : Atlas IK (1), Nova POS (3)      mehmet (PM): e-Belge (2)
 * cto    (CTO) : her seyi okur, hicbir sey yazamaz
 * admin        : her sey
 * </pre>
 *
 * Seed raporlari: 1-2-4 Atlas IK (hafta 29-30-31), 3 e-Belge (hafta 30). Her test {@code @Transactional} ile
 * geri alinir.
 */
@SpringBootTest
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class AuthorizationIntegrationTest {

    private static final String NOVA_REPORT = """
            { "weekNumber": 31, "reportDate": "2026-08-03", "progressStage": "ANALIZ",
              "overallStatus": "YOLUNDA", "riskLevel": "DUSUK" }
            """;
    private static final String ATLAS_W31_UPDATE = """
            { "weekNumber": 31, "reportDate": "2026-08-03", "progressStage": "TEST",
              "overallStatus": "RISKLI", "riskLevel": "ORTA" }
            """;
    private static final String WORK_ITEM = """
            { "title": "Yetki testi", "status": "PLANLANDI" }
            """;
    private static final String PROJECT = """
            { "name": "Yeni Proje", "status": "PLANLANDI", "ownerId": 2 }
            """;
    private static final String TRACKING_ITEM = """
            { "type": "AKSIYON", "title": "Yetki testi", "assignee": "Ayse", "followUpDate": "%s", "status": "ACIK" }
            """.formatted(LocalDate.now(ClockConfig.ZONE).plusDays(3));

    @Autowired
    private MockMvc mockMvc;

    private final Map<String, String> tokens = new HashMap<>();

    private String token(String user) throws Exception {
        if (!tokens.containsKey(user)) {
            String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "%s@example.com", "password": "%s" }
                                    """.formatted(user, DataSeeder.DEMO_PASSWORD)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            tokens.put(user, JsonPath.read(body, "$.token"));
        }
        return tokens.get(user);
    }

    private ResultActions as(String user, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(user)));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    /** e-Belge (Mehmet) raporundaki ilk is kaleminin id'si. */
    private int ebelgeWorkItemId() throws Exception {
        String body = as("admin", get("/api/reports/3/work-items")).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(body, "$[*].id");
        return ids.getFirst();
    }

    /** Projenin ilk takip kaydinin id'si (seed: Atlas IK = proje 1, e-Belge = proje 2). */
    private int trackingItemId(int projectId) throws Exception {
        String body = as("admin", get("/api/projects/" + projectId + "/tracking-items"))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(body, "$[*].id");
        return ids.getFirst();
    }

    // --- Okuma: gorunurluk ---

    @Test
    @DisplayName("Proje listesi: Admin ve CTO hepsini, proje yoneticisi yalnizca kendi projelerini gorur")
    void projectListIsScopedToOwner() throws Exception {
        as("admin", get("/api/projects")).andExpect(jsonPath("$.length()").value(3));
        as("cto", get("/api/projects")).andExpect(jsonPath("$.length()").value(3));
        as("ayse", get("/api/projects"))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Atlas IK", "Nova POS")));
        as("mehmet", get("/api/projects"))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("e-Belge")));
    }

    @Test
    @DisplayName("Dashboard: proje yoneticisi kendi projelerini gorur, sayaclar da onlardan hesaplanir")
    void dashboardIsScopedAndCountersFollow() throws Exception {
        as("cto", get("/api/dashboard/summary")).andExpect(jsonPath("$.totalProjects").value(3));
        as("ayse", get("/api/dashboard/summary"))
                .andExpect(jsonPath("$.totalProjects").value(2))
                .andExpect(jsonPath("$.highRiskProjects").value(0))
                .andExpect(jsonPath("$.projects[*].name", containsInAnyOrder("Atlas IK", "Nova POS")));
        // Takip sayaclari da yalnizca gorunen projelerden: e-Belge'nin engeli ve karari Mehmet'te.
        as("ayse", get("/api/dashboard/summary"))
                .andExpect(jsonPath("$.openBlockers").value(0))
                .andExpect(jsonPath("$.pendingDecisions").value(0))
                .andExpect(jsonPath("$.overdueTrackingItems").value(1));
        as("mehmet", get("/api/dashboard/summary"))
                .andExpect(jsonPath("$.openRisks").value(0))
                .andExpect(jsonPath("$.openBlockers").value(1))
                .andExpect(jsonPath("$.pendingDecisions").value(1));
        // Trend de ayni kumeden: her haftada yalnizca gorunen projeler sayilir.
        as("cto", get("/api/dashboard/trend?year=2026&weekNumber=31&weeks=1"))
                .andExpect(jsonPath("$.weeks[0].totalProjects").value(3));
        as("ayse", get("/api/dashboard/trend?year=2026&weekNumber=31&weeks=1"))
                .andExpect(jsonPath("$.weeks[0].totalProjects").value(2))
                .andExpect(jsonPath("$.weeks[0].projectsWithReport").value(1));
        as("mehmet", get("/api/dashboard/trend?year=2026&weekNumber=31&weeks=1"))
                .andExpect(jsonPath("$.weeks[0].totalProjects").value(1));
        // Excel ciktisi da ayni kumeden: proje yoneticisi yalnizca kendi projelerini indirir.
        byte[] file = as("ayse", get("/api/dashboard/export")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(file))) {
            org.assertj.core.api.Assertions.assertThat(workbook.getSheet("Projeler").getLastRowNum()).isEqualTo(2);
        }
        // Baskasinin sorumlu filtresi hata degil, bos sonuc (filtre sozlesmesi, bolum 8).
        as("ayse", get("/api/dashboard/summary").param("ownerId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(0));
    }

    @Test
    @DisplayName("Projeler arasi rapor, is kalemi ve takip kaydi listeleri de sahiplige gore sinirlidir")
    void crossProjectListsAreScoped() throws Exception {
        as("cto", get("/api/reports")).andExpect(jsonPath("$.totalElements").value(4));
        as("mehmet", get("/api/reports"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].projectName").value("e-Belge"));
        as("mehmet", get("/api/work-items"))
                .andExpect(jsonPath("$.content[*].projectName", everyItem(is("e-Belge"))));
        as("ayse", get("/api/work-items"))
                .andExpect(jsonPath("$.content[*].projectName", everyItem(is("Atlas IK"))));
        as("cto", get("/api/tracking-items")).andExpect(jsonPath("$.totalElements").value(5));
        as("mehmet", get("/api/tracking-items"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].projectName", everyItem(is("e-Belge"))));
        // Baskasinin projesiyle filtrelemek hata degil, bos sonuc.
        as("mehmet", get("/api/tracking-items").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Baskasinin kaydi proje yoneticisine 404 doner; mesaj olmayan kayittakiyle ayni")
    void othersRecordsAreNotFoundForProjectManagers() throws Exception {
        String missing = as("mehmet", get("/api/projects/999")).andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        String hidden = as("mehmet", get("/api/projects/1")).andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat((String) JsonPath.read(hidden, "$.message"))
                .isEqualTo(((String) JsonPath.read(missing, "$.message")).replace("999", "1"));

        as("mehmet", get("/api/projects/1/reports")).andExpect(status().isNotFound());
        as("mehmet", get("/api/reports/4")).andExpect(status().isNotFound());
        as("mehmet", get("/api/reports/4/work-items")).andExpect(status().isNotFound());
        as("ayse", get("/api/projects/2")).andExpect(status().isNotFound());
        as("ayse", get("/api/work-items/" + ebelgeWorkItemId())).andExpect(status().isNotFound());
        as("mehmet", get("/api/projects/1/tracking-items")).andExpect(status().isNotFound());
        as("mehmet", get("/api/tracking-items/" + trackingItemId(1))).andExpect(status().isNotFound());
        as("ayse", get("/api/tracking-items/" + trackingItemId(2))).andExpect(status().isNotFound());

        // Kendi kayitlari acik.
        as("mehmet", get("/api/projects/2")).andExpect(status().isOk());
        as("ayse", get("/api/reports/4/work-items")).andExpect(status().isOk());
        as("mehmet", get("/api/projects/2/tracking-items")).andExpect(status().isOk());
        as("cto", get("/api/tracking-items/" + trackingItemId(1))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Kullanici listesi ve /me her rolde okunur")
    void everyRoleReadsUsersAndMe() throws Exception {
        for (String user : List.of("ayse", "cto", "admin")) {
            as(user, get("/api/users")).andExpect(status().isOk());
            as(user, get("/api/auth/me")).andExpect(status().isOk());
        }
    }

    // --- Yazma: rol ---

    @Test
    @DisplayName("CTO hicbir sey yazamaz: proje, rapor, is kalemi ve takip kaydi yazmalari 403 FORBIDDEN")
    void ctoIsReadOnly() throws Exception {
        as("cto", json(post("/api/projects"), PROJECT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Bu işlem için yetkiniz yok."));
        as("cto", delete("/api/projects/3")).andExpect(status().isForbidden());
        as("cto", json(post("/api/projects/3/reports"), NOVA_REPORT)).andExpect(status().isForbidden());
        as("cto", json(put("/api/reports/4"), ATLAS_W31_UPDATE)).andExpect(status().isForbidden());
        as("cto", delete("/api/reports/4")).andExpect(status().isForbidden());
        as("cto", json(post("/api/reports/4/work-items"), WORK_ITEM)).andExpect(status().isForbidden());
        as("cto", delete("/api/work-items/" + ebelgeWorkItemId())).andExpect(status().isForbidden());
        as("cto", json(post("/api/projects/1/tracking-items"), TRACKING_ITEM)).andExpect(status().isForbidden());
        as("cto", json(put("/api/tracking-items/" + trackingItemId(1)), TRACKING_ITEM)).andExpect(status().isForbidden());
        as("cto", delete("/api/tracking-items/" + trackingItemId(1))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rol yetmiyorsa istek govdesine bakilmaz: gecersiz govde ya da olmayan kayit da 403, alan hatasi sizmaz")
    void roleIsCheckedBeforeTheRequestBody() throws Exception {
        as("cto", json(post("/api/projects"), "{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
        as("cto", json(put("/api/reports/4"), "{}")).andExpect(status().isForbidden());
        as("cto", json(post("/api/reports/4/work-items"), "json degil")).andExpect(status().isForbidden());
        as("cto", json(post("/api/projects/999999/reports"), "{}")).andExpect(status().isForbidden());
        as("cto", json(post("/api/projects/1/tracking-items"), "{}")).andExpect(status().isForbidden());
        as("ayse", json(put("/api/projects/1"), "{}")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Kurali tanimlanmamis yazma istegi herkese kapali (Admin dahil): yeni endpoint kuralsiz acik kalmaz")
    void unlistedWritesAreDenied() throws Exception {
        as("admin", json(patch("/api/projects/1"), PROJECT)).andExpect(status().isForbidden());
        as("admin", json(post("/api/work-items"), WORK_ITEM)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Proje yoneticisi proje olusturamaz, duzenleyemez, silemez (kendi projesini de) - 403")
    void projectManagersCannotManageProjects() throws Exception {
        as("ayse", json(post("/api/projects"), PROJECT)).andExpect(status().isForbidden());
        as("ayse", json(put("/api/projects/1"), PROJECT)).andExpect(status().isForbidden());
        as("ayse", delete("/api/projects/3")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Proje yoneticisi yalnizca kendi projesine rapor, is kalemi ve takip kaydi yazar; baskasininkine 404")
    void projectManagersWriteOnlyTheirOwnProjects() throws Exception {
        as("ayse", json(post("/api/projects/3/reports"), NOVA_REPORT)).andExpect(status().isCreated());
        as("ayse", json(put("/api/reports/4"), ATLAS_W31_UPDATE)).andExpect(status().isOk());
        as("ayse", json(post("/api/reports/4/work-items"), WORK_ITEM)).andExpect(status().isCreated());

        as("mehmet", json(post("/api/projects/1/reports"), NOVA_REPORT)).andExpect(status().isNotFound());
        as("mehmet", json(put("/api/reports/4"), ATLAS_W31_UPDATE)).andExpect(status().isNotFound());
        as("mehmet", delete("/api/reports/4")).andExpect(status().isNotFound());
        as("mehmet", json(post("/api/reports/4/work-items"), WORK_ITEM)).andExpect(status().isNotFound());

        int ebelgeItem = ebelgeWorkItemId();
        as("ayse", delete("/api/work-items/" + ebelgeItem)).andExpect(status().isNotFound());
        as("mehmet", delete("/api/work-items/" + ebelgeItem)).andExpect(status().isNoContent());

        as("ayse", json(post("/api/projects/1/tracking-items"), TRACKING_ITEM)).andExpect(status().isCreated());
        as("mehmet", json(post("/api/projects/1/tracking-items"), TRACKING_ITEM)).andExpect(status().isNotFound());
        int ebelgeTrackingItem = trackingItemId(2);
        as("ayse", json(put("/api/tracking-items/" + ebelgeTrackingItem), TRACKING_ITEM)).andExpect(status().isNotFound());
        as("ayse", delete("/api/tracking-items/" + ebelgeTrackingItem)).andExpect(status().isNotFound());
        as("mehmet", json(put("/api/tracking-items/" + ebelgeTrackingItem), TRACKING_ITEM)).andExpect(status().isOk());
        as("mehmet", delete("/api/tracking-items/" + ebelgeTrackingItem)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Admin her seyi yapar: proje olusturur, baska sorumluya atar, raporsuz projeyi siler")
    void adminCanDoEverything() throws Exception {
        as("admin", json(post("/api/projects"), PROJECT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerFullName").value("Mehmet Kaya"));
        as("admin", json(post("/api/reports/3/work-items"), WORK_ITEM)).andExpect(status().isCreated());
        as("admin", json(post("/api/projects/2/tracking-items"), TRACKING_ITEM)).andExpect(status().isCreated());
        as("admin", delete("/api/projects/3")).andExpect(status().isNoContent());
    }
}
