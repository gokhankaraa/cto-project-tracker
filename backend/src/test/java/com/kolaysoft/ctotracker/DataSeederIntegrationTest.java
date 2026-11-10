package com.kolaysoft.ctotracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.config.DataSeeder;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
import com.kolaysoft.ctotracker.repository.WeeklyReportRepository;

/**
 * Demo verisinin trend kismi: "Orion Mobil" (proje 4, Mehmet) son 10 haftada bugune gore raporlanir, boylece
 * varsayilan 12 haftalik trend her gun hareketli gorunur. Mehmet'in diger projesi e-Belge her hafta riskli
 * (2026 h30 raporu).
 */
@SpringBootTest
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class DataSeederIntegrationTest {

    private static final String ORION = "Orion Mobil";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    private List<Project> orion() {
        return projectRepository.findAll().stream().filter(p -> p.getName().equals(ORION)).toList();
    }

    private long reportsOf(Project project) {
        return weeklyReportRepository.findAll().stream()
                .filter(r -> r.getProject().getId().equals(project.getId()))
                .count();
    }

    @Test
    @DisplayName("Varsayilan trend: Orion 11 hafta once raporsuz, sonra yolunda -> riskli -> gecikmeli -> bloke -> yolunda")
    void trendMovesWeekByWeek() throws Exception {
        // weeks[i] = (11 - i) hafta once; son eleman bu hafta (Orion'un gecen haftaki raporu sayilir).
        mockMvc.perform(get("/api/dashboard/trend").param("ownerId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks.length()").value(12))
                .andExpect(jsonPath("$.weeks[*].totalProjects", contains(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)))
                .andExpect(jsonPath("$.weeks[*].projectsWithReport", contains(1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)))
                .andExpect(jsonPath("$.weeks[*].projectsByStatus.YOLUNDA", contains(0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1)))
                .andExpect(jsonPath("$.weeks[*].projectsByStatus.RISKLI", contains(1, 1, 1, 2, 2, 1, 1, 1, 2, 1, 1, 1)))
                .andExpect(jsonPath("$.weeks[*].projectsByStatus.GECIKMELI", contains(0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0)))
                .andExpect(jsonPath("$.weeks[*].projectsByStatus.BLOKE", contains(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0)));
    }

    @Test
    @DisplayName("Dashboard'da Orion en son raporuyla yolunda, dusuk riskli ve gecikmesiz; sayaclari degistirmez")
    void latestReportKeepsCountersCalm() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(4))
                .andExpect(jsonPath("$.delayedProjects").value(1))
                .andExpect(jsonPath("$.highRiskProjects").value(1))
                .andExpect(jsonPath("$.projects[3].name").value(ORION))
                .andExpect(jsonPath("$.projects[3].overallStatus").value("YOLUNDA"))
                .andExpect(jsonPath("$.projects[3].delayed").value(false));
    }

    @Test
    @DisplayName("Mevcut demo veritabaninda Orion yoksa seeder bir kez ekler; tekrar calisinca kopya olusmaz")
    void missingTrendProjectIsSeededOnce() {
        Project existing = orion().getFirst();
        weeklyReportRepository.deleteAll(weeklyReportRepository.findAll().stream()
                .filter(r -> r.getProject().getId().equals(existing.getId()))
                .toList());
        projectRepository.delete(existing);
        assertThat(orion()).isEmpty();

        dataSeeder.run();
        dataSeeder.run();

        assertThat(orion()).hasSize(1);
        assertThat(reportsOf(orion().getFirst())).isEqualTo(10);
    }
}
