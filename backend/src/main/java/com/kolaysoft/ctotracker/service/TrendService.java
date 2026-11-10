package com.kolaysoft.ctotracker.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.config.ClockConfig;
import com.kolaysoft.ctotracker.dto.DashboardTrendResponse;
import com.kolaysoft.ctotracker.dto.TrendWeek;
import com.kolaysoft.ctotracker.dto.filter.TrendFilter;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.TrackingItem;
import com.kolaysoft.ctotracker.entity.TrackingItemType;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
import com.kolaysoft.ctotracker.repository.TrackingItemRepository;
import com.kolaysoft.ctotracker.repository.WeeklyReportRepository;
import com.kolaysoft.ctotracker.repository.WorkItemRepository;
import com.kolaysoft.ctotracker.repository.spec.ProjectSpecifications;
import com.kolaysoft.ctotracker.security.CurrentUser;

/**
 * Portfoyun haftalik trendi (filtre sozlesmesi, bolum 2a).
 *
 * <p>Her hafta icin dashboard'daki hafta referansi kurali uygulanir: her proje o haftaya kadarki (dahil) en son
 * raporuyla degerlendirilir. Boylece trendin son noktasi, ayni referansla acilan dashboard'un sayaclariyla aynidir.
 *
 * <p>Risk / engel / karar / aksiyon icin hafta sonunda acik olan kayit: o haftanin sonundan once olusturulmus ve o
 * gune kadar kapanmamis. Kayitlarin degisiklik gecmisi tutulmadigi icin risk seviyesinin ya da takip tarihinin
 * haftalar icindeki degisimi izlenemez; bu yuzden trendde seviye kirilimi ve "gecikmis takip" yoktur.
 *
 * <p>Veri uc sorguyla yuklenir (raporlar, is kalemleri, takip kayitlari), hesap bellekte yapilir; proje ve hafta
 * sayisi arttikca sorgu sayisi artmaz.
 */
@Service
@Transactional(readOnly = true)
public class TrendService {

    public static final int DEFAULT_WEEKS = 12;
    public static final int MAX_WEEKS = 52;

    private final ProjectRepository projectRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final WorkItemRepository workItemRepository;
    private final TrackingItemRepository trackingItemRepository;
    private final DelayPolicy delayPolicy;
    private final CurrentUser currentUser;
    private final Clock clock;

