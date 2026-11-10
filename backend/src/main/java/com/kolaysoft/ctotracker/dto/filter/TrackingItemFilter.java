package com.kolaysoft.ctotracker.dto.filter;

import java.util.List;

import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;

/**
 * Projeler arasi risk / engel / karar / aksiyon listesi filtresi (filtre sozlesmesi, bolum 4a).
 * Takip kayitlari haftaya bagli olmadigi icin yil ve hafta referansi yoktur; gecikme bugune goredir.
 */
public record TrackingItemFilter(
        Long projectId,
        List<TrackingItemType> types,
        List<TrackingItemStatus> statuses,
        List<RiskLevel> riskLevels,
        Boolean overdue,
        String assignee) {

    public TrackingItemFilter {
        types = FilterValues.values(types);
        statuses = FilterValues.values(statuses);
        riskLevels = FilterValues.values(riskLevels);
        assignee = FilterValues.text(assignee);
    }
}
