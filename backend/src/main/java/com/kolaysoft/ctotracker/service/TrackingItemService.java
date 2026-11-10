package com.kolaysoft.ctotracker.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.common.exception.InvalidParameterException;
import com.kolaysoft.ctotracker.common.exception.ResourceNotFoundException;
import com.kolaysoft.ctotracker.common.web.SortParameter;
import com.kolaysoft.ctotracker.dto.PageResponse;
import com.kolaysoft.ctotracker.dto.TrackingItemRequest;
import com.kolaysoft.ctotracker.dto.TrackingItemResponse;
import com.kolaysoft.ctotracker.dto.filter.TrackingItemFilter;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.TrackingItem;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
import com.kolaysoft.ctotracker.repository.TrackingItemRepository;
import com.kolaysoft.ctotracker.repository.spec.TrackingItemSpecifications;
import com.kolaysoft.ctotracker.security.CurrentUser;

/**
 * Risk, engel, karar ve aksiyon kayitlari (takip kayitlari).
 *
 * <ul>
 *   <li>Risk seviyesi risk ve engel kayitlarinda zorunlu, karar ve aksiyon kayitlarinda verilmez.</li>
 *   <li>Kapali olmayan kaydin sorumlusu ve takip tarihi zorunludur.</li>
 *   <li>Yeni ya da degistirilen takip tarihi bugunden once olamaz. Degistirilmeyen eski tarih korunur:
 *       gecikmis kaydin aciklamasi tarihi ertelemeden duzenlenebilir.</li>
 *   <li>Kapanis tarihini sunucu yazar (kayit kapaninca bugun), kayit yeniden acilinca siler.</li>
 *   <li>Gecikme: kayit kapali degil ve takip tarihi bugunden once. Is kalemlerinden farkli olarak
 *       referans bugundur, cunku takip kaydi bir haftaya bagli degildir.</li>
 * </ul>
 *
 * Kayitlarin gorunurlugu projeninkiyle aynidir: proje yoneticisi baskasinin projesindeki kayda 404 alir.
 */
@Service
@Transactional
public class TrackingItemService {

    /** Proje detayindaki sira: acik kayitlar en yakin takip tarihine gore ustte, kapalilar en altta. */
    private static final Comparator<TrackingItem> PROJECT_ORDER = Comparator
            .comparing((TrackingItem item) -> !item.getStatus().isOpen())
            .thenComparing(TrackingItem::getFollowUpDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TrackingItem::getId);