    public TrendService(ProjectRepository projectRepository, WeeklyReportRepository weeklyReportRepository,
                        WorkItemRepository workItemRepository, TrackingItemRepository trackingItemRepository,
                        DelayPolicy delayPolicy, CurrentUser currentUser, Clock clock) {
        this.projectRepository = projectRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.workItemRepository = workItemRepository;
        this.trackingItemRepository = trackingItemRepository;
        this.delayPolicy = delayPolicy;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    /** ISO yili ve haftasi. */
    private record YearWeek(int year, int week) {

        static YearWeek of(LocalDate date) {
            return new YearWeek(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        }

        LocalDate monday() {
            return WeeklyReport.mondayOf(year, week);
        }

        int key() {
            return WeeklyReport.weekKey(year, week);
        }
    }

    public DashboardTrendResponse getTrend(TrendFilter filter) {
        List<YearWeek> weeks = window(filter);

        List<Project> projects = projectRepository.findAll(ProjectSpecifications.hasId(filter.projectId())
                .and(ProjectSpecifications.statusIn(filter.projectStatuses()))
                .and(ProjectSpecifications.ownedBy(filter.ownerId()))
                // Gorunurluk: proje yoneticisi yalnizca kendi projelerinin trendini gorur.
                .and(ProjectSpecifications.ownedBy(currentUser.viewer().visibleOwnerId())), Sort.by("id"));
        if (projects.isEmpty()) {
            return new DashboardTrendResponse(weeks.stream().map(this::emptyWeek).toList());
        }
        List<Long> projectIds = projects.stream().map(Project::getId).toList();

        // Once raporlar: is kalemlerinin rapor iliskisi ayni oturumdaki bu nesnelere baglanir (ek sorgu olmaz).
        Map<Long, List<WeeklyReport>> reportsByProject = weeklyReportRepository.findByProjectIdIn(projectIds).stream()
                .sorted(Comparator.comparingInt(TrendService::keyOf))
                .collect(Collectors.groupingBy(report -> report.getProject().getId()));
        Map<Long, List<WorkItem>> itemsByReport = workItemRepository.findByWeeklyReportProjectIdIn(projectIds).stream()
                .collect(Collectors.groupingBy(item -> item.getWeeklyReport().getId()));
        List<TrackingItem> trackingItems = trackingItemRepository.findByProjectIdIn(projectIds);

        List<TrendWeek> result = new ArrayList<>(weeks.size());
        for (YearWeek week : weeks) {
            List<WeeklyReport> evaluated = projectIds.stream()
                    .map(id -> latestUpTo(reportsByProject.getOrDefault(id, List.of()), week.key()))
                    .filter(Objects::nonNull)
                    .toList();
            result.add(toWeek(week, projects.size(), evaluated, itemsByReport, trackingItems));
        }
        return new DashboardTrendResponse(result);
    }

    /** Referans haftada biten, eskiden yeniye {@code weeks} hafta (yil sinirini gecebilir). */
    private List<YearWeek> window(TrendFilter filter) {
        YearWeek end;
        if (filter.year() == null) {
            end = YearWeek.of(LocalDate.now(clock));
        } else if (filter.weekNumber() == null) {
            end = new YearWeek(filter.year(), WeeklyReport.hasWeek(filter.year(), 53) ? 53 : 52);
        } else {
            end = new YearWeek(filter.year(), filter.weekNumber());
        }
        LocalDate endMonday = end.monday();
        List<YearWeek> weeks = new ArrayList<>(filter.weeks());
        for (int i = filter.weeks() - 1; i >= 0; i--) {
            weeks.add(YearWeek.of(endMonday.minusWeeks(i)));
        }
        return weeks;
    }

    /** Anahtari {@code key}'den buyuk olmayan en son rapor; liste anahtara gore artan siradadir. */
    private static WeeklyReport latestUpTo(List<WeeklyReport> sortedReports, int key) {
        WeeklyReport latest = null;
        for (WeeklyReport report : sortedReports) {
            if (keyOf(report) > key) {
                break;
            }
            latest = report;
        }
        return latest;
    }

    private TrendWeek toWeek(YearWeek week, int totalProjects, List<WeeklyReport> evaluated,
                             Map<Long, List<WorkItem>> itemsByReport, List<TrackingItem> trackingItems) {
        Map<OverallStatus, Long> byStatus = new EnumMap<>(OverallStatus.class);
        for (OverallStatus status : OverallStatus.values()) {
            byStatus.put(status, 0L);
        }
        evaluated.forEach(report -> byStatus.merge(report.getOverallStatus(), 1L, Long::sum));

        long highRisk = evaluated.stream().filter(report -> report.getRiskLevel() == RiskLevel.YUKSEK).count();
        long delayed = evaluated.stream()
                .filter(report -> delayPolicy.isDelayed(report, itemsByReport.getOrDefault(report.getId(), List.of())))
                .count();
        Integer averageProgress = evaluated.isEmpty() ? null : (int) Math.round(evaluated.stream()
                .mapToInt(report -> report.getProgressStage().getPercentage())
                .average()
                .orElse(0));

        // Hafta [pazartesi 00:00, sonraki pazartesi 00:00) Turkiye saatiyle; kapanis tarihi gun olarak tutulur.
        LocalDate monday = week.monday();
        LocalDate sunday = monday.plusDays(6);
        Instant start = monday.atStartOfDay(ClockConfig.ZONE).toInstant();
        Instant end = monday.plusWeeks(1).atStartOfDay(ClockConfig.ZONE).toInstant();
        List<TrackingItem> openAtEnd = trackingItems.stream()
                .filter(item -> item.getCreatedAt().isBefore(end))
                .filter(item -> item.getClosedDate() == null || item.getClosedDate().isAfter(sunday))
                .toList();
        long opened = trackingItems.stream()
                .filter(item -> !item.getCreatedAt().isBefore(start) && item.getCreatedAt().isBefore(end))
                .count();
        long closed = trackingItems.stream()
                .filter(item -> item.getClosedDate() != null
                        && !item.getClosedDate().isBefore(monday) && !item.getClosedDate().isAfter(sunday))
                .count();

        return new TrendWeek(week.year(), week.week(), monday, totalProjects, evaluated.size(), byStatus,
                highRisk, delayed, averageProgress,
                countType(openAtEnd, TrackingItemType.RISK),
                countType(openAtEnd, TrackingItemType.ENGEL),
                countType(openAtEnd, TrackingItemType.KARAR),
                countType(openAtEnd, TrackingItemType.AKSIYON),
                opened, closed);
    }

    private static int keyOf(WeeklyReport report) {
        return WeeklyReport.weekKey(report.getYear(), report.getWeekNumber());
    }

    private TrendWeek emptyWeek(YearWeek week) {
        return toWeek(week, 0, List.of(), Map.of(), List.of());
    }

    private static long countType(List<TrackingItem> items, TrackingItemType type) {
        return items.stream().filter(item -> item.getType() == type).count();
    }
}
