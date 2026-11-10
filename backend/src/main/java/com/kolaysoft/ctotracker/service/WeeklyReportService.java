package com.kolaysoft.ctotracker.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.common.exception.BusinessRuleException;
import com.kolaysoft.ctotracker.common.exception.DuplicateResourceException;
import com.kolaysoft.ctotracker.common.exception.InvalidParameterException;
import com.kolaysoft.ctotracker.common.exception.ResourceNotFoundException;
import com.kolaysoft.ctotracker.common.web.SortParameter;
import com.kolaysoft.ctotracker.dto.PageResponse;
import com.kolaysoft.ctotracker.dto.WeeklyReportRequest;
import com.kolaysoft.ctotracker.dto.WeeklyReportResponse;
import com.kolaysoft.ctotracker.dto.filter.ReportFilter;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
import com.kolaysoft.ctotracker.repository.WeeklyReportRepository;
import com.kolaysoft.ctotracker.repository.WorkItemRepository;
import com.kolaysoft.ctotracker.repository.spec.WeeklyReportSpecifications;
import com.kolaysoft.ctotracker.security.CurrentUser;

/**
 * Haftalik rapor CRUD is mantigi ve is kurallari (on analiz H-03/H-04/H-05).
 *
 * <p>Uygulanan kurallar:
 * <ul>
 *   <li>Bir proje bir hafta icin yalnizca bir rapor alabilir (409 Conflict).</li>
 *   <li>Guncellemede ilerleme kademesi ayni kalabilir ya da tam bir sonraki kademeye
 *       gecebilir; geriye alinamaz ve sira atlanamaz (400 Bad Request). Olusturmada
 *       herhangi bir kademe kabul edilir (rapor, projenin o anki durumunu kaydeder).</li>
 *   <li>Ilerleme yuzdesi kademeden turer; ayrica saklanmaz.</li>
 *   <li>Bulunamayan proje/rapor 404 doner.</li>
 * </ul>
 */
@Service
@Transactional
public class WeeklyReportService {

