package com.kolaysoft.ctotracker.repository.spec;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.kolaysoft.ctotracker.dto.filter.WorkItemFilter;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * İş kalemi sorgusu için filtre koşulları (filtre sözleşmesi T13, bölüm 4).
 *
 * <p>Her metot tek bir koşul üretir; değer verilmemişse koşul kısıtlama yapmaz.
 */
public final class WorkItemSpecifications {

    private WorkItemSpecifications() {
    }

    /** İş kalemi filtresindeki tüm koşulların VE ile birleşimi; her zaman değerlendirilen rapora göre. */
    public static Specification<WorkItem> matching(WorkItemFilter filter) {
        return Specification.allOf(
                inEvaluatedReport(filter.weekNumber()),
                forProject(filter.projectId()),
                assigneeContains(filter.assignee()),
                statusIn(filter.statuses()),
                delayed(filter.delayed()));
    }

    /**
     * Yalnızca her projenin değerlendirilen raporundaki iş kalemleri (filtre sözleşmesi, S-05):
     * {@code weekNumber} yoksa projenin en son raporu, varsa hafta numarası ≤ weekNumber olan
     * en son raporu. "En son" yıl ve hafta birlikte karşılaştırılarak bulunur (bkz. {@link #weekKey}).
     */
    public static Specification<WorkItem> inEvaluatedReport(Integer weekNumber) {
        return (root, query, cb) -> {
            Path<WeeklyReport> report = root.get("weeklyReport");

            Subquery<Integer> latestKey = query.subquery(Integer.class);
            Root<WeeklyReport> candidate = latestKey.from(WeeklyReport.class);
            Predicate sameProject = cb.equal(
                    candidate.get("project").get("id"), report.get("project").get("id"));
            Predicate withinWeek = weekNumber == null
                    ? cb.conjunction()
                    : cb.le(candidate.get("weekNumber"), weekNumber);
            latestKey.select(cb.max(weekKey(cb, candidate))).where(sameProject, withinWeek);

            return cb.equal(weekKey(cb, report), latestKey);
        };
    }

    /**
     * Raporun yıl ve haftasını tek sayıda birleştirir: {@code year * 100 + weekNumber} (or. 2026 h30 = 202630).
     * Hafta en fazla 53 olduğu için bu sayının sırası (yıl, hafta) sırasıyla aynıdır; böylece
     * "en son rapor" tek bir MAX ile bulunur.
     */
    static Expression<Integer> weekKey(CriteriaBuilder cb, Path<WeeklyReport> report) {
        return cb.sum(cb.prod(report.<Integer>get("year"), 100), report.<Integer>get("weekNumber"));
    }

    public static Specification<WorkItem> inReport(Long reportId) {
        return (root, query, cb) -> cb.equal(root.get("weeklyReport").get("id"), reportId);
    }

    public static Specification<WorkItem> forProject(Long projectId) {
        if (projectId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("weeklyReport").get("project").get("id"), projectId);
    }

    /** Sorumlu metninde büyük/küçük harfe duyarsız "içerir" araması (K-T13-09). */
    public static Specification<WorkItem> assigneeContains(String text) {
        if (text == null || text.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + escapeLike(text.strip().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("assignee")), pattern, '\\');
    }

    public static Specification<WorkItem> statusIn(Collection<WorkItemStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    /**
     * Gecikme koşulu: {@code true} yalnızca gecikmiş, {@code false} yalnızca gecikmemiş iş kalemleri.
     *
     * <p>{@link com.kolaysoft.ctotracker.service.DelayPolicy#isDelayed(WorkItem)} kuralının sorgu
     * karşılığıdır; biri değişirse diğeri de değişmelidir. Planlanan tarihi olmayan kayıtlar için
     * {@code isNotNull} kontrolü şart: aksi halde SQL'deki NULL karşılaştırması {@code false} sorgusunda
     * bu kayıtları da dışarıda bırakırdı.
     */
    public static Specification<WorkItem> delayed(Boolean delayed) {
        if (delayed == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Path<WorkItemStatus> status = root.get("status");
            Path<LocalDate> plannedDate = root.get("plannedDate");
            Path<LocalDate> reportDate = root.get("weeklyReport").get("reportDate");

            Predicate isDelayed = cb.or(
                    cb.equal(status, WorkItemStatus.GECIKTI),
                    cb.and(
                            cb.isNotNull(plannedDate),
                            cb.notEqual(status, WorkItemStatus.TAMAMLANDI),
                            cb.lessThan(plannedDate, reportDate)));
            return delayed ? isDelayed : cb.not(isDelayed);
        };
    }

    /** Kullanıcının yazdığı % ve _ karakterleri joker olarak değil, düz karakter olarak aransın. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
