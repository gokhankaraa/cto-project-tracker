package com.kolaysoft.ctotracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import com.kolaysoft.ctotracker.TestcontainersConfiguration;
import com.kolaysoft.ctotracker.dto.filter.WorkItemFilter;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;
import com.kolaysoft.ctotracker.repository.spec.WorkItemSpecifications;
import com.kolaysoft.ctotracker.service.DelayPolicy;

/**
 * Is kalemi filtre kosullarinin ve gecikme kuralinin gercek veritabaninda testi
 * (filtre sozlesmesi bolum 4 ve S-05, analiz notu KF-01 ve KF-09).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class WorkItemQueryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private WorkItemRepository repository;

    private DelayPolicy delayPolicy;

    private Project p1;
    private WeeklyReport p1W11;
    private WeeklyReport p2W10;

    @BeforeEach
    void setUp() {
        delayPolicy = new DelayPolicy(repository);

        p1 = project("P1");
        Project p2 = project("P2");
        WeeklyReport p1W10 = report(p1, 10, LocalDate.of(2026, 3, 2));
        p1W11 = report(p1, 11, LocalDate.of(2026, 3, 9));
        p2W10 = report(p2, 10, LocalDate.of(2026, 3, 2));

        // P1 hafta 10: en son rapor degil, varsayilan listede gorunmemeli
        item(p1W10, "eski hafta", "Ali Veli", WorkItemStatus.DEVAM_EDIYOR, LocalDate.of(2026, 3, 1));
        // P1 hafta 11 (rapor tarihi 03-09): gecikme kuralinin dort durumu ve arama ornekleri
        item(p1W11, "geciken tarih", "Ayse Yilmaz", WorkItemStatus.DEVAM_EDIYOR, LocalDate.of(2026, 3, 5));
        item(p1W11, "bitmis eski", "ayse_k", WorkItemStatus.TAMAMLANDI, LocalDate.of(2026, 3, 1));
        item(p1W11, "sinir gunu", "%100 Ekip", WorkItemStatus.DEVAM_EDIYOR, LocalDate.of(2026, 3, 9));
        item(p1W11, "gecikti", null, WorkItemStatus.GECIKTI, null);
        item(p1W11, "tarihsiz", "Mehmet", WorkItemStatus.PLANLANDI, null);
        // P2 hafta 10: P2'nin en son raporu
        item(p2W10, "p2 isi", "Mehmet Kaya", WorkItemStatus.PLANLANDI, LocalDate.of(2026, 3, 20));
        em.flush();
        em.clear();
    }

    private Project project(String name) {
        Project p = new Project();
        p.setName(name);
        p.setStatus(ProjectStatus.DEVAM_EDIYOR);
        return em.persist(p);
    }

    private WeeklyReport report(Project project, int week, LocalDate date) {
        return report(project, week, date, OverallStatus.YOLUNDA);
    }

    private WeeklyReport report(Project project, int week, LocalDate date, OverallStatus status) {
        WeeklyReport r = new WeeklyReport();
        r.setProject(project);
        r.setWeekNumber(week);
        r.setReportDate(date);
        r.setProgressStage(ProgressStage.GELISTIRME);
        r.setOverallStatus(status);
        r.setRiskLevel(RiskLevel.DUSUK);
        return em.persist(r);
    }

    private WorkItem item(WeeklyReport report, String title, String assignee, WorkItemStatus status,
                          LocalDate plannedDate) {
        WorkItem w = new WorkItem();
        w.setWeeklyReport(report);
        w.setTitle(title);
        w.setAssignee(assignee);
        w.setStatus(status);
        w.setPlannedDate(plannedDate);
        return em.persist(w);
    }

    private Set<String> titles(Specification<WorkItem> spec) {
        return repository.findAll(spec).stream().map(WorkItem::getTitle).collect(Collectors.toSet());
    }

    private Set<String> titles(WorkItemFilter filter) {
        return titles(WorkItemSpecifications.matching(filter));
    }

    private WorkItemFilter assigneeFilter(String assignee) {
        return new WorkItemFilter(null, null, null, assignee, null, null);
    }

    @Test
    @DisplayName("Degerlendirilen rapor: varsayilan olarak her projenin yalnizca en son raporundaki is kalemleri")
    void defaultListUsesLatestReportPerProject() {
        assertThat(titles(new WorkItemFilter(null, null, null, null, null, null)))
                .containsExactlyInAnyOrder("geciken tarih", "bitmis eski", "sinir gunu", "gecikti", "tarihsiz", "p2 isi");
    }

    @Test
    @DisplayName("Degerlendirilen rapor: yil sinirinda en son rapor yila gore secilir (2027 h1 > 2026 h52)")
    void defaultListComparesYearBeforeWeek() {
        Project p3 = project("P3");
        WeeklyReport december = report(p3, 52, LocalDate.of(2026, 12, 21));
        WeeklyReport january = report(p3, 1, LocalDate.of(2027, 1, 4));
        item(december, "aralik isi", null, WorkItemStatus.TAMAMLANDI, null);
        item(january, "ocak isi", null, WorkItemStatus.PLANLANDI, null);
        em.flush();
        em.clear();

        assertThat(titles(new WorkItemFilter(p3.getId(), null, null, null, null, null)))
                .containsExactly("ocak isi");
    }

    @Test
    @DisplayName("Referans yil + hafta: her proje icin o haftaya kadarki en son rapor; oncesinde rapor yoksa bos")
    void weekNumberUsesLatestReportUpToThatWeek() {
        assertThat(titles(new WorkItemFilter(null, 2026, 10, null, null, null))).containsExactlyInAnyOrder("eski hafta", "p2 isi");
        assertThat(titles(new WorkItemFilter(null, 2026, 9, null, null, null))).isEmpty();
        assertThat(titles(new WorkItemFilter(p1.getId(), 2026, 10, null, null, null))).containsExactly("eski hafta");
        // Referans nokta onceki yillari da kapsar: 2027 h5'e kadarki en son rapor 2026 h11'dir.
        assertThat(titles(new WorkItemFilter(p1.getId(), 2027, 5, null, null, null)))
                .containsExactlyInAnyOrder("geciken tarih", "bitmis eski", "sinir gunu", "gecikti", "tarihsiz");
    }

    @Test
    @DisplayName("Referans yil sinirda: yalnizca yil o yilin sonu, yil + hafta o hafta demektir")
    void yearReferenceAcrossYearBoundary() {
        Project p3 = project("P3");
        WeeklyReport december = report(p3, 52, LocalDate.of(2026, 12, 21));
        WeeklyReport january = report(p3, 1, LocalDate.of(2027, 1, 4));
        item(december, "aralik isi", null, WorkItemStatus.TAMAMLANDI, null);
        item(january, "ocak isi", null, WorkItemStatus.PLANLANDI, null);
        em.flush();
        em.clear();

        assertThat(titles(new WorkItemFilter(p3.getId(), 2026, null, null, null, null))).containsExactly("aralik isi");
        assertThat(titles(new WorkItemFilter(p3.getId(), 2027, null, null, null, null))).containsExactly("ocak isi");
        assertThat(titles(new WorkItemFilter(p3.getId(), 2027, 1, null, null, null))).containsExactly("ocak isi");
        assertThat(titles(new WorkItemFilter(p3.getId(), 2026, 51, null, null, null))).isEmpty();
    }

    @Test
    @DisplayName("Sorumlu aramasi buyuk/kucuk harfe duyarsiz 'icerir' aramasidir")
    void assigneeSearchIsCaseInsensitiveContains() {
        assertThat(titles(assigneeFilter("AYSE"))).containsExactlyInAnyOrder("geciken tarih", "bitmis eski");
        assertThat(titles(assigneeFilter("  mehmet  "))).containsExactlyInAnyOrder("tarihsiz", "p2 isi");
    }

    @Test
    @DisplayName("Sorumlu aramasinda % ve _ joker degil duz karakter olarak aranir")
    void assigneeSearchEscapesWildcards() {
        assertThat(titles(assigneeFilter("_"))).containsExactly("bitmis eski");
        assertThat(titles(assigneeFilter("%"))).containsExactly("sinir gunu");
    }

    @Test
    @DisplayName("Gecikme kosulu: GECIKTI veya tarihi raporun tarihinden once olup tamamlanmamis is")
    void delayedConditionCoversAllCases() {
        assertThat(titles(new WorkItemFilter(null, null, null, null, null, true)))
                .containsExactlyInAnyOrder("geciken tarih", "gecikti");
        // Planlanan tarihi olmayan, GECIKTI olmayan kayit da 'false' sonucunda yer almali (NULL karsilastirmasi tuzagi).
        assertThat(titles(new WorkItemFilter(null, null, null, null, null, false)))
                .containsExactlyInAnyOrder("bitmis eski", "sinir gunu", "tarihsiz", "p2 isi");
    }

    @Test
    @DisplayName("Filtreler VE ile birlesir; durum filtresi cok degerli")
    void filtersCombine() {
        assertThat(titles(new WorkItemFilter(p1.getId(), null, null, "ayse", List.of(WorkItemStatus.DEVAM_EDIYOR), true)))
                .containsExactly("geciken tarih");
        assertThat(titles(new WorkItemFilter(null, null, null, null,
                List.of(WorkItemStatus.GECIKTI, WorkItemStatus.PLANLANDI), null)))
                .containsExactlyInAnyOrder("gecikti", "tarihsiz", "p2 isi");
    }

    @Test
    @DisplayName("Sorgudaki gecikme kosulu ile DelayPolicy her is kalemi icin ayni sonucu verir")
    void queryConditionMatchesDelayPolicy() {
        Set<Long> delayedByQuery = repository.findAll(WorkItemSpecifications.delayed(true)).stream()
                .map(WorkItem::getId).collect(Collectors.toSet());

        List<WorkItem> all = repository.findAll();
        assertThat(all).hasSize(7);
        for (WorkItem w : all) {
            assertThat(delayPolicy.isDelayed(w))
                    .as("'%s' icin bellek ici kural ile sorgu ayni olmali", w.getTitle())
                    .isEqualTo(delayedByQuery.contains(w.getId()));
        }
    }

    @Test
    @DisplayName("Rapor gecikmesi: GECIKMELI durum ya da raporda gecikmis is kalemi")
    void reportLevelDelay() {
        WeeklyReport gecikmeliDurum = report(project("P3"), 10, LocalDate.of(2026, 3, 2), OverallStatus.GECIKMELI);
        em.flush();

        assertThat(delayPolicy.isDelayed(em.find(WeeklyReport.class, p1W11.getId()))).isTrue();
        assertThat(delayPolicy.isDelayed(em.find(WeeklyReport.class, p2W10.getId()))).isFalse();
        assertThat(delayPolicy.isDelayed(gecikmeliDurum)).isTrue();
    }
}
