package com.kolaysoft.ctotracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kolaysoft.ctotracker.entity.TrackingItem;

public interface TrackingItemRepository extends JpaRepository<TrackingItem, Long> {

    List<TrackingItem> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);
}
