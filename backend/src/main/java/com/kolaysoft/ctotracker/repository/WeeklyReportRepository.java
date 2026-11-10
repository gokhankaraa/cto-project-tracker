package com.kolaysoft.ctotracker.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.kolaysoft.ctotracker.entity.WeeklyReport;

/** Filtreli sorgular icin bkz. {@code repository.spec.WeeklyReportSpecifications}. */
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long>,
        JpaSpecificationExecutor<WeeklyReport> {

    /** Bir projenin tum haftalik raporlari. */
    List<WeeklyReport> findByProjectId(Long projectId);

    /** Verilen projelerin tum raporlari (trend: tek sorguda). */
    List<WeeklyReport> findByProjectIdIn(Collection<Long> projectIds);

    /** Projenin hic raporu var mi (proje silme guard'i icin). */
    boolean existsByProjectId(Long projectId);

    /**
     * Her projenin referans noktaya kadarki (dahil) en son raporu, tek sorguda (dashboard; proje basina sorgu atilmaz).
     * Anahtar {@code year * 100 + weekNumber}'dir (bkz. {@link WeeklyReport#weekKey}); or. 202629 "2026'nin 29. haftasina
     * kadar" demektir ve onceki yillarin raporlarini da kapsar. Referans yoksa {@link #LATEST} verilir: en guncel rapor
     * (en yeni yil, o yil icinde en yuksek hafta). Raporu olmayan proje sonucta yer almaz.
     */
    // PostgreSQL'in DISTINCT ON'u: proje basina siralamadaki ilk satir. Iliskili alt sorgu (her rapor icin MAX) 200
    // proje x 52 haftada 46 ms, bu 9 ms surdu (README "Performance"). Uygulama yalnizca PostgreSQL ile calisir.
    @Query(value = """
            select distinct on (r.project_id) r.* from weekly_reports r
            where r.project_id in (:projectIds) and r.year * 100 + r.week_number <= :referenceKey
            order by r.project_id, r.year desc, r.week_number desc
            """, nativeQuery = true)
    List<WeeklyReport> findEvaluatedByProjectIdIn(Collection<Long> projectIds, int referenceKey);

    /** {@link #findEvaluatedByProjectIdIn} icin "referans yok": her anahtardan buyuk. */
    int LATEST = Integer.MAX_VALUE;

    /** Proje + yil + hafta benzersizlik kontrolu icin (on analiz H-03, V2). */
    boolean existsByProjectIdAndYearAndWeekNumber(Long projectId, Integer year, Integer weekNumber);

    /** Projeler arasi rapor listesi: proje ayni sorguda gelir (satir basina bir sorgu yerine). */
    @Override
    @EntityGraph(attributePaths = "project")
    Page<WeeklyReport> findAll(Specification<WeeklyReport> spec, Pageable pageable);
}
