package com.kolaysoft.ctotracker;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        // Baskasinin sorumlu filtresi hata degil, bos sonuc (filtre sozlesmesi, bolum 8).
        as("ayse", get("/api/dashboard/summary").param("ownerId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(0));
    }

    @Test
    @DisplayName("Projeler arasi rapor ve is kalemi listeleri de sahiplige gore sinirlidir")
    void crossProjectListsAreScoped() throws Exception {
        as("cto", get("/api/reports")).andExpect(jsonPath("$.totalElements").value(4));
        as("mehmet", get("/api/reports"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].projectName").value("e-Belge"));
        as("mehmet", get("/api/work-items"))
                .andExpect(jsonPath("$.content[*].projectName", everyItem(is("e-Belge"))));
        as("ayse", get("/api/work-items"))
                .andExpect(jsonPath("$.content[*].projectName", everyItem(is("Atlas IK"))));
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

        // Kendi kayitlari acik.
        as("mehmet", get("/api/projects/2")).andExpect(status().isOk());
        as("ayse", get("/api/reports/4/work-items")).andExpect(status().isOk());
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
    @DisplayName("CTO hicbir sey yazamaz: proje, rapor ve is kalemi yazmalari 403 FORBIDDEN")
    void ctoIsReadOnly() throws Exception {
        as("cto", json(post("/api/projects"), PROJECT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Bu islem icin yetkiniz yok."));
        as("cto", delete("/api/projects/3")).andExpect(status().isForbidden());
        as("cto", json(post("/api/projects/3/reports"), NOVA_REPORT)).andExpect(status().isForbidden());
        as("cto", json(put("/api/reports/4"), ATLAS_W31_UPDATE)).andExpect(status().isForbidden());
        as("cto", delete("/api/reports/4")).andExpect(status().isForbidden());
        as("cto", json(post("/api/reports/4/work-items"), WORK_ITEM)).andExpect(status().isForbidden());
        as("cto", delete("/api/work-items/" + ebelgeWorkItemId())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Proje yoneticisi proje olusturamaz, duzenleyemez, silemez (kendi projesini de) - 403")
    void projectManagersCannotManageProjects() throws Exception {
        as("ayse", json(post("/api/projects"), PROJECT)).andExpect(status().isForbidden());
        as("ayse", json(put("/api/projects/1"), PROJECT)).andExpect(status().isForbidden());
        as("ayse", delete("/api/projects/3")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Proje yoneticisi yalnizca kendi projesine rapor ve is kalemi yazar; baskasininkine 404")
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
    }

    @Test
    @DisplayName("Admin her seyi yapar: proje olusturur, baska sorumluya atar, raporsuz projeyi siler")
    void adminCanDoEverything() throws Exception {
        as("admin", json(post("/api/projects"), PROJECT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerFullName").value("Mehmet Kaya"));
        as("admin", json(post("/api/reports/3/work-items"), WORK_ITEM)).andExpect(status().isCreated());
        as("admin", delete("/api/projects/3")).andExpect(status().isNoContent());
    }
}
