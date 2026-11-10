package com.kolaysoft.ctotracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.kolaysoft.ctotracker.dto.DashboardSummaryResponse;
import com.kolaysoft.ctotracker.dto.filter.DashboardFilter;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
import com.kolaysoft.ctotracker.repository.WeeklyReportRepository;
import com.kolaysoft.ctotracker.repository.WorkItemRepository;
import com.kolaysoft.ctotracker.security.CurrentUser;
import com.kolaysoft.ctotracker.security.Viewer;
import com.kolaysoft.ctotracker.entity.Role;

/** DashboardService toplulastirma testleri (Spring context'siz, Mockito ile). */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WeeklyReportRepository weeklyReportRepository;

    @Mock
    private WorkItemRepository workItemRepository;

    @Mock
    private DelayPolicy delayPolicy;

    /** Takip ozeti varsayilan olarak bos harita doner (Mockito); kaydi olmayan projeler gibi davranir. */
    @Mock
    private TrackingItemService trackingItemService;

    /** Bu testler is kurallarini sinar; oturumdaki kullanici her projeyi goren Admin'dir. */

    @Mock

    private CurrentUser currentUser;


    @BeforeEach

    void signInAsAdmin() {

        lenient().when(currentUser.viewer()).thenReturn(new Viewer(99L, Role.ADMIN));

    }


    @InjectMocks
    private DashboardService service;

    /** Proje sorgusu artik filtre kosuluyla (Specification) ve id sirasiyla cagriliyor. */
    private static Specification<Project> anySpec() {
        return any();
    }

    private Project project(Long id, String name, RiskLevel ignored) {
        Project p = new Project();
        p.setId(id);
        p.setName(name);
        p.setCustomer("Demo Musteri A.S.");
        p.setStatus(ProjectStatus.DEVAM_EDIYOR);
        return p;
    }

    private WeeklyReport report(Long id, Long projectId, int week, ProgressStage stage, RiskLevel risk) {
        Project p = new Project();
        p.setId(projectId);
        WeeklyReport r = new WeeklyReport();
        r.setId(id);
        r.setProject(p);
        r.setWeekNumber(week);
        r.setReportDate(LocalDate.of(2026, 7, 27));
        r.setProgressStage(stage);
        r.setOverallStatus(OverallStatus.YOLUNDA);
        r.setRiskLevel(risk);
        return r;
    }

    /** Degerlendirilen raporlarin toplu sorgusu: verilen referans anahtariyla bu raporlar doner. */
    private void evaluated(int referenceKey, WeeklyReport... reports) {
        when(weeklyReportRepository.findEvaluatedByProjectIdIn(anyCollection(), eq(referenceKey)))
                .thenReturn(List.of(reports));
    }

    private static WorkItem item(WeeklyReport report, WorkItemStatus status) {
        WorkItem item = new WorkItem();
        item.setWeeklyReport(report);
        item.setStatus(status);
        return item;
    }

    @Test
    @DisplayName("Ozet: raporu olan ve olmayan projeler birlikte, son rapor ve sayaclar dogru")
    void summaryIncludesReportedAndUnreportedProjects() {
        Project atlas = project(1L, "Atlas IK", null);
        Project bos = project(2L, "Raporsuz", null);
        when(projectRepository.findAll(anySpec(), any(Sort.class))).thenReturn(List.of(atlas, bos));

        WeeklyReport atlasLast = report(10L, 1L, 30, ProgressStage.TEST, RiskLevel.YUKSEK);
        when(workItemRepository.findByWeeklyReportIdIn(List.of(10L))).thenReturn(List.of(
                item(atlasLast, WorkItemStatus.DEVAM_EDIYOR), item(atlasLast, WorkItemStatus.DEVAM_EDIYOR),
                item(atlasLast, WorkItemStatus.TAMAMLANDI)));

        evaluated(WeeklyReportRepository.LATEST, atlasLast);
        DashboardSummaryResponse summary = service.getSummary(DashboardFilter.none());

        assertThat(summary.totalProjects()).isEqualTo(2);
        assertThat(summary.highRiskProjects()).isEqualTo(1);
        assertThat(summary.projectsWithoutReport()).isEqualTo(1);
        assertThat(summary.totalLiveTasks()).isEqualTo(2);

        var atlasSummary = summary.projects().stream().filter(s -> s.projectId().equals(1L)).findFirst().orElseThrow();
        assertThat(atlasSummary.hasReport()).isTrue();
        assertThat(atlasSummary.lastWeekNumber()).isEqualTo(30);
        assertThat(atlasSummary.progressStage()).isEqualTo(ProgressStage.TEST);
        assertThat(atlasSummary.progressPercentage()).isEqualTo(75);
        assertThat(atlasSummary.liveTaskCount()).isEqualTo(2);

        var bosSummary = summary.projects().stream().filter(s -> s.projectId().equals(2L)).findFirst().orElseThrow();
        assertThat(bosSummary.hasReport()).isFalse();
        assertThat(bosSummary.progressStage()).isNull();
        assertThat(bosSummary.progressPercentage()).isZero();
        assertThat(bosSummary.liveTaskCount()).isZero();
    }

    @Test
    @DisplayName("Risk filtresi: yalnizca istenen risk seviyesindeki projeler doner")
    void riskFilterReturnsOnlyMatchingProjects() {
        Project atlas = project(1L, "Atlas IK", null);
        Project ebelge = project(2L, "e-Belge", null);
        when(projectRepository.findAll(anySpec(), any(Sort.class))).thenReturn(List.of(atlas, ebelge));

        WeeklyReport atlasLast = report(10L, 1L, 30, ProgressStage.TEST, RiskLevel.DUSUK);
        WeeklyReport ebelgeLast = report(20L, 2L, 30, ProgressStage.ANALIZ, RiskLevel.YUKSEK);

        evaluated(WeeklyReportRepository.LATEST, atlasLast, ebelgeLast);
        DashboardSummaryResponse summary = service.getSummary(
                new DashboardFilter(null, null, null, null, null, null, List.of(RiskLevel.YUKSEK), null));

        assertThat(summary.projects()).hasSize(1);
        assertThat(summary.projects().get(0).projectId()).isEqualTo(2L);
        assertThat(summary.projects().get(0).riskLevel()).isEqualTo(RiskLevel.YUKSEK);
    }

    @Test
    @DisplayName("Bos portfoy: proje yoksa sayaclar sifir, liste bos")
    void emptyPortfolioReturnsZeroes() {
        when(projectRepository.findAll(anySpec(), any(Sort.class))).thenReturn(List.of());

        DashboardSummaryResponse summary = service.getSummary(DashboardFilter.none());

        assertThat(summary.totalProjects()).isZero();
        assertThat(summary.projects()).isEmpty();
        assertThat(summary.totalLiveTasks()).isZero();
    }

    // --- Filtreler ---

    private DashboardFilter riskFilter(RiskLevel... levels) {
        return new DashboardFilter(null, null, null, null, null, null, List.of(levels), null);
    }

    private DashboardFilter delayedFilter(boolean delayed) {
        return new DashboardFilter(null, null, null, null, null, null, null, delayed);
    }

    @Test
    @DisplayName("Referans yil + hafta: o noktaya kadarki son rapor kullanilir, raporu olmayan proje hasReport=false")
    void weekNumberSelectsLatestReportUpToThatWeek() {
        Project atlas = project(1L, "Atlas IK", null);
        Project ebelge = project(2L, "e-Belge", null);
        when(projectRepository.findAll(anySpec(), any(Sort.class))).thenReturn(List.of(atlas, ebelge));

        WeeklyReport atlasW29 = report(10L, 1L, 29, ProgressStage.GELISTIRME, RiskLevel.ORTA);

        evaluated(202629, atlasW29);
        DashboardSummaryResponse summary = service.getSummary(
                new DashboardFilter(null, null, null, 2026, 29, null, null, null));

        assertThat(summary.totalProjects()).isEqualTo(2);
        assertThat(summary.projectsWithoutReport()).isEqualTo(1);
        assertThat(summary.projects().get(0).lastWeekNumber()).isEqualTo(29);
        assertThat(summary.projects().get(0).riskLevel()).isEqualTo(RiskLevel.ORTA);
        assertThat(summary.projects().get(1).hasReport()).isFalse();
    }

    @Test
    @DisplayName("Yalnizca yil: o yilin sonuna kadarki son rapor (anahtar yil*100+53)")
    void yearAloneSelectsLatestReportUpToEndOfYear() {
        when(projectRepository.findAll(anySpec(), any(Sort.class))).thenReturn(List.of(project(1L, "Atlas IK", null)));
        WeeklyReport atlasW52 = report(10L, 1L, 52, ProgressStage.TEST, RiskLevel.DUSUK);

        evaluated(202653, atlasW52);
        DashboardSummaryResponse summary = service.getSummary(
                new DashboardFilter(null, null, null, 2026, null, null, null, null));

        assertThat(summary.projects().get(0).lastWeekNumber()).isEqualTo(52);
    }

    @Test
    @DisplayName("Cok degerli risk filtresi VEYA ile birlesir; rapor filtresinde raporsuz proje listelenmez")
    void multiValueRiskFilterUsesOrAndExcludesUnreportedProjects() {
        Project dusuk = project(1L, "Dusuk", null);
        Project orta = project(2L, "Orta", null);
        Project yuksek = project(3L, "Yuksek", null);
        Project raporsuz = project(4L, "Raporsuz", null);
        when(projectRepository.findAll(anySpec(), any(Sort.class)))
                .thenReturn(List.of(dusuk, orta, yuksek, raporsuz));

        evaluated(WeeklyReportRepository.LATEST, report(10L, 1L, 30, ProgressStage.TEST, RiskLevel.DUSUK), report(20L, 2L, 30, ProgressStage.TEST, RiskLevel.ORTA), report(30L, 3L, 30, ProgressStage.TEST, RiskLevel.YUKSEK));
        DashboardSummaryResponse summary = service.getSummary(riskFilter(RiskLevel.ORTA, RiskLevel.YUKSEK));

        assertThat(summary.projects()).extracting(s -> s.projectId()).containsExactly(2L, 3L);
        assertThat(summary.totalProjects()).isEqualTo(2);
        assertThat(summary.highRiskProjects()).isEqualTo(1);
        assertThat(summary.projectsWithoutReport()).isZero();
    }

    @Test
    @DisplayName("Gecikme filtresi ve sayaci: true gecikenleri, false gecikmeyenleri doner; raporsuz proje elenir")
    void delayedFilterAndCounter() {
        Project geciken = project(1L, "Geciken", null);
        Project zamaninda = project(2L, "Zamaninda", null);
        Project raporsuz = project(3L, "Raporsuz", null);
        when(projectRepository.findAll(anySpec(), any(Sort.class)))
                .thenReturn(List.of(geciken, zamaninda, raporsuz));
        WeeklyReport gecikenRapor = report(10L, 1L, 30, ProgressStage.TEST, RiskLevel.ORTA);
        WeeklyReport zamanindaRapor = report(20L, 2L, 30, ProgressStage.TEST, RiskLevel.DUSUK);
        when(delayPolicy.isDelayed(eq(gecikenRapor), anyCollection())).thenReturn(true);
        

        evaluated(WeeklyReportRepository.LATEST, gecikenRapor, zamanindaRapor);
        DashboardSummaryResponse all = service.getSummary(DashboardFilter.none());
        DashboardSummaryResponse onlyDelayed = service.getSummary(delayedFilter(true));
        DashboardSummaryResponse notDelayed = service.getSummary(delayedFilter(false));

        assertThat(all.delayedProjects()).isEqualTo(1);
        assertThat(all.totalProjects()).isEqualTo(3);
        assertThat(onlyDelayed.projects()).extracting(s -> s.projectId()).containsExactly(1L);
        assertThat(onlyDelayed.delayedProjects()).isEqualTo(1);
        assertThat(notDelayed.projects()).extracting(s -> s.projectId()).containsExactly(2L);
        assertThat(notDelayed.delayedProjects()).isZero();
    }

    @Test
    @DisplayName("Takip ozeti: satirda acik / gecikmis kayit ve en yuksek seviye; sayaclar yalnizca listelenen projelerden")
    void trackingSummaryFollowsListedProjects() {
        Project atlas = project(1L, "Atlas IK", null);
        Project ebelge = project(2L, "e-Belge", null);
        when(projectRepository.findAll(anySpec(), any(Sort.class))).thenReturn(List.of(atlas, ebelge));
        when(trackingItemService.openSummaries(List.of(1L, 2L))).thenReturn(Map.of(
                1L, new TrackingSummary(3, 1, 1, 0, 1, RiskLevel.YUKSEK),
                2L, new TrackingSummary(1, 0, 0, 1, 0, RiskLevel.ORTA)));

        evaluated(WeeklyReportRepository.LATEST, report(10L, 1L, 30, ProgressStage.TEST, RiskLevel.YUKSEK), report(20L, 2L, 30, ProgressStage.ANALIZ, RiskLevel.DUSUK));
        DashboardSummaryResponse all = service.getSummary(DashboardFilter.none());

        assertThat(all.openRisks()).isEqualTo(1);
        assertThat(all.openBlockers()).isEqualTo(1);
        assertThat(all.pendingDecisions()).isEqualTo(1);
        assertThat(all.overdueTrackingItems()).isEqualTo(1);
        assertThat(all.projects().getFirst().openTrackingItems()).isEqualTo(3);
        assertThat(all.projects().getFirst().overdueTrackingItems()).isEqualTo(1);
        assertThat(all.projects().getFirst().highestOpenRiskLevel()).isEqualTo(RiskLevel.YUKSEK);

        // Rapor filtresi e-Belge'yi eler: onun engeli sayaclardan da duser.
        DashboardSummaryResponse highRisk = service.getSummary(
                new DashboardFilter(null, null, null, null, null, null, List.of(RiskLevel.YUKSEK), null));
        assertThat(highRisk.projects()).extracting(s -> s.projectId()).containsExactly(1L);
        assertThat(highRisk.openBlockers()).isZero();
        assertThat(highRisk.openRisks()).isEqualTo(1);
    }

    @Test
    @DisplayName("Takip kaydi olmayan proje: sayilar 0, en yuksek seviye null")
    void projectWithoutTrackingItems() {
        when(projectRepository.findAll(anySpec(), any(Sort.class))).thenReturn(List.of(project(3L, "Nova POS", null)));

        evaluated(WeeklyReportRepository.LATEST);
        DashboardSummaryResponse summary = service.getSummary(DashboardFilter.none());

        assertThat(summary.projects().getFirst().openTrackingItems()).isZero();
        assertThat(summary.projects().getFirst().highestOpenRiskLevel()).isNull();
        assertThat(summary.overdueTrackingItems()).isZero();
    }
}
