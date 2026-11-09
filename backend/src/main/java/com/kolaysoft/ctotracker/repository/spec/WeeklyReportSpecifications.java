package com.kolaysoft.ctotracker.repository.spec;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import com.kolaysoft.ctotracker.dto.filter.ReportFilter;
import com.kolaysoft.ctotracker.entity.WeeklyReport;

/**
 * Haftalık rapor sorgusu için filtre koşulları (filtre sözleşmesi, bölüm 3).
 *
 * <p>Her metot tek bir koşul üretir; değer verilmemişse koşul kısıtlama yapmaz.
 * Aralık koşullarında uçlar dahildir. Hafta aralığı {@link #inYear} ile birlikte o yılın içinde
 * uygulanır; yılsız hafta parametresine {@code ReportFilter} izin vermez (V2).
 */
public final class WeeklyReportSpecifications {

    private WeeklyReportSpecifications() {
    }

    /** Rapor filtresindeki tüm koşulların VE ile birleşimi. */
    public static Specification<WeeklyReport> matching(ReportFilter filter) {
        return Specification.allOf(
                forProject(filter.projectId()),
                ownedBy(filter.ownerId()),
                inYear(filter.year()),
                weekFrom(filter.weekFrom()),
                weekTo(filter.weekTo()),
                dateFrom(filter.dateFrom()),
                dateTo(filter.dateTo()),
                in("overallStatus", filter.overallStatuses()),
                in("riskLevel", filter.riskLevels()),
                in("progressStage", filter.progressStages()));
    }

    public static Specification<WeeklyReport> forProject(Long projectId) {
        if (projectId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    /** Raporun ait olduğu projenin sorumlu proje yöneticisi. */
    public static Specification<WeeklyReport> ownedBy(Long ownerId) {
        if (ownerId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("project").get("owner").get("id"), ownerId);
    }

    /** Raporun yılı (anlattığı haftanın ISO yılı, bkz. {@code WeeklyReport#yearOf}). */
    public static Specification<WeeklyReport> inYear(Integer year) {
        if (year == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("year"), year);
    }

    public static Specification<WeeklyReport> weekFrom(Integer weekFrom) {
        if (weekFrom == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("weekNumber"), weekFrom);
    }

    public static Specification<WeeklyReport> weekTo(Integer weekTo) {
        if (weekTo == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("weekNumber"), weekTo);
    }

    public static Specification<WeeklyReport> dateFrom(LocalDate dateFrom) {
        if (dateFrom == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("reportDate"), dateFrom);
    }

    public static Specification<WeeklyReport> dateTo(LocalDate dateTo) {
        if (dateTo == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("reportDate"), dateTo);
    }

    /** Enum alanı verilen değerlerden biri mi (değerler arasında VEYA, KF-02). */
    private static Specification<WeeklyReport> in(String attribute, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> root.get(attribute).in(values);
    }
}
