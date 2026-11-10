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

import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;

/** Filtreli sorgular icin bkz. {@code repository.spec.WorkItemSpecifications}. */
public interface WorkItemRepository extends JpaRepository<WorkItem, Long>, JpaSpecificationExecutor<WorkItem> {

    /** Verilen raporlarin is kalemleri (dashboard: canli task ve gecikme bellekte, tek sorguyla). */
    List<WorkItem> findByWeeklyReportIdIn(Collection<Long> reportIds);

    /** Verilen projelerin raporlarindaki tum is kalemleri (trend: gecikme kurali bellekte, tek sorguyla). */
    List<WorkItem> findByWeeklyReportProjectIdIn(Collection<Long> projectIds);

    /** Bir haftalık rapora bağlı iş kalemleri. */
    List<WorkItem> findByWeeklyReportId(Long weeklyReportId);

    /** Canlı task hesabı için: bir raporda belirli durumdaki iş kalemi sayısı (on analiz H-05). */
    long countByWeeklyReportIdAndStatus(Long weeklyReportId, WorkItemStatus status);

    /** Rapor listesi: sayfadaki raporlarin belirli durumdaki is kalemi sayilari tek sorguda (sayisi 0 olan donmez). */
    @Query("""
            select w.weeklyReport.id as reportId, count(w) as count from WorkItem w
            where w.weeklyReport.id in :reportIds and w.status = :status
            group by w.weeklyReport.id
            """)
    List<ReportCount> countByWeeklyReportIdInAndStatus(Collection<Long> reportIds, WorkItemStatus status);

    /** Rapor basina sayi. */
    interface ReportCount {
        Long getReportId();

        long getCount();
    }

    /**
     * Projeler arasi is kalemi listesi: rapor ve projesi ayni sorguda gelir (satir basina iki sorgu yerine). Toplam
     * sayisi sorgusu bundan etkilenmez.
     */
    @Override
    @EntityGraph(attributePaths = {"weeklyReport", "weeklyReport.project"})
    Page<WorkItem> findAll(Specification<WorkItem> spec, Pageable pageable);
}
