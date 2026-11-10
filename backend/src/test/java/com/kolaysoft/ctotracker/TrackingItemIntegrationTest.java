package com.kolaysoft.ctotracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.kolaysoft.ctotracker.config.ClockConfig;

/**
 * Risk, engel, karar ve aksiyon kayitlari HTTP uzerinden (MockMvc, gercek PostgreSQL): CRUD, alan hatalari,
 * V5 kisitlarina uyan kayit, proje silme engeli. Seed verisine dayanir: proje 1 Atlas IK (3 kayit),
 * proje 2 e-Belge (2 kayit), proje 3 Nova POS (kayitsiz). Tarihler bugune goredir; her test geri alinir.
 */
@SpringBootTest
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class TrackingItemIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ClockConfig.ZONE);

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String risk(String status, LocalDate followUp) {
        return """
                { "type": "RISK", "title": "Kasa donanimi gec gelebilir", "riskLevel": "YUKSEK",
                  "impact": "Pilot magaza acilisi kayar", "actionPlan": "Ikinci tedarikciden teklif",
                  "assignee": "Ayşe Yılmaz", "followUpDate": "%s", "status": "%s" }
                """.formatted(followUp, status);
    }

    private long create(long projectId, String body) throws Exception {
        MvcResult result = mockMvc.perform(json(post("/api/projects/" + projectId + "/tracking-items"), body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    @DisplayName("Olustur, oku, kapat, yeniden ac, sil: kapanis tarihi durumu izler")
    void lifecycle() throws Exception {
        long id = create(3, risk("ACIK", TODAY.plusDays(5)));

        mockMvc.perform(get("/api/tracking-items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectName").value("Nova POS"))
                .andExpect(jsonPath("$.type").value("RISK"))
                .andExpect(jsonPath("$.riskLevel").value("YUKSEK"))
                .andExpect(jsonPath("$.assignee").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.followUpDate").value(TODAY.plusDays(5).toString()))
                .andExpect(jsonPath("$.closedDate").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.overdue").value(false));

        mockMvc.perform(json(put("/api/tracking-items/" + id), risk("KAPANDI", TODAY.plusDays(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("KAPANDI"))
                .andExpect(jsonPath("$.closedDate").value(TODAY.toString()));

        mockMvc.perform(json(put("/api/tracking-items/" + id), risk("DEVAM_EDIYOR", TODAY.plusDays(6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedDate").doesNotExist());

        mockMvc.perform(delete("/api/tracking-items/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tracking-items/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Takip kaydı bulunamadı: " + id));
    }

    @Test
    @DisplayName("Proje listesi: seed kayitlari, acik olanlar takip tarihine gore, kapali karar en altta; gecikmis aksiyon isaretli")
    void projectList() throws Exception {
        mockMvc.perform(get("/api/projects/1/tracking-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("AKSIYON"))
                .andExpect(jsonPath("$[0].overdue").value(true))
                .andExpect(jsonPath("$[1].type").value("RISK"))
                .andExpect(jsonPath("$[2].type").value("KARAR"))
                .andExpect(jsonPath("$[2].status").value("KAPANDI"))
                .andExpect(jsonPath("$[2].closedDate").exists());

        mockMvc.perform(get("/api/projects/3/tracking-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/projects/999/tracking-items")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Alan hatalari 400 VALIDATION_ERROR ile alan adini doner")
    void fieldErrors() throws Exception {
        mockMvc.perform(json(post("/api/projects/3/tracking-items"), "{ \"title\": \" \" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='type')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='title')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='status')]").exists());

        mockMvc.perform(json(post("/api/projects/3/tracking-items"), """
                        { "type": "ENGEL", "title": "Erisim yok", "assignee": "Mehmet",
                          "followUpDate": "%s", "status": "ACIK" }
                        """.formatted(TODAY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("riskLevel"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Risk ve engel kayıtlarında risk seviyesi zorunludur."));

        mockMvc.perform(json(post("/api/projects/3/tracking-items"), risk("ACIK", TODAY.minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("followUpDate"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Takip tarihi bugünden önce olamaz."));

        mockMvc.perform(json(post("/api/projects/3/tracking-items"), """
                        { "type": "AKSIYON", "title": "Toplanti", "status": "ACIK", "followUpDate": "%s" }
                        """.formatted(TODAY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assignee"));

        mockMvc.perform(json(post("/api/projects/3/tracking-items"), """
                        { "type": "SORUN", "title": "x", "status": "ACIK" }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Bekleyen karar sorumlu ve tarihle acilir; karar verilince tarihsiz kapatilabilir")
    void decisionWithoutFollowUpOnceDecided() throws Exception {
        long id = create(3, """
                { "type": "KARAR", "title": "Odeme saglayicisi", "assignee": "Zeynep Demir",
                  "followUpDate": "%s", "status": "ACIK" }
                """.formatted(TODAY.plusDays(4)));

        mockMvc.perform(json(put("/api/tracking-items/" + id), """
                        { "type": "KARAR", "title": "Odeme saglayicisi", "actionPlan": "A saglayicisi secildi",
                          "status": "KAPANDI" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").doesNotExist())
                .andExpect(jsonPath("$.followUpDate").doesNotExist())
                .andExpect(jsonPath("$.closedDate").value(TODAY.toString()));
    }

    @Test
    @DisplayName("Takip kaydi olan proje silinemez (409); kayit silinince proje silinir")
    void projectWithItemsCannotBeDeleted() throws Exception {
        long id = create(3, risk("ACIK", TODAY));

        mockMvc.perform(delete("/api/projects/3"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Bu projenin risk, engel, karar ya da aksiyon kayıtları var; önce bu kayıtları silin."));

        mockMvc.perform(delete("/api/tracking-items/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/projects/3")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Projeler arasi liste: varsayilan siralama takip tarihi (tarihsiz kapali karar sonda), filtreler, sayfa")
    void crossProjectList() throws Exception {
        mockMvc.perform(get("/api/tracking-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content[0].title").value("Müşteriyle kapsam toplantısı"))
                .andExpect(jsonPath("$.content[0].overdue").value(true))
                .andExpect(jsonPath("$.content[4].title").value("Raporlama kütüphanesi seçimi"));

        mockMvc.perform(get("/api/tracking-items?overdue=true"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("AKSIYON"));
        mockMvc.perform(get("/api/tracking-items?type=KARAR&status=ACIK,DEVAM_EDIYOR"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("e-Arşiv sağlayıcısı seçimi"));
        mockMvc.perform(get("/api/tracking-items?riskLevel=ORTA,YUKSEK"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/tracking-items?assignee=zeynep&projectId=1"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/tracking-items?size=2&page=1&sort=title,asc"))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Müşteriyle kapsam toplantısı"));
    }

    @Test
    @DisplayName("Dashboard: takip sayaclari ve proje satirlari (acik / gecikmis kayit, en yuksek acik risk seviyesi)")
    void dashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openRisks").value(1))
                .andExpect(jsonPath("$.openBlockers").value(1))
                .andExpect(jsonPath("$.pendingDecisions").value(1))
                .andExpect(jsonPath("$.overdueTrackingItems").value(1))
                .andExpect(jsonPath("$.projects[0].openTrackingItems").value(2))
                .andExpect(jsonPath("$.projects[0].overdueTrackingItems").value(1))
                .andExpect(jsonPath("$.projects[0].highestOpenRiskLevel").value("YUKSEK"))
                .andExpect(jsonPath("$.projects[1].openTrackingItems").value(2))
                .andExpect(jsonPath("$.projects[1].highestOpenRiskLevel").value("ORTA"))
                .andExpect(jsonPath("$.projects[2].openTrackingItems").value(0))
                .andExpect(jsonPath("$.projects[2].highestOpenRiskLevel").doesNotExist());

        // Hafta referansi takip ozetini degistirmez: kayitlar haftaya bagli degil, bugunku durum gosterilir.
        mockMvc.perform(get("/api/dashboard/summary?year=2026&weekNumber=29"))
                .andExpect(jsonPath("$.openRisks").value(1))
                .andExpect(jsonPath("$.projects[0].openTrackingItems").value(2));
        // Proje filtresi sayaclari da daraltir.
        mockMvc.perform(get("/api/dashboard/summary?projectId=2"))
                .andExpect(jsonPath("$.openRisks").value(0))
                .andExpect(jsonPath("$.openBlockers").value(1))
                .andExpect(jsonPath("$.overdueTrackingItems").value(0));
    }
}