    /** Projeler arasi listede siralanabilir alanlar: API alan adi -> entity yolu (filtre sozlesmesi, S-03). */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "followUpDate", "followUpDate",
            "createdAt", "createdAt",
            "title", "title",
            "projectName", "project.name");

    /** Varsayilan: en yakin takip tarihi ustte; takip tarihi olmayan (kapali) kayitlar sonda. */
    private static final String DEFAULT_SORT = "followUpDate,asc";

    private final TrackingItemRepository trackingItemRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final AuditService auditService;

    public TrackingItemService(TrackingItemRepository trackingItemRepository,
                               ProjectRepository projectRepository,
                               CurrentUser currentUser,
                               Clock clock,
                               AuditService auditService) {
        this.trackingItemRepository = trackingItemRepository;
        this.projectRepository = projectRepository;
        this.currentUser = currentUser;
        this.clock = clock;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TrackingItemResponse> findByProject(Long projectId) {
        getVisibleProjectOrThrow(projectId);
        LocalDate today = LocalDate.now(clock);
        return trackingItemRepository.findByProjectId(projectId).stream()
                .sorted(PROJECT_ORDER)
                .map(item -> toResponse(item, today))
                .toList();
    }

    /**
     * Projeler arasi filtreli ve sayfali liste (filtre sozlesmesi, bolum 4a). Proje yoneticisi yalnizca kendi
     * projelerinin kayitlarini gorur; kosul filtrelerle VE ile birlesir.
     *
     * @param sort {@code alan} veya {@code alan,asc|desc}; null ise {@code followUpDate,asc}
     */
    @Transactional(readOnly = true)
    public PageResponse<TrackingItemResponse> search(TrackingItemFilter filter, int page, int size, String sort) {
        LocalDate today = LocalDate.now(clock);
        Pageable pageable = PageRequest.of(page, size, SortParameter.parse(sort, SORT_FIELDS, DEFAULT_SORT));
        return PageResponse.of(
                trackingItemRepository.findAll(TrackingItemSpecifications.matching(filter, today)
                        .and(TrackingItemSpecifications.projectOwnedBy(currentUser.viewer().visibleOwnerId())),
                        pageable),
                item -> toResponse(item, today));
    }

    /**
     * Verilen projelerin kapanmamis kayitlarinin ozeti (dashboard). Kaydi olmayan proje haritada yer almaz;
     * cagiran {@link TrackingSummary#EMPTY} kullanir. Gorunurluk cagiranin sorumlulugundadir: dashboard
     * yalnizca kullanicinin gorebildigi projelerin id'lerini verir.
     */
    @Transactional(readOnly = true)
    public Map<Long, TrackingSummary> openSummaries(Collection<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        LocalDate today = LocalDate.now(clock);
        return trackingItemRepository.findByProjectIdInAndStatusNot(projectIds, TrackingItemStatus.KAPANDI).stream()
                .collect(Collectors.groupingBy(item -> item.getProject().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), items -> summarize(items, today))));
    }

    /**
     * Verilen projelerin kapanmamis kayitlari, en yakin takip tarihi once (Excel ciktisi). Gorunurluk cagiranin
     * sorumlulugundadir: yalnizca kullanicinin gorebildigi projelerin id'leri verilir.
     */
    @Transactional(readOnly = true)
    public List<TrackingItemResponse> openItemsOf(Collection<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now(clock);
        return trackingItemRepository.findByProjectIdInAndStatusNot(projectIds, TrackingItemStatus.KAPANDI).stream()
                .sorted(PROJECT_ORDER)
                .map(item -> toResponse(item, today))
                .toList();
    }

    private TrackingSummary summarize(List<TrackingItem> openItems, LocalDate today) {
        RiskLevel highest = openItems.stream()
                .map(TrackingItem::getRiskLevel)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new TrackingSummary(
                openItems.size(),
                openItems.stream().filter(item -> isOverdue(item, today)).count(),
                countType(openItems, TrackingItemType.RISK),
                countType(openItems, TrackingItemType.ENGEL),
                countType(openItems, TrackingItemType.KARAR),
                highest);
    }

    private static long countType(List<TrackingItem> items, TrackingItemType type) {
        return items.stream().filter(item -> item.getType() == type).count();
    }

    @Transactional(readOnly = true)
    public TrackingItemResponse findById(Long id) {
        return toResponse(getItemOrThrow(id), LocalDate.now(clock));
    }

    public TrackingItemResponse create(Long projectId, TrackingItemRequest request) {
        Project project = getVisibleProjectOrThrow(projectId);
        LocalDate today = LocalDate.now(clock);
        validate(request, null, today);

        TrackingItem item = new TrackingItem();
        item.setProject(project);
        item.setCreatedAt(Instant.now(clock));
        apply(item, request, today);
        TrackingItem saved = trackingItemRepository.save(item);
        auditService.created(AuditSubject.of(saved));
        return toResponse(saved, today);
    }

    public TrackingItemResponse update(Long id, TrackingItemRequest request) {
        TrackingItem item = getItemOrThrow(id);
        LocalDate today = LocalDate.now(clock);
        validate(request, item.getFollowUpDate(), today);
        AuditSubject before = AuditSubject.of(item);
        apply(item, request, today);
        TrackingItem saved = trackingItemRepository.save(item);
        auditService.updated(before, AuditSubject.of(saved));
        return toResponse(saved, today);
    }

    public void delete(Long id) {
        TrackingItem item = getItemOrThrow(id);
        auditService.deleted(AuditSubject.of(item));
        trackingItemRepository.delete(item);
    }

    /**
     * Alanlar arasi kurallar; ihlal edilen ilk kural alan adiyla 400 doner (formda alanin altinda gorunur).
     *
     * @param storedFollowUpDate guncellemede kayitli takip tarihi (olusturmada null)
     */
    private void validate(TrackingItemRequest request, LocalDate storedFollowUpDate, LocalDate today) {
        if (request.type().hasRiskLevel() && request.riskLevel() == null) {
            throw new InvalidParameterException("riskLevel", "Risk ve engel kayıtlarında risk seviyesi zorunludur.");
        }
        if (!request.type().hasRiskLevel() && request.riskLevel() != null) {
            throw new InvalidParameterException("riskLevel",
                    "Risk seviyesi yalnızca risk ve engel kayıtlarında girilir.");
        }
        if (!request.status().isOpen()) {
            return;
        }
        if (request.assignee() == null || request.assignee().isBlank()) {
            throw new InvalidParameterException("assignee", "Kapanmamış kaydın sorumlusu zorunludur.");
        }
        LocalDate followUp = request.followUpDate();
        if (followUp == null) {
            throw new InvalidParameterException("followUpDate", "Kapanmamış kaydın takip tarihi zorunludur.");
        }
        if (followUp.isBefore(today) && !followUp.equals(storedFollowUpDate)) {
            throw new InvalidParameterException("followUpDate", "Takip tarihi bugünden önce olamaz.");
        }
    }

    private void apply(TrackingItem item, TrackingItemRequest request, LocalDate today) {
        item.setType(request.type());
        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setRiskLevel(request.riskLevel());
        item.setImpact(request.impact());
        item.setActionPlan(request.actionPlan());
        item.setAssignee(request.assignee() == null || request.assignee().isBlank() ? null : request.assignee().trim());
        item.setFollowUpDate(request.followUpDate());

        boolean wasClosed = item.getStatus() != null && !item.getStatus().isOpen();
        item.setStatus(request.status());
        if (!request.status().isOpen()) {
            // Kapali kalan kaydin ilk kapanis tarihi korunur.
            item.setClosedDate(wasClosed ? Objects.requireNonNullElse(item.getClosedDate(), today) : today);
        } else {
            item.setClosedDate(null);
        }
    }

    private boolean isOverdue(TrackingItem item, LocalDate today) {
        return item.getStatus().isOpen() && item.getFollowUpDate() != null && item.getFollowUpDate().isBefore(today);
    }

    /** Kayit yoksa ya da projesi oturumdaki kullaniciya gorunmuyorsa ayni 404. */
    private TrackingItem getItemOrThrow(Long id) {
        return trackingItemRepository.findById(id)
                .filter(item -> currentUser.viewer().canSee(item.getProject()))
                .orElseThrow(() -> new ResourceNotFoundException("Takip kaydı", id));
    }

    private Project getVisibleProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .filter(project -> currentUser.viewer().canSee(project))
                .orElseThrow(() -> new ResourceNotFoundException("Proje", projectId));
    }

    private TrackingItemResponse toResponse(TrackingItem item, LocalDate today) {
        Project project = item.getProject();
        return new TrackingItemResponse(
                item.getId(),
                project.getId(),
                project.getName(),
                item.getType(),
                item.getTitle(),
                item.getDescription(),
                item.getRiskLevel(),
                item.getImpact(),
                item.getActionPlan(),
                item.getAssignee(),
                item.getFollowUpDate(),
                item.getStatus(),
                item.getClosedDate(),
                item.getCreatedAt(),
                isOverdue(item, today));
    }
}
