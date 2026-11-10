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
 * İş kalemi sorgusu için filtre koşulları (filtre sözleşmesi, bölüm 4).
 *
 * <p>Her metot tek bir koşul üretir; değer verilmemişse koşul kısıtlama yapmaz.
 */
public final class WorkItemSpecifications {

    private WorkItemSpecifications() {
    }

    /** İş kalemi filtresindeki tüm koşulların VE ile birleşimi; her zaman değerlendirilen rapora göre. */
    public static Specification<WorkItem> matching(WorkItemFilter filter) {
        return Specification.allOf(
                inEvaluatedReport(filter.referenceKey()),
                forProject(filter.projectId()),
                assigneeContains(filter.assignee()),
                statusIn(filter.statuses()),
                delayed(filter.delayed()));
    }

    /**
     * Yalnızca her projenin değerlendirilen raporundaki iş kalemleri (filtre sözleşmesi, S-05):
     * {@code referenceKey} null ise projenin en son raporu, değilse anahtarı ({@link #weekKey})
     * {@code referenceKey}'den büyük olmayan en son raporu. Anahtar yıl ve haftayı birlikte taşıdığı için
     * "2026'nın 29. haftasına kadar" önceki yılların raporlarını da kapsar.
     *
     * @param referenceKey {@code WorkItemFilter#referenceKey()}; or. 2026 h29 için 202629
     */
    public static Specification<WorkItem> inEvaluatedReport(Integer referenceKey) {
        return (root, query, cb) -> {
            Path<WeeklyReport> report = root.get("weeklyReport");

            Subquery<Integer> latestKey = query.subquery(Integer.class);
            Root<WeeklyReport> candidate = latestKey.from(WeeklyReport.class);
            Predicate sameProject = cb.equal(
                    candidate.get("project").get("id"), report.get("project").get("id"));
            Predicate upToReference = referenceKey == null
                    ? cb.conjunction()
                    : cb.le(weekKey(cb, candidate), referenceKey);
            latestKey.select(cb.max(weekKey(cb, candidate))).where(sameProject, upToReference);

            return cb.equal(weekKey(cb, report), latestKey);
        };
    }

    /**
     * {@link WeeklyReport#weekKey(int, int)} kuralının sorgu karşılığı: {@code year * 100 + weekNumber}.
     * Hafta en fazla 53 olduğu için bu sayının sırası (yıl, hafta) sırasıyla aynıdır; böylece
     * "en son rapor" tek bir MAX ile bulunur.
     */
    static Expression<Integer> weekKey(CriteriaBuilder cb, Path<WeeklyReport> report) {
        return cb.sum(cb.prod(report.<Integer>get("year"), 100), report.<Integer>get("weekNumber"));
    }

    public static Specification<WorkItem> inReport(Long reportId) {
        return (root, query, cb) -> cb.equal(root.get("weeklyReport").get("id"), reportId);
    }

    /** İş kaleminin bağlı olduğu projenin sorumlu proje yöneticisi (görünürlük, bkz. Viewer). */
    public static Specification<WorkItem> projectOwnedBy(Long ownerId) {
        if (ownerId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("weeklyReport").get("project").get("owner").get("id"), ownerId);
    }

    public static Specification<WorkItem> forProject(Long projectId) {
        if (projectId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("weeklyReport").get("project").get("id"), projectId);
    }

    /** Sorumlu metninde büyük/küçük harfe duyarsız "içerir" araması (KF-09). */
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
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
