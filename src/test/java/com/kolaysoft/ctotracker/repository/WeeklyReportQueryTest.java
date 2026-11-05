package com.kolaysoft.ctotracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.kolaysoft.ctotracker.TestcontainersConfiguration;
import com.kolaysoft.ctotracker.common.web.SortParameter;
import com.kolaysoft.ctotracker.dto.filter.ReportFilter;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.repository.spec.WeeklyReportSpecifications;

/**
 * Rapor filtre kosullarinin gercek veritabaninda testi (T13 filtre sozlesmesi, bolum 3).
 * {@code @DataJpaTest} DataSeeder'i calistirmaz; veri her testte burada kurulur ve geri alinir.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class WeeklyReportQueryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private WeeklyReportRepository repository;

    private User ali;
    private User veli;
    private Project zeta;
    private Project alfa;
    private WeeklyReport zetaW10;
    private WeeklyReport zetaW11;
    private WeeklyReport alfaW11;
    private WeeklyReport alfaW12;

    @BeforeEach
    void setUp() {
        ali = user("ali@example.com", "Ali");
        veli = user("veli@example.com", "Veli");
        // "Zeta" buyuk, "alfa" kucuk harfle: siralamanin buyuk/kucuk harfe duyarsiz oldugunu gostermek icin.
        zeta = project("Zeta", ali);
        alfa = project("alfa", veli);
        zetaW10 = report(zeta, 10, LocalDate.of(2026, 3, 2), ProgressStage.ANALIZ, OverallStatus.YOLUNDA, RiskLevel.DUSUK);
        zetaW11 = report(zeta, 11, LocalDate.of(2026, 3, 9), ProgressStage.GELISTIRME, OverallStatus.RISKLI, RiskLevel.ORTA);
        alfaW11 = report(alfa, 11, LocalDate.of(2026, 3, 9), ProgressStage.ANALIZ, OverallStatus.GECIKMELI, RiskLevel.YUKSEK);
        alfaW12 = report(alfa, 12, LocalDate.of(2026, 3, 16), ProgressStage.TEST, OverallStatus.BLOKE, RiskLevel.YUKSEK);
        em.flush();
    }

    private User user(String email, String name) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(Role.PROJECT_MANAGER);
        return em.persist(u);
    }

    private Project project(String name, User owner) {
        Project p = new Project();
        p.setName(name);
        p.setStatus(ProjectStatus.DEVAM_EDIYOR);
        p.setOwner(owner);
        return em.persist(p);
    }

    private WeeklyReport report(Project project, int week, LocalDate date, ProgressStage stage,
                                OverallStatus status, RiskLevel risk) {
        WeeklyReport r = new WeeklyReport();
        r.setProject(project);
        r.setWeekNumber(week);
        r.setReportDate(date);
        r.setProgressStage(stage);
        r.setOverallStatus(status);
        r.setRiskLevel(risk);
        return em.persist(r);
    }

    private ReportFilter filter(Long projectId, Long ownerId, Integer weekFrom, Integer weekTo,
                                LocalDate dateFrom, LocalDate dateTo) {
        return new ReportFilter(projectId, ownerId, weekFrom, weekTo, dateFrom, dateTo, null, null, null);
    }

    private List<WeeklyReport> find(ReportFilter filter) {
        return repository.findAll(WeeklyReportSpecifications.matching(filter));
    }

    @Test
    @DisplayName("Filtre yoksa tum raporlar doner")
    void emptyFilterReturnsAll() {
        assertThat(find(filter(null, null, null, null, null, null)))
                .containsExactlyInAnyOrder(zetaW10, zetaW11, alfaW11, alfaW12);
    }

    @Test
    @DisplayName("Proje ve sorumlu filtreleri")
    void projectAndOwnerFilters() {
        assertThat(find(filter(zeta.getId(), null, null, null, null, null))).containsExactlyInAnyOrder(zetaW10, zetaW11);
        assertThat(find(filter(null, veli.getId(), null, null, null, null))).containsExactlyInAnyOrder(alfaW11, alfaW12);
        assertThat(find(filter(zeta.getId(), veli.getId(), null, null, null, null))).isEmpty();
    }

    @Test
    @DisplayName("Hafta araliginin iki ucu da dahildir")
    void weekRangeIsInclusive() {
        assertThat(find(filter(null, null, 11, 11, null, null))).containsExactlyInAnyOrder(zetaW11, alfaW11);
        assertThat(find(filter(null, null, 10, 11, null, null))).containsExactlyInAnyOrder(zetaW10, zetaW11, alfaW11);
        assertThat(find(filter(null, null, 12, null, null, null))).containsExactly(alfaW12);
    }

    @Test
    @DisplayName("Tarih araliginin iki ucu da dahildir")
    void dateRangeIsInclusive() {
        assertThat(find(filter(null, null, null, null, LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 16))))
                .containsExactlyInAnyOrder(zetaW11, alfaW11, alfaW12);
        assertThat(find(filter(null, null, null, null, null, LocalDate.of(2026, 3, 2)))).containsExactly(zetaW10);
    }

    @Test
    @DisplayName("Enum filtreleri: ayni parametrede VEYA, farkli parametrelerde VE")
    void enumFiltersCombineWithOrInsideAndAcross() {
        ReportFilter risk = new ReportFilter(null, null, null, null, null, null,
                null, List.of(RiskLevel.ORTA, RiskLevel.YUKSEK), null);
        ReportFilter riskAndStage = new ReportFilter(null, null, null, null, null, null,
                null, List.of(RiskLevel.ORTA, RiskLevel.YUKSEK), List.of(ProgressStage.TEST));
        ReportFilter status = new ReportFilter(null, null, null, null, null, null,
                List.of(OverallStatus.GECIKMELI, OverallStatus.BLOKE), null, null);

        assertThat(find(risk)).containsExactlyInAnyOrder(zetaW11, alfaW11, alfaW12);
        assertThat(find(riskAndStage)).containsExactly(alfaW12);
        assertThat(find(status)).containsExactlyInAnyOrder(alfaW11, alfaW12);
    }

    @Test
    @DisplayName("Sayfalama ve varsayilan siralama: en yeni rapor en ustte, esitlikte id")
    void pagingWithDefaultSort() {
        PageRequest firstPage = PageRequest.of(0, 3,
                SortParameter.parse(null, Map.of("reportDate", "reportDate"), "reportDate,desc"));

        Page<WeeklyReport> page = repository.findAll(
                WeeklyReportSpecifications.matching(filter(null, null, null, null, null, null)), firstPage);

        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
        // 03-16, sonra ayni tarihli (03-09) iki rapordan id'si buyuk olan, sonra digeri
        assertThat(page.getContent()).containsExactly(alfaW12, alfaW11, zetaW11);
    }

    @Test
    @DisplayName("Proje adina gore siralama buyuk/kucuk harfe duyarsizdir: 'alfa' 'Zeta'dan once")
    void projectNameSortIgnoresCase() {
        PageRequest byName = PageRequest.of(0, 10,
                SortParameter.parse("projectName", Map.of("projectName", "project.name"), "projectName"));

        List<WeeklyReport> content = repository.findAll(
                WeeklyReportSpecifications.matching(filter(null, null, null, null, null, null)), byName).getContent();

        assertThat(content).extracting(r -> r.getProject().getName()).containsExactly("alfa", "alfa", "Zeta", "Zeta");
    }
}
