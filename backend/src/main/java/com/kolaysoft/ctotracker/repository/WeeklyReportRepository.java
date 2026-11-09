package com.kolaysoft.ctotracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.kolaysoft.ctotracker.entity.WeeklyReport;

/** Filtreli sorgular icin bkz. {@code repository.spec.WeeklyReportSpecifications}. */
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long>,
        JpaSpecificationExecutor<WeeklyReport> {

    /** Bir projenin tum haftalik raporlari. */
    List<WeeklyReport> findByProjectId(Long projectId);

    /** Projenin hic raporu var mi (proje silme guard'i icin). */
    boolean existsByProjectId(Long projectId);

    /** Projenin en guncel raporu: en yeni yil, o yil icinde en yuksek hafta (dashboard icin). */
    Optional<WeeklyReport> findTopByProjectIdOrderByYearDescWeekNumberDesc(Long projectId);

    /**
     * Projenin referans noktaya kadarki (dahil) en son raporu: dashboard'da {@code year}/{@code weekNumber}
     * icin (V2). Anahtar {@code year * 100 + weekNumber}'dir (bkz. {@link WeeklyReport#weekKey});
     * or. 202629 "2026'nin 29. haftasina kadar" demektir ve onceki yillarin raporlarini da kapsar.
     */
    @Query("""
            select r from WeeklyReport r
            where r.project.id = :projectId and r.year * 100 + r.weekNumber <= :referenceKey
            order by r.year desc, r.weekNumber desc
            limit 1
            """)
    Optional<WeeklyReport> findLatestByProjectIdUpTo(Long projectId, int referenceKey);

    /** Proje + yil + hafta benzersizlik kontrolu icin (on analiz H-03, V2). */
    boolean existsByProjectIdAndYearAndWeekNumber(Long projectId, Integer year, Integer weekNumber);
}
