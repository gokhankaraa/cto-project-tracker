package com.kolaysoft.ctotracker.repository.spec;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.kolaysoft.ctotracker.dto.filter.TrackingItemFilter;
import com.kolaysoft.ctotracker.entity.TrackingItem;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Risk / engel / karar / aksiyon sorgusu icin filtre kosullari (filtre sozlesmesi, bolum 4a).
 * Her metot tek bir kosul uretir; deger verilmemisse kosul kisitlama yapmaz.
 */
public final class TrackingItemSpecifications {

    private TrackingItemSpecifications() {
    }

    /** Filtredeki tum kosullarin VE ile birlesimi. Cok degerli parametrelerin degerleri VEYA ile birlesir. */
    public static Specification<TrackingItem> matching(TrackingItemFilter filter, LocalDate today) {
        return Specification.allOf(
                forProject(filter.projectId()),
                in("type", filter.types()),
                in("status", filter.statuses()),
                in("riskLevel", filter.riskLevels()),
                overdue(filter.overdue(), today),
                assigneeContains(filter.assignee()));
    }

    /** Kaydin projesinin sorumlu proje yoneticisi (gorunurluk, bkz. Viewer). */
    public static Specification<TrackingItem> projectOwnedBy(Long ownerId) {
        if (ownerId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("project").get("owner").get("id"), ownerId);
    }

    public static Specification<TrackingItem> forProject(Long projectId) {
        if (projectId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    private static Specification<TrackingItem> in(String attribute, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> root.get(attribute).in(values);
    }

    /**
     * Gecikme kosulu: kayit kapali degil ve takip tarihi bugunden once. {@code TrackingItemService}'teki
     * kuralin sorgu karsiligidir; biri degisirse digeri de degismelidir. Takip tarihi olmayan (kapali) kayit
     * icin {@code isNotNull} sart: SQL'de NULL karsilastirmasi {@code false} sorgusunda bu kayitlari da
     * disarida birakirdi.
     */
    public static Specification<TrackingItem> overdue(Boolean overdue, LocalDate today) {
        if (overdue == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Path<LocalDate> followUp = root.get("followUpDate");
            Predicate isOverdue = cb.and(
                    cb.notEqual(root.get("status"), TrackingItemStatus.KAPANDI),
                    cb.isNotNull(followUp),
                    cb.lessThan(followUp, today));
            return overdue ? isOverdue : cb.not(isOverdue);
        };
    }

    /** Sorumlu metninde buyuk/kucuk harfe duyarsiz "icerir" aramasi (is kalemlerindekiyle ayni). */
    public static Specification<TrackingItem> assigneeContains(String text) {
        if (text == null || text.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + WorkItemSpecifications.escapeLike(text.strip().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("assignee")), pattern, '\\');
    }
}
