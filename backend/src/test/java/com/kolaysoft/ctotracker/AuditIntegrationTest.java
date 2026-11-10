package com.kolaysoft.ctotracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.kolaysoft.ctotracker.config.ClockConfig;
import com.kolaysoft.ctotracker.config.DataSeeder;
import com.kolaysoft.ctotracker.entity.AuditAction;
import com.kolaysoft.ctotracker.entity.AuditChange;
import com.kolaysoft.ctotracker.entity.AuditEntityType;
import com.kolaysoft.ctotracker.entity.AuditEvent;
import com.kolaysoft.ctotracker.repository.AuditEventRepository;

/**
 * Degisiklik gecmisi uctan uca: demo kullanicilari gercek token'la yazar, gecmis veritabanindan okunur.
 * Seed verisi gecmise yazilmaz (servislerden gecmez), bu yuzden her test bos gecmisle baslar. Okuma tarafinin sorgu
 * sayisi Hibernate istatistikleriyle olculur.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class AuditIntegrationTest {

    private static final String TODAY_PLUS_3 = LocalDate.now(ClockConfig.ZONE).plusDays(3).toString();

    /** e-Belge'nin seed engeli, risk seviyesi ORTA yerine YUKSEK; diger alanlar seed'deki gibi. */
    private static final String BLOCKER_HIGH = """
            { "type": "ENGEL", "title": "Test ortamina erisim yok", "riskLevel": "YUKSEK",
              "impact": "Entegrasyon testleri baslayamiyor", "actionPlan": "Musteri BT ekibinden VPN erisimi istendi",
              "assignee": "Mehmet Kaya", "followUpDate": "%s", "status": "DEVAM_EDIYOR" }
            """.formatted(TODAY_PLUS_3);

    @Autowired
    private MockMvc mockMvc;

    /** Gecmis yazilamadiginda degisikligin geri alindigini gostermek icin save hata firlatabilir. */
    @MockitoSpyBean
    private AuditEventRepository auditEventRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private final Map<String, String> tokens = new HashMap<>();

    private ResultActions as(String user, MockHttpServletRequestBuilder request) throws Exception {
        if (!tokens.containsKey(user)) {
            String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "email": "%s@example.com", "password": "%s" }
                                    """.formatted(user, DataSeeder.DEMO_PASSWORD)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            tokens.put(user, JsonPath.read(body, "$.token"));
        }
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get(user)));
    }

    private ResultActions as(String user, MockHttpServletRequestBuilder request, String json) throws Exception {
        return as(user, request.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static int id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private List<AuditEvent> events() {
        return auditEventRepository.findAll(org.springframework.data.domain.Sort.by("id"));
    }

    /** e-Belge'nin seed engelinin id'si. */
    private int blockerId() throws Exception {
        String body = as("mehmet", get("/api/projects/2/tracking-items")).andReturn().getResponse()
                .getContentAsString();
        List<Integer> ids = JsonPath.read(body, "$[?(@.type == 'ENGEL')].id");
        return ids.getFirst();
    }

    @Test
    @DisplayName("12 yazma yolunun her biri tam bir olay yazar: tur, islem, kayit, proje ve yazan kisi")
    void everyWriteIsRecordedOnce() throws Exception {
        int project = id(as("admin", post("/api/projects"), """
                { "name": "Gecmis Testi", "status": "PLANLANDI", "ownerId": 1 }
                """).andExpect(status().isCreated()));
        as("admin", put("/api/projects/" + project), """
                { "name": "Gecmis Testi", "customer": "Demo", "status": "DEVAM_EDIYOR", "ownerId": 1 }
                """).andExpect(status().isOk());
        int report = id(as("admin", post("/api/projects/" + project + "/reports"), """
                { "weekNumber": 33, "reportDate": "2026-08-13", "progressStage": "ANALIZ",
                  "overallStatus": "YOLUNDA", "riskLevel": "DUSUK" }
                """).andExpect(status().isCreated()));
        as("admin", put("/api/reports/" + report), """
                { "weekNumber": 33, "reportDate": "2026-08-13", "progressStage": "GELISTIRME",
                  "overallStatus": "RISKLI", "riskLevel": "DUSUK" }
                """).andExpect(status().isOk());
        int workItem = id(as("admin", post("/api/reports/" + report + "/work-items"), """
                { "title": "Kurulum", "status": "PLANLANDI" }
                """).andExpect(status().isCreated()));
        as("admin", put("/api/work-items/" + workItem), """
                { "title": "Kurulum", "status": "DEVAM_EDIYOR", "assignee": "Ayse" }
                """).andExpect(status().isOk());
        int item = id(as("admin", post("/api/projects/" + project + "/tracking-items"), """
                { "type": "AKSIYON", "title": "Toplanti", "assignee": "Ayse", "followUpDate": "%s", "status": "ACIK" }
                """.formatted(TODAY_PLUS_3)).andExpect(status().isCreated()));
        as("admin", put("/api/tracking-items/" + item), """
                { "type": "AKSIYON", "title": "Toplanti", "status": "KAPANDI" }
                """).andExpect(status().isOk());
        as("admin", delete("/api/tracking-items/" + item)).andExpect(status().isNoContent());
        as("admin", delete("/api/work-items/" + workItem)).andExpect(status().isNoContent());
        as("admin", delete("/api/reports/" + report)).andExpect(status().isNoContent());
        as("admin", delete("/api/projects/" + project)).andExpect(status().isNoContent());

        List<AuditEvent> events = events();
        assertThat(events).extracting(AuditEvent::getEntityType, AuditEvent::getAction).containsExactly(
                tuple(AuditEntityType.PROJE, AuditAction.OLUSTURULDU),
                tuple(AuditEntityType.PROJE, AuditAction.GUNCELLENDI),
                tuple(AuditEntityType.RAPOR, AuditAction.OLUSTURULDU),
                tuple(AuditEntityType.RAPOR, AuditAction.GUNCELLENDI),
                tuple(AuditEntityType.IS_KALEMI, AuditAction.OLUSTURULDU),
                tuple(AuditEntityType.IS_KALEMI, AuditAction.GUNCELLENDI),
                tuple(AuditEntityType.TAKIP_KAYDI, AuditAction.OLUSTURULDU),
                tuple(AuditEntityType.TAKIP_KAYDI, AuditAction.GUNCELLENDI),
                tuple(AuditEntityType.TAKIP_KAYDI, AuditAction.SILINDI),
                tuple(AuditEntityType.IS_KALEMI, AuditAction.SILINDI),
                tuple(AuditEntityType.RAPOR, AuditAction.SILINDI),
                tuple(AuditEntityType.PROJE, AuditAction.SILINDI));
        assertThat(events).allSatisfy(event -> {
            assertThat(event.getProjectId()).isEqualTo(project);
            assertThat(event.getActorId()).isEqualTo(4L);
            assertThat(event.getActorName()).isEqualTo("Sistem Admin");
        });
        assertThat(events.get(2).getSummary()).isEqualTo("2026 / 33. hafta");
        // Rapor guncellemesi: yalnizca degisen iki alan.
        assertThat(events.get(3).getChanges()).extracting(AuditChange::getField, AuditChange::getOldValue,
                AuditChange::getNewValue).containsExactly(
                tuple("progressStage", "ANALIZ", "GELISTIRME"),
                tuple("overallStatus", "YOLUNDA", "RISKLI"));
        // Kapatilan aksiyonun kapanis tarihi sunucuda yazilir; gecmiste de gorunur.
        assertThat(events.get(7).getChanges()).extracting(AuditChange::getField)
                .contains("status", "closedDate", "assignee", "followUpDate");
    }

    @Test
    @DisplayName("Risk seviyesini kim, ne zaman degistirdi: yazan kisi token'dan; ayni degerlerle tekrar kayit yok")
    void riskLevelChangeByProjectManager() throws Exception {
        int blocker = blockerId();
        as("mehmet", put("/api/tracking-items/" + blocker), BLOCKER_HIGH).andExpect(status().isOk());
        as("mehmet", put("/api/tracking-items/" + blocker), BLOCKER_HIGH).andExpect(status().isOk());

        assertThat(events()).singleElement().satisfies(event -> {
            assertThat(event.getActorId()).isEqualTo(2L);
            assertThat(event.getActorName()).isEqualTo("Mehmet Kaya");
            assertThat(event.getProjectId()).isEqualTo(2L);
            assertThat(event.getEntityId()).isEqualTo(blocker);
            assertThat(event.getSummary()).isEqualTo("Test ortamina erisim yok");
            assertThat(event.getChanges()).extracting(AuditChange::getField, AuditChange::getOldValue,
                    AuditChange::getNewValue).containsExactly(tuple("riskLevel", "ORTA", "YUKSEK"));
        });
    }

    @Test
    @DisplayName("Reddedilen degisiklik iz birakmaz: 400, 403, 404, 409")
    void rejectedChangesLeaveNoTrace() throws Exception {
        as("ayse", post("/api/projects/1/reports"), """
                { "weekNumber": 31, "reportDate": "2026-08-03", "progressStage": "TEST",
                  "overallStatus": "YOLUNDA", "riskLevel": "DUSUK" }
                """).andExpect(status().isConflict());
        as("ayse", post("/api/projects/1/tracking-items"), """
                { "type": "RISK", "title": "Seviyesiz risk", "assignee": "Ayse", "followUpDate": "%s",
                  "status": "ACIK" }
                """.formatted(TODAY_PLUS_3)).andExpect(status().isBadRequest());
        as("cto", put("/api/tracking-items/" + blockerId()), BLOCKER_HIGH).andExpect(status().isForbidden());
        as("ayse", put("/api/tracking-items/" + blockerId()), BLOCKER_HIGH).andExpect(status().isNotFound());
        as("admin", delete("/api/projects/1")).andExpect(status().isConflict());

        assertThat(events()).isEmpty();
    }

    // --- Okuma: GET /api/projects/{id}/history ---

    @Test
    @DisplayName("Gecmis: en yeni ustte, tur ve kayit filtresi, sayfalama; degerler ham")
    void historyNewestFirstWithFilters() throws Exception {
        int blocker = blockerId();
        as("mehmet", put("/api/tracking-items/" + blocker), BLOCKER_HIGH).andExpect(status().isOk());
        as("mehmet", post("/api/reports/3/work-items"), """
                { "title": "VPN testi", "status": "PLANLANDI", "plannedDate": "2026-08-07" }
                """).andExpect(status().isCreated());
        as("mehmet", put("/api/tracking-items/" + blocker), BLOCKER_HIGH.replace("YUKSEK", "ORTA"))
                .andExpect(status().isOk());

        as("mehmet", get("/api/projects/2/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].entityType").value("TAKIP_KAYDI"))
                .andExpect(jsonPath("$.content[0].action").value("GUNCELLENDI"))
                .andExpect(jsonPath("$.content[0].entityId").value(blocker))
                .andExpect(jsonPath("$.content[0].summary").value("Test ortamina erisim yok"))
                .andExpect(jsonPath("$.content[0].actorId").value(2))
                .andExpect(jsonPath("$.content[0].actorName").value("Mehmet Kaya"))
                .andExpect(jsonPath("$.content[0].occurredAt").isNotEmpty())
                .andExpect(jsonPath("$.content[0].changes[0].field").value("riskLevel"))
                .andExpect(jsonPath("$.content[0].changes[0].oldValue").value("YUKSEK"))
                .andExpect(jsonPath("$.content[0].changes[0].newValue").value("ORTA"))
                .andExpect(jsonPath("$.content[1].entityType").value("IS_KALEMI"))
                .andExpect(jsonPath("$.content[1].action").value("OLUSTURULDU"))
                .andExpect(jsonPath("$.content[1].changes[?(@.field == 'plannedDate')].newValue").value("2026-08-07"))
                .andExpect(jsonPath("$.content[1].changes[0].oldValue").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.content[2].changes[0].newValue").value("YUKSEK"));

        as("mehmet", get("/api/projects/2/history").param("entityType", "TAKIP_KAYDI"))
                .andExpect(jsonPath("$.totalElements").value(2));
        as("mehmet", get("/api/projects/2/history").param("entityType", "TAKIP_KAYDI")
                        .param("entityId", String.valueOf(blocker)))
                .andExpect(jsonPath("$.totalElements").value(2));
        as("mehmet", get("/api/projects/2/history").param("entityType", "IS_KALEMI"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].summary").value("VPN testi"));
        as("mehmet", get("/api/projects/2/history").param("size", "1").param("page", "2"))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].changes[0].newValue").value("YUKSEK"));
    }

    @Test
    @DisplayName("Gecmisi projeyi goren okur: PM baskasinin projesinde 404, CTO her projede; hatali parametre 400")
    void historyVisibilityAndValidation() throws Exception {
        as("mehmet", put("/api/tracking-items/" + blockerId()), BLOCKER_HIGH).andExpect(status().isOk());

        as("cto", get("/api/projects/2/history")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        as("ayse", get("/api/projects/2/history")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Proje")));
        as("ayse", get("/api/projects/1/history")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        as("admin", get("/api/projects/999/history")).andExpect(status().isNotFound());

        as("admin", get("/api/projects/2/history").param("entityId", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("entityId"));
        as("admin", get("/api/projects/2/history").param("entityType", "KULLANICI"))
                .andExpect(status().isBadRequest());
        as("admin", get("/api/projects/2/history").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sorgu sayisi olay sayisiyla buyumez: degisen alanlar tek sorguda yuklenir")
    void historyQueryCountIsConstant() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        as("admin", get("/api/projects/3")).andExpect(status().isOk()); // token alinir

        for (int i = 0; i < 30; i++) {
            as("admin", put("/api/projects/3"), """
                    { "name": "Nova POS %d", "status": "PLANLANDI", "ownerId": 1 }
                    """.formatted(i)).andExpect(status().isOk());
        }
        // Test transaction'inda yazilan olaylar bellekte kalir; gercek istek gibi veritabanindan okunsunlar.
        entityManager.flush();
        entityManager.clear();
        statistics.clear();
        as("admin", get("/api/projects/3/history")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.content[0].changes[0].newValue").value("Nova POS 29"));

        // Proje gorunurlugu, sayfa, toplam sayisi ve sayfanin degisen alanlari: 20 olay icin 20 sorgu degil.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Gecmis yazilamazsa degisiklik de geri alinir: izsiz degisiklik olmaz")
    void failedAuditRollsBackTheChange() throws Exception {
        doThrow(new DataAccessResourceFailureException("gecmis tablosu yazilamadi"))
                .when(auditEventRepository).save(any());

        as("admin", put("/api/projects/3"), """
                { "name": "Kaybolmamali", "status": "PLANLANDI", "ownerId": 1 }
                """).andExpect(status().is5xxServerError());

        as("admin", get("/api/projects/3")).andExpect(jsonPath("$.name").value("Nova POS"));
    }
}
