package com.kolaysoft.ctotracker.dto.filter;

import java.time.LocalDate;
import java.util.List;

import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.RiskLevel;

/**
 * Projeler arası haftalık rapor listesi filtresi (filtre sözleşmesi, bölüm 3).
 *
 * <p>Tekil değerlerde null, çok değerli alanlarda boş liste "filtre yok" demektir.
 * Hafta ve tarih aralıklarının uçları dahildir. {@code year} raporun yılına eşitlik filtresidir;
 * hafta aralığı o yılın içinde uygulanır, bu yüzden {@code weekFrom}/{@code weekTo} yalnızca
 * {@code year} ile birlikte verilebilir.
 */
public record ReportFilter(
        Long projectId,
        Long ownerId,
        Integer year,
        Integer weekFrom,
        Integer weekTo,
        LocalDate dateFrom,
        LocalDate dateTo,
        List<OverallStatus> overallStatuses,
        List<RiskLevel> riskLevels,
        List<ProgressStage> progressStages) {

    public ReportFilter {
        FilterValues.requireYear(year, weekFrom, "weekFrom");
        FilterValues.requireYear(year, weekTo, "weekTo");
        overallStatuses = FilterValues.values(overallStatuses);
        riskLevels = FilterValues.values(riskLevels);
        progressStages = FilterValues.values(progressStages);
    }
}
