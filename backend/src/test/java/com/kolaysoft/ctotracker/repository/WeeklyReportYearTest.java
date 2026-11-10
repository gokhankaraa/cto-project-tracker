package com.kolaysoft.ctotracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.time.temporal.IsoFields;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import com.kolaysoft.ctotracker.TestcontainersConfiguration;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.WeeklyReport;

import jakarta.persistence.PersistenceException;

/**
 * Rapor yilinin gercek PostgreSQL'de testi: yilin anlatilan haftadan turetilmesi (V3) ve V3 migration'inin
 * ayni kurali uygulamasi, proje + yil + hafta benzersizligi ve "en son rapor" seciminin yil sinirinda dogru
 * calismasi.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class WeeklyReportYearTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private WeeklyReportRepository repository;

    /** Ayni islem (transaction) icinde calisir; migration SQL'i ve ham INSERT icin. */
    private JdbcTemplate jdbc;

    @Autowired
    void setDataSource(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    private Project project;

    @BeforeEach
    void setUp() {
        Project p = new Project();
        p.setName("Yil Projesi");
        p.setStatus(ProjectStatus.DEVAM_EDIYOR);
        project = em.persist(p);
    }

    private WeeklyReport report(int week, LocalDate date) {
        WeeklyReport r = new WeeklyReport();
        r.setProject(project);
        r.setWeekNumber(week);
        r.setReportDate(date);
        r.setProgressStage(ProgressStage.ANALIZ);
        r.setOverallStatus(OverallStatus.YOLUNDA);
        r.setRiskLevel(RiskLevel.DUSUK);
        return em.persist(r);
    }

    @ParameterizedTest(name = "hafta {0}, tarih {1} -> {2}")
    @CsvSource({
            "31, 2026-08-03, 2026",  // Seed: 31. haftanin raporu ertesi pazartesi yazilir
            "30, 2026-07-27, 2026",
            "53, 2026-12-31, 2026",  // Rapor haftanin icinde
            "53, 2027-01-04, 2026",  // 2026'nin 53. haftasi, raporu 2027'de yazilir (V2'de 2027 idi)
            "52, 2027-01-04, 2026",  // 2026'nin 52. haftasi, iki hafta sonra; en yakin yine 2026
            " 1, 2027-01-04, 2027",  // Pazartesi, 2027'nin 1. haftasi
            " 1, 2024-12-30, 2025",  // Takvimde 2024, ISO'da 2025'in 1. haftasi
            "52, 2026-01-05, 2025"   // 2025 52 haftalik; 52. haftanin raporu 2026'nin ilk pazartesi
    })
    @DisplayName("Yil, anlatilan haftanin yilidir; Java kurali ile V3 migration'i ayni sonucu verir")
    void reportYearIsTheReportedWeeksYear(int week, LocalDate date, int expectedYear) {
        assertThat(WeeklyReport.yearOf(week, date)).isEqualTo(expectedYear);

        // V3: kayit V2 kuraliyla (tarihin ISO yili) eklenir, migration SQL'i yeniden calistirilir.
        int v2Year = date.get(IsoFields.WEEK_BASED_YEAR);
        jdbc.update("INSERT INTO weekly_reports (project_id, report_date, year, week_number, progress_stage, "
                + "overall_status, risk_level) VALUES (?, ?, ?, ?, 'ANALIZ', 'YOLUNDA', 'DUSUK')",
                project.getId(), date, v2Year, week);
        jdbc.update(v3Sql());

        Integer migratedYear = jdbc.queryForObject(
                "SELECT year FROM weekly_reports WHERE project_id = ? AND week_number = ?",
                Integer.class, project.getId(), week);
        assertThat(migratedYear).isEqualTo(expectedYear);
    }

    @Test
    @DisplayName("Rapor tarihi ve hafta hangi sirayla verilirse verilsin yil yeniden hesaplanir")
    void yearFollowsWeekAndReportDate() {
        WeeklyReport r = report(30, LocalDate.of(2026, 7, 27));
        em.flush();
        assertThat(r.getYear()).isEqualTo(2026);

        r.setReportDate(LocalDate.of(2027, 7, 26));
        assertThat(r.getYear()).isEqualTo(2027);

        r.setReportDate(LocalDate.of(2027, 1, 4));
        r.setWeekNumber(53);
        em.flush();
        em.clear();

        assertThat(repository.findById(r.getId()).orElseThrow().getYear()).isEqualTo(2026);
    }

    @ParameterizedTest(name = "{0} yilinda 53. hafta {1}")
    @CsvSource({"2026, true", "2020, true", "2025, false", "2027, false"})
    @DisplayName("53. hafta yalnizca 53 haftalik yillarda vardir; pazartesisi dogru hesaplanir")
    void week53ExistsOnlyInLongYears(int year, boolean exists) {
        assertThat(WeeklyReport.hasWeek(year, 53)).isEqualTo(exists);
        assertThat(WeeklyReport.hasWeek(year, 52)).isTrue();
        if (exists) {
            assertThat(WeeklyReport.mondayOf(year, 53).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(WeeklyReport.mondayOf(year, 53).get(IsoFields.WEEK_BASED_YEAR)).isEqualTo(year);
        }
        assertThat(WeeklyReport.mondayOf(2026, 31)).isEqualTo(LocalDate.of(2026, 7, 27));
    }

    private String v3Sql() {
        try {
            return new ClassPathResource("db/migration/V3__fix_report_year_at_week_boundary.sql")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("Ayni hafta farkli yilda kaydedilebilir; ayni yil + hafta veritabaninda reddedilir")
    void uniquenessIsPerProjectYearAndWeek() {
        report(29, LocalDate.of(2026, 7, 20));
        report(29, LocalDate.of(2027, 7, 19));
        em.flush();

        assertThat(repository.existsByProjectIdAndYearAndWeekNumber(project.getId(), 2026, 29)).isTrue();
        assertThat(repository.existsByProjectIdAndYearAndWeekNumber(project.getId(), 2027, 29)).isTrue();
        assertThat(repository.existsByProjectIdAndYearAndWeekNumber(project.getId(), 2028, 29)).isFalse();

        // id IDENTITY ile uretildigi icin INSERT persist aninda calisir; kisit hatasi da burada gelir.
        assertThatThrownBy(() -> report(29, LocalDate.of(2026, 7, 22)))
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining("uk_weekly_report_project_year_week");
    }

    @Test
    @DisplayName("En son rapor yil sinirinda yila gore secilir: 2027 h1, 2026 h52'den yenidir")
    void latestReportComparesYearBeforeWeek() {
        report(52, LocalDate.of(2026, 12, 21));
        WeeklyReport newest = report(1, LocalDate.of(2027, 1, 4));
        em.flush();

        assertThat(repository.findEvaluatedByProjectIdIn(List.of(project.getId()), WeeklyReportRepository.LATEST))
                .containsExactly(newest);
        // Referansla: 2026 h53'e kadar 2026 h52 secilir (2027 h1 referanstan sonra).
        assertThat(repository.findEvaluatedByProjectIdIn(List.of(project.getId()), WeeklyReport.weekKey(2026, 53)))
                .extracting(WeeklyReport::getWeekNumber).containsExactly(52);
    }
}
