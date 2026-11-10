package com.kolaysoft.ctotracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

/**
 * Projeler arasi listelerin sorgu sayisi satir sayisiyla buyumez: proje ve rapor sayfa sorgusunda gelir, canli task
 * sayilari tek sorguda. Sayilar Hibernate istatistikleriyle olculur; test transaction'inda yazilan kayitlar once
 * veritabanina yazilip bellekten atilir (aksi halde sorgu olmadan bellekten okunurlardi).
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class ListQueryCountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    /** 25 proje x 2 hafta x 2 is kalemi (biri devam ediyor): 50 rapor, 100 is kalemi; sayfa 20 satir. */
    @BeforeEach
    void setUp() {
        for (int p = 1; p <= 25; p++) {
            Project project = new Project();
            project.setName("Sorgu Proje " + p);
            project.setStatus(ProjectStatus.DEVAM_EDIYOR);
            entityManager.persist(project);
            for (int week = 30; week <= 31; week++) {
                WeeklyReport report = new WeeklyReport();
                report.setProject(project);
                report.setWeekNumber(week);
                report.setReportDate(WeeklyReport.mondayOf(2026, week).plusWeeks(1));
                report.setProgressStage(ProgressStage.GELISTIRME);
                report.setOverallStatus(OverallStatus.YOLUNDA);
                report.setRiskLevel(RiskLevel.DUSUK);
                entityManager.persist(report);
                for (WorkItemStatus status : new WorkItemStatus[] {WorkItemStatus.DEVAM_EDIYOR, WorkItemStatus.PLANLANDI}) {
                    WorkItem item = new WorkItem();
                    item.setWeeklyReport(report);
                    item.setTitle("Kalem " + p + "-" + week + "-" + status);
                    item.setStatus(status);
                    item.setPlannedDate(LocalDate.of(2026, 9, p));
                    entityManager.persist(item);
                }
            }
        }
        entityManager.flush();
        entityManager.clear();
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    @DisplayName("Rapor listesi: 20 satir icin sayfa, toplam ve canli task sayilari (3 sorgu; satir basina degil)")
    void reportListQueryCount() throws Exception {
        mockMvc.perform(get("/api/reports").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalElements").value(50))
                .andExpect(jsonPath("$.content[0].projectName").isNotEmpty())
                .andExpect(jsonPath("$.content[*].liveTaskCount", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(1))));

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Is kalemi listesi: 20 satir icin sayfa ve toplam (2 sorgu; rapor ve proje sayfa sorgusunda)")
    void workItemListQueryCount() throws Exception {
        mockMvc.perform(get("/api/work-items").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                // Her projenin yalnizca degerlendirilen (31.) haftasi: 25 proje x 2 kalem.
                .andExpect(jsonPath("$.totalElements").value(50))
                .andExpect(jsonPath("$.content[*].weekNumber", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(31))))
                .andExpect(jsonPath("$.content[0].projectName").isNotEmpty());

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }
}