    /** Rapor listesinde siralanabilir alanlar: API alan adi -> entity yolu (filtre sozlesmesi, S-03). */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "reportDate", "reportDate",
            "weekNumber", "weekNumber",
            "projectName", "project.name");

    /** Varsayilan siralama: en yeni rapor en ustte. */
    private static final String DEFAULT_SORT = "reportDate,desc";

    /** Rapor, anlattigi haftanin pazartesisinden itibaren 14 gun icinde (o hafta ya da ertesi hafta) yazilir. */
    static final int REPORT_WINDOW_DAYS = 14;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final WeeklyReportRepository weeklyReportRepository;
    private final ProjectRepository projectRepository;
    private final WorkItemRepository workItemRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public WeeklyReportService(WeeklyReportRepository weeklyReportRepository,
                               ProjectRepository projectRepository,
                               WorkItemRepository workItemRepository,
                               CurrentUser currentUser,
                               AuditService auditService) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.projectRepository = projectRepository;
        this.workItemRepository = workItemRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<WeeklyReportResponse> findByProject(Long projectId) {
        getVisibleProjectOrThrow(projectId);
        return weeklyReportRepository.findByProjectId(projectId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WeeklyReportResponse findById(Long reportId) {
        return toResponse(getReportOrThrow(reportId));
    }

    /**
     * Projeler arasi filtreli ve sayfali rapor listesi (filtre sozlesmesi, bolum 3).
     *
     * @param sort {@code alan} veya {@code alan,asc|desc}; null ise {@code reportDate,desc}
     */
    @Transactional(readOnly = true)
    public PageResponse<WeeklyReportResponse> search(ReportFilter filter, int page, int size, String sort) {
        validateRanges(filter);
        Pageable pageable = PageRequest.of(page, size, SortParameter.parse(sort, SORT_FIELDS, DEFAULT_SORT));
        return PageResponse.of(
                weeklyReportRepository.findAll(WeeklyReportSpecifications.matching(filter)
                        .and(WeeklyReportSpecifications.ownedBy(currentUser.viewer().visibleOwnerId())), pageable),
                this::toResponse);
    }

    public WeeklyReportResponse create(Long projectId, WeeklyReportRequest request) {
        Project project = getVisibleProjectOrThrow(projectId);

        int year = validateReportWeek(request);
        if (weeklyReportRepository.existsByProjectIdAndYearAndWeekNumber(projectId, year, request.weekNumber())) {
            throw duplicateWeek(year, request.weekNumber());
        }

        WeeklyReport report = new WeeklyReport();
        report.setProject(project);
        applyRequest(report, request);
        WeeklyReport saved = weeklyReportRepository.save(report);
        auditService.created(AuditSubject.of(saved));
        return toResponse(saved);
    }

    public WeeklyReportResponse update(Long reportId, WeeklyReportRequest request) {
        WeeklyReport report = getReportOrThrow(reportId);

        // Yil ya da hafta degistiyse, yeni yil + hafta ayni projede baska bir raporla cakismasin.
        int year = validateReportWeek(request);
        boolean weekChanged = report.getYear() != year || !report.getWeekNumber().equals(request.weekNumber());
        if (weekChanged && weeklyReportRepository.existsByProjectIdAndYearAndWeekNumber(
                report.getProject().getId(), year, request.weekNumber())) {
            throw duplicateWeek(year, request.weekNumber());
        }

        validateStageTransition(report.getProgressStage(), request.progressStage());
        AuditSubject before = AuditSubject.of(report);
        applyRequest(report, request);
        WeeklyReport saved = weeklyReportRepository.save(report);
        auditService.updated(before, AuditSubject.of(saved));
        return toResponse(saved);
    }

    /** Rapor is kalemleriyle birlikte silinir; gecmise yalnizca raporun silinmesi yazilir. */
    public void delete(Long reportId) {
        WeeklyReport report = getReportOrThrow(reportId);
        auditService.deleted(AuditSubject.of(report));
        weeklyReportRepository.delete(report);
    }

    /** Ters araliklar 400 doner; hata araligin bitis parametresinde raporlanir (filtre sozlesmesi, bolum 5). */
    private void validateRanges(ReportFilter filter) {
        if (filter.weekFrom() != null && filter.weekTo() != null && filter.weekFrom() > filter.weekTo()) {
            throw new InvalidParameterException("weekTo",
                    "Bitiş haftası (weekTo) başlangıç haftasından (weekFrom) küçük olamaz.");
        }
        if (filter.dateFrom() != null && filter.dateTo() != null && filter.dateFrom().isAfter(filter.dateTo())) {
            throw new InvalidParameterException("dateTo",
                    "Bitiş tarihi (dateTo) başlangıç tarihinden (dateFrom) önce olamaz.");
        }
    }

    /**
     * Hafta ile rapor tarihinin birbirine uydugunu dogrular ve anlatilan haftanin yilini doner. Rapor,
     * anlattigi haftanin icinde ya da en gec ertesi haftasinda yazilir (seed verisinde ertesi pazartesi);
     * boylece yil sinirinda bile hangi yilin haftasi oldugu tektir. Hatalar alana bagli 400 VALIDATION_ERROR olur.
     */
    private int validateReportWeek(WeeklyReportRequest request) {
        int week = request.weekNumber();
        LocalDate reportDate = request.reportDate();
        int year = WeeklyReport.yearOf(week, reportDate);
        if (!WeeklyReport.hasWeek(year, week)) {
            throw new InvalidParameterException("weekNumber",
                    "Rapor tarihinin yılında ve komşu yıllarda %d. hafta yok.".formatted(week));
        }
        LocalDate monday = WeeklyReport.mondayOf(year, week);
        if (reportDate.isBefore(monday)) {
            throw new InvalidParameterException("reportDate",
                    "Rapor tarihi, anlattığı haftadan (%d / %d. hafta, %s) önce olamaz."
                            .formatted(year, week, DATE_FORMAT.format(monday)));
        }
        LocalDate lastDay = monday.plusDays(REPORT_WINDOW_DAYS - 1);
        if (reportDate.isAfter(lastDay)) {
            throw new InvalidParameterException("reportDate",
                    "Rapor en geç anlattığı haftanın ertesi haftasında yazılabilir (%d / %d. hafta için son gün %s)."
                            .formatted(year, week, DATE_FORMAT.format(lastDay)));
        }
        return year;
    }

    /** Kademe ayni kalabilir ya da tam bir adim ilerleyebilir; geriye/atlayarak gecis reddedilir. */
    private void validateStageTransition(ProgressStage current, ProgressStage next) {
        int diff = next.ordinal() - current.ordinal();
        if (diff < 0) {
            throw new BusinessRuleException(
                    "İlerleme kademesi geriye alınamaz: %s -> %s.".formatted(current, next));
        }
        if (diff > 1) {
            throw new BusinessRuleException(
                    "İlerleme kademesinde sıra atlanamaz; yalnızca bir sonraki kademeye geçilebilir: %s -> %s."
                            .formatted(current, next));
        }
    }

    private static DuplicateResourceException duplicateWeek(Integer year, Integer weekNumber) {
        return new DuplicateResourceException(
                "Bu proje için %d yılının %d. haftasına ait rapor zaten mevcut.".formatted(year, weekNumber));
    }

    private void applyRequest(WeeklyReport report, WeeklyReportRequest request) {
        report.setWeekNumber(request.weekNumber());
        report.setReportDate(request.reportDate());
        report.setProgressStage(request.progressStage());
        report.setOverallStatus(request.overallStatus());
        report.setRiskLevel(request.riskLevel());
        report.setDone(request.done());
        report.setPlanned(request.planned());
        report.setRisks(request.risks());
        report.setNote(request.note());
    }

    /** Rapor yoksa ya da projesi oturumdaki kullaniciya gorunmuyorsa ayni 404. */
    private WeeklyReport getReportOrThrow(Long reportId) {
        return weeklyReportRepository.findById(reportId)
                .filter(report -> currentUser.viewer().canSee(report.getProject()))
                .orElseThrow(() -> new ResourceNotFoundException("Haftalık rapor", reportId));
    }

    private Project getVisibleProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .filter(project -> currentUser.viewer().canSee(project))
                .orElseThrow(() -> new ResourceNotFoundException("Proje", projectId));
    }

    private WeeklyReportResponse toResponse(WeeklyReport r) {
        // Canli task: durumu DEVAM_EDIYOR olan is kalemleri (on analiz H-05); ayri alan tutulmaz.
        long liveTaskCount = workItemRepository.countByWeeklyReportIdAndStatus(
                r.getId(), WorkItemStatus.DEVAM_EDIYOR);
        return new WeeklyReportResponse(
                r.getId(),
                r.getProject().getId(),
                r.getProject().getName(),
                r.getYear(),
                r.getWeekNumber(),
                r.getReportDate(),
                r.getProgressStage(),
                r.getProgressStage().getPercentage(),
                liveTaskCount,
                r.getOverallStatus(),
                r.getRiskLevel(),
                r.getDone(),
                r.getPlanned(),
                r.getRisks(),
                r.getNote());
    }
}
