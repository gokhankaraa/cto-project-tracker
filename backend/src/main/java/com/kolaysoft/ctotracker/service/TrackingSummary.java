package com.kolaysoft.ctotracker.service;

import com.kolaysoft.ctotracker.entity.RiskLevel;

/**
 * Bir projenin kapanmamis risk / engel / karar / aksiyon kayitlarinin ozeti (dashboard satiri ve sayaclari).
 *
 * @param openItems        kapanmamis kayit sayisi (dort tur)
 * @param overdueItems     takip tarihi gecmis kapanmamis kayit sayisi
 * @param openRisks        kapanmamis risk sayisi
 * @param openBlockers     kapanmamis engel sayisi
 * @param pendingDecisions verilmemis (kapanmamis) karar sayisi
 * @param highestRiskLevel kapanmamis risk ve engellerin en yuksek seviyesi; yoksa null
 */
public record TrackingSummary(
        long openItems,
        long overdueItems,
        long openRisks,
        long openBlockers,
        long pendingDecisions,
        RiskLevel highestRiskLevel) {

    public static final TrackingSummary EMPTY = new TrackingSummary(0, 0, 0, 0, 0, null);
}
