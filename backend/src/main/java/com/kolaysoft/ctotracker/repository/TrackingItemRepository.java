package com.kolaysoft.ctotracker.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.kolaysoft.ctotracker.entity.TrackingItem;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;

/** Filtreli sorgular icin bkz. {@code repository.spec.TrackingItemSpecifications}. */
public interface TrackingItemRepository extends JpaRepository<TrackingItem, Long>,
        JpaSpecificationExecutor<TrackingItem> {

    List<TrackingItem> findByProjectId(Long projectId);

    /** Verilen projelerin belirli bir durumda olmayan kayitlari (dashboard: kapanmamis kayitlar). */
    List<TrackingItem> findByProjectIdInAndStatusNot(Collection<Long> projectIds, TrackingItemStatus status);

    boolean existsByProjectId(Long projectId);
}
