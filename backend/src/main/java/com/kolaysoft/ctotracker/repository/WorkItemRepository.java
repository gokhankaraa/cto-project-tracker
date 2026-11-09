package com.kolaysoft.ctotracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;

/** Filtreli sorgular icin bkz. {@code repository.spec.WorkItemSpecifications} (T13). */
public interface WorkItemRepository extends JpaRepository<WorkItem, Long>, JpaSpecificationExecutor<WorkItem> {

    /** Bir haftalık rapora bağlı iş kalemleri. */
    List<WorkItem> findByWeeklyReportId(Long weeklyReportId);

    /** Canlı task hesabı için: bir raporda belirli durumdaki iş kalemi sayısı (on analiz H-05). */
    long countByWeeklyReportIdAndStatus(Long weeklyReportId, WorkItemStatus status);
}
