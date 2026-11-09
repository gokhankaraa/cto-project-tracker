package com.kolaysoft.ctotracker.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.common.exception.ResourceNotFoundException;
import com.kolaysoft.ctotracker.common.web.SortParameter;
import com.kolaysoft.ctotracker.dto.PageResponse;
import com.kolaysoft.ctotracker.dto.WorkItemRequest;
import com.kolaysoft.ctotracker.dto.WorkItemResponse;
import com.kolaysoft.ctotracker.dto.filter.WorkItemFilter;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.repository.WeeklyReportRepository;
import com.kolaysoft.ctotracker.repository.WorkItemRepository;
import com.kolaysoft.ctotracker.repository.spec.WorkItemSpecifications;
import com.kolaysoft.ctotracker.security.CurrentUser;

/**
 * İş kalemi CRUD is mantigi (on analiz H-05).
 * Her is kalemi bir haftalik rapora baglidir; baglanti URL yolundan gelir.
 */
@Service
@Transactional
public class WorkItemService {

    /** İş kalemi listesinde sıralanabilir alanlar: API alan adı -> entity yolu (filtre sözleşmesi, S-03). */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "plannedDate", "plannedDate",
            "title", "title",
            "projectName", "weeklyReport.project.name");

    /** Varsayılan sıralama: en yakın planlanan tarih en üstte, tarihsizler sonda. */
    private static final String DEFAULT_SORT = "plannedDate,asc";

    private final WorkItemRepository workItemRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final DelayPolicy delayPolicy;
    private final CurrentUser currentUser;

    public WorkItemService(WorkItemRepository workItemRepository,
                           WeeklyReportRepository weeklyReportRepository,
                           DelayPolicy delayPolicy,
                           CurrentUser currentUser) {
        this.workItemRepository = workItemRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.delayPolicy = delayPolicy;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<WorkItemResponse> findByReport(Long reportId) {
        getVisibleReportOrThrow(reportId);
        return workItemRepository.findByWeeklyReportId(reportId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkItemResponse findById(Long workItemId) {
        return toResponse(getWorkItemOrThrow(workItemId));
    }

    /**
     * Projeler arası filtreli ve sayfalı iş kalemi listesi (filtre sözleşmesi, bölüm 4).
     * Her zaman projelerin değerlendirilen raporuna göre çalışır (S-05).
     *
     * @param sort {@code alan} veya {@code alan,asc|desc}; null ise {@code plannedDate,asc}
     */
    @Transactional(readOnly = true)
    public PageResponse<WorkItemResponse> search(WorkItemFilter filter, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, SortParameter.parse(sort, SORT_FIELDS, DEFAULT_SORT));
        return PageResponse.of(
                workItemRepository.findAll(WorkItemSpecifications.matching(filter)
                        .and(WorkItemSpecifications.projectOwnedBy(currentUser.viewer().visibleOwnerId())), pageable),
                this::toResponse);
    }

    public WorkItemResponse create(Long reportId, WorkItemRequest request) {
        WeeklyReport report = getVisibleReportOrThrow(reportId);

        WorkItem workItem = new WorkItem();
        workItem.setWeeklyReport(report);
        applyRequest(workItem, request);
        return toResponse(workItemRepository.save(workItem));
    }

    public WorkItemResponse update(Long workItemId, WorkItemRequest request) {
        WorkItem workItem = getWorkItemOrThrow(workItemId);
        applyRequest(workItem, request);
        return toResponse(workItemRepository.save(workItem));
    }

    public void delete(Long workItemId) {
        workItemRepository.delete(getWorkItemOrThrow(workItemId));
    }

    private void applyRequest(WorkItem workItem, WorkItemRequest request) {
        workItem.setTitle(request.title());
        workItem.setDescription(request.description());
        workItem.setAssignee(request.assignee());
        workItem.setStatus(request.status());
        workItem.setPlannedDate(request.plannedDate());
        workItem.setCompletedDate(request.completedDate());
    }

    /** Is kalemi yoksa ya da projesi oturumdaki kullaniciya gorunmuyorsa ayni 404. */
    private WorkItem getWorkItemOrThrow(Long workItemId) {
        return workItemRepository.findById(workItemId)
                .filter(item -> currentUser.viewer().canSee(item.getWeeklyReport().getProject()))
                .orElseThrow(() -> new ResourceNotFoundException("Is kalemi", workItemId));
    }

    private WeeklyReport getVisibleReportOrThrow(Long reportId) {
        return weeklyReportRepository.findById(reportId)
                .filter(report -> currentUser.viewer().canSee(report.getProject()))
                .orElseThrow(() -> new ResourceNotFoundException("Haftalik rapor", reportId));
    }

    private WorkItemResponse toResponse(WorkItem w) {
        WeeklyReport report = w.getWeeklyReport();
        Project project = report.getProject();
        return new WorkItemResponse(
                w.getId(),
                report.getId(),
                project.getId(),
                project.getName(),
                report.getYear(),
                report.getWeekNumber(),
                w.getTitle(),
                w.getDescription(),
                w.getAssignee(),
                w.getStatus(),
                w.getPlannedDate(),
                w.getCompletedDate(),
                delayPolicy.isDelayed(w));
    }
}
