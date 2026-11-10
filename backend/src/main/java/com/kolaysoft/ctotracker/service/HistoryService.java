package com.kolaysoft.ctotracker.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.common.exception.InvalidParameterException;
import com.kolaysoft.ctotracker.common.exception.ResourceNotFoundException;
import com.kolaysoft.ctotracker.dto.AuditEventResponse;
import com.kolaysoft.ctotracker.dto.PageResponse;
import com.kolaysoft.ctotracker.entity.AuditEntityType;
import com.kolaysoft.ctotracker.entity.AuditEvent;
import com.kolaysoft.ctotracker.repository.AuditEventRepository;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
import com.kolaysoft.ctotracker.security.CurrentUser;

/**
 * Degisiklik gecmisini okur (yazan taraf {@link AuditService}). Projeyi gorebilen gecmisini de gorur: proje
 * yoneticisi kendi projelerinin, CTO ve Admin hepsinin. Silinmis projenin gecmisi API'den okunmaz.
 *
 * <p>Siralama sabittir: en yeni olay ustte. Bir sayfanin degisen alanlari tek sorguda yuklenir
 * ({@code AuditEvent.changes} uzerindeki {@code @BatchSize}); sorgu sayisi sayfadaki olay sayisiyla buyumez.
 */
@Service
@Transactional(readOnly = true)
public class HistoryService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));

    private final AuditEventRepository auditEventRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUser currentUser;

    public HistoryService(AuditEventRepository auditEventRepository, ProjectRepository projectRepository,
                          CurrentUser currentUser) {
        this.auditEventRepository = auditEventRepository;
        this.projectRepository = projectRepository;
        this.currentUser = currentUser;
    }

    /**
     * Projenin ve projedeki kayitlarin gecmisi.
     *
     * @param entityType yalnizca bu turdeki kayitlar (null: hepsi)
     * @param entityId   yalnizca bu kayit; id'ler turler arasinda cakisabildigi icin tur olmadan verilemez (400)
     */
    public PageResponse<AuditEventResponse> findByProject(Long projectId, AuditEntityType entityType, Long entityId,
                                                          int page, int size) {
        if (entityId != null && entityType == null) {
            throw new InvalidParameterException("entityId",
                    "Kayıt (entityId) tür (entityType) ile birlikte verilmelidir.");
        }
        projectRepository.findById(projectId)
                .filter(project -> currentUser.viewer().canSee(project))
                .orElseThrow(() -> new ResourceNotFoundException("Proje", projectId));

        Specification<AuditEvent> spec = (root, query, cb) -> cb.equal(root.get("projectId"), projectId);
        if (entityType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), entityType));
        }
        if (entityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityId"), entityId));
        }
        return PageResponse.of(auditEventRepository.findAll(spec, PageRequest.of(page, size, NEWEST_FIRST)),
                HistoryService::toResponse);
    }

    private static AuditEventResponse toResponse(AuditEvent e) {
        return new AuditEventResponse(e.getId(), e.getOccurredAt(), e.getActorId(), e.getActorName(), e.getAction(),
                e.getEntityType(), e.getEntityId(), e.getSummary(),
                e.getChanges().stream()
                        .map(c -> new AuditEventResponse.Change(c.getField(), c.getOldValue(), c.getNewValue()))
                        .toList());
    }
}
