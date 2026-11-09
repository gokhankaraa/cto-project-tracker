package com.kolaysoft.ctotracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.kolaysoft.ctotracker.TestcontainersConfiguration;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.WeeklyReport;

import jakarta.persistence.PersistenceException;

/**
 * Rapor yilinin (V2) gercek PostgreSQL'de testi: yilin tarihten turetilmesi, proje + yil + hafta
 * benzersizligi ve "en son rapor" seciminin yil sinirinda dogru calismasi.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class WeeklyReportYearTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private WeeklyReportRepository repository;

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

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "2026-07-27, 2026",
            "2026-12-31, 2026",  // Persembe, 2026'nin 53. haftasi
            "2027-01-01, 2026",  // Cuma, hala 2026'nin 53. haftasi
            "2027-01-04, 2027",  // Pazartesi, 2027'nin 1. haftasi
            "2024-12-30, 2025"   // Pazartesi, 2025'in 1. haftasi
    })
    @DisplayName("Java'daki yil kurali V2 migration'indaki PostgreSQL ISOYEAR ile ayni sonucu verir")
    void javaYearMatchesPostgresIsoYear(LocalDate date, int expectedYear) {
        Object postgresYear = em.getEntityManager()
                .createNativeQuery("SELECT CAST(EXTRACT(ISOYEAR FROM CAST(?1 AS DATE)) AS INTEGER)")
                .setParameter(1, date)
                .getSingleResult();

        assertThat(WeeklyReport.yearOf(date)).isEqualTo(expectedYear);
        assertThat(postgresYear).isEqualTo(expectedYear);
    }

    @Test
    @DisplayName("Kaydedilen raporun yili rapor tarihinden gelir ve tarih degisince guncellenir")
    void yearFollowsReportDate() {
        WeeklyReport r = report(30, LocalDate.of(2026, 7, 27));
        em.flush();
        assertThat(r.getYear()).isEqualTo(2026);

        r.setReportDate(LocalDate.of(2027, 7, 26));
        em.flush();
        em.clear();

        assertThat(repository.findById(r.getId()).orElseThrow().getYear()).isEqualTo(2027);
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

        assertThat(repository.findTopByProjectIdOrderByYearDescWeekNumberDesc(project.getId()))
                .contains(newest);
    }
}
