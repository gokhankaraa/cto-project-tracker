package com.kolaysoft.ctotracker.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.dto.DashboardProjectSummary;
import com.kolaysoft.ctotracker.dto.DashboardSummaryResponse;
import com.kolaysoft.ctotracker.dto.filter.DashboardFilter;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
import com.kolaysoft.ctotracker.repository.WeeklyReportRepository;
import com.kolaysoft.ctotracker.repository.WorkItemRepository;
import com.kolaysoft.ctotracker.repository.spec.ProjectSpecifications;
import com.kolaysoft.ctotracker.security.CurrentUser;

/**
 * CTO dashboard toplulastirma mantigi (on analiz H-06, filtre sozlesmesi bolum 2).
 *
 * <p>Her proje icin degerlendirilen rapor bulunur: referans verilmezse en guncel rapor (en yeni yil, o yil
 * icinde en yuksek hafta), yalnizca yil verilirse o yilin sonuna kadarki, yil + hafta verilirse o haftaya
 * kadarki en son rapor. Ilerleme, durum, risk, canli task ve
 * gecikme bilgisi bu rapordan turer. Raporu olmayan projeler de listelenir (ilerleme bilgisi bos/varsayilan).
 *
 * <p>Filtreler iki asamada uygulanir: proje filtreleri (proje, durum, sorumlu) veritabani sorgusunda,
 * rapora dayali filtreler (rapor durumu, risk, gecikme) ozet satirlari uzerinde. Proje sayisi kucuk
 * oldugu icin ikinci asama bellekte yapilir. Sayaclar filtrelenmis kumeden hesaplanir.
 *
 * <p>Risk, engel, karar ve aksiyon ozeti ({@link TrackingSummary}) haftaya bagli degildir: yil / hafta
 * referansi verilse de bugunku durumu gosterir. Yalnizca listelenen projelerin kayitlari sayilir.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final WorkItemRepository workItemRepository;
    private final DelayPolicy delayPolicy;
    private final CurrentUser currentUser;
    private final TrackingItemService trackingItemService;

    public DashboardService(ProjectRepository projectRepository,
                            WeeklyReportRepository weeklyReportRepository,
                            WorkItemRepository workItemRepository,
                            DelayPolicy delayPolicy,
                            CurrentUser currentUser,
                            TrackingItemService trackingItemService) {
        this.projectRepository = projectRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.workItemRepository = workItemRepository;
        this.delayPolicy = delayPolicy;
        this.currentUser = currentUser;
        this.trackingItemService = trackingItemService;
    }

    /**
     * Portfoy ozetini uretir. Projeler id'ye gore artan sirada doner.
     *
     * @param filter dashboard filtresi; {@link DashboardFilter#none()} tum projeleri dondurur
     */
    public DashboardSummaryResponse getSummary(DashboardFilter filter) {
        // Gorunurluk kosulu filtrelerle VE ile birlesir (filtre sozlesmesi, bolum 8): proje yoneticisi
        // yalnizca kendi projelerini gorur, sayaclar da bu kumeden hesaplanir.
        List<Project> projects = projectRepository.findAll(ProjectSpecifications.matching(filter)
                .and(ProjectSpecifications.ownedBy(currentUser.viewer().visibleOwnerId())), Sort.by("id"));
        Map<Long, TrackingSummary> tracking = trackingItemService.openSummaries(
                projects.stream().map(Project::getId).toList());

        List<DashboardProjectSummary> summaries = projects.stream()
                .map(project -> toSummary(project, filter.referenceKey(),
                        tracking.getOrDefault(project.getId(), TrackingSummary.EMPTY)))
                .filter(summary -> matchesReportCriteria(summary, filter))
                .toList();

        long highRisk = summaries.stream().filter(s -> RiskLevel.YUKSEK.equals(s.riskLevel())).count();
        long withoutReport = summaries.stream().filter(s -> !s.hasReport()).count();
        long totalLiveTasks = summaries.stream().mapToLong(DashboardProjectSummary::liveTaskCount).sum();
        long delayed = summaries.stream().filter(DashboardProjectSummary::delayed).count();

        // Takip sayaclari yalnizca filtreden gecen projelerin kayitlarindan.
        List<TrackingSummary> listed = summaries.stream()
                .map(s -> tracking.getOrDefault(s.projectId(), TrackingSummary.EMPTY))
                .toList();

        return new DashboardSummaryResponse(
                summaries.size(), highRisk, withoutReport, totalLiveTasks, delayed,
                listed.stream().mapToLong(TrackingSummary::openRisks).sum(),
                listed.stream().mapToLong(TrackingSummary::openBlockers).sum(),
                listed.stream().mapToLong(TrackingSummary::pendingDecisions).sum(),
                listed.stream().mapToLong(TrackingSummary::overdueItems).sum(),
                summaries);
    }

    /**
     * Rapora dayali filtreler. Bu filtrelerden biri verildiginde raporu olmayan projeler listelenmez
     * (filtreleme analizi KF-04).
     */
    private boolean matchesReportCriteria(DashboardProjectSummary summary, DashboardFilter filter) {
        if (!filter.hasReportCriteria()) {
            return true;
        }
        if (!summary.hasReport()) {
            return false;
        }
        return (filter.riskLevels().isEmpty() || filter.riskLevels().contains(summary.riskLevel()))
                && (filter.overallStatuses().isEmpty() || filter.overallStatuses().contains(summary.overallStatus()))
                && (filter.delayed() == null || filter.delayed() == summary.delayed());
    }

    private DashboardProjectSummary toSummary(Project project, Integer referenceKey, TrackingSummary tracking) {
        User owner = project.getOwner();
        String ownerName = owner != null ? owner.getFullName() : null;

        Optional<WeeklyReport> evaluatedReport = findEvaluatedReport(project.getId(), referenceKey);

        if (evaluatedReport.isEmpty()) {
            return new DashboardProjectSummary(
                    project.getId(), project.getName(), project.getCustomer(), ownerName,
                    project.getStatus(), false, null, null, null, 0, null, null, 0, false,
                    tracking.openItems(), tracking.overdueItems(), tracking.highestRiskLevel());
        }

        WeeklyReport r = evaluatedReport.get();
        long liveTaskCount = workItemRepository.countByWeeklyReportIdAndStatus(
                r.getId(), WorkItemStatus.DEVAM_EDIYOR);

        return new DashboardProjectSummary(
                project.getId(), project.getName(), project.getCustomer(), ownerName,
                project.getStatus(), true, r.getYear(), r.getWeekNumber(),
                r.getProgressStage(), r.getProgressStage().getPercentage(),
                r.getOverallStatus(), r.getRiskLevel(), liveTaskCount, delayPolicy.isDelayed(r),
                tracking.openItems(), tracking.overdueItems(), tracking.highestRiskLevel());
    }

    /** Referans yoksa en guncel rapor, varsa anahtari referansa kadar (dahil) olan en son rapor. */
    private Optional<WeeklyReport> findEvaluatedReport(Long projectId, Integer referenceKey) {
        if (referenceKey == null) {
            return weeklyReportRepository.findTopByProjectIdOrderByYearDescWeekNumberDesc(projectId);
        }
        return weeklyReportRepository.findLatestByProjectIdUpTo(projectId, referenceKey);
    }
}
