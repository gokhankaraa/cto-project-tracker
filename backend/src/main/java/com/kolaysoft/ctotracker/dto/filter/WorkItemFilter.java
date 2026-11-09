package com.kolaysoft.ctotracker.dto.filter;

import java.util.List;

import com.kolaysoft.ctotracker.entity.WorkItemStatus;

/**
 * Projeler arası iş kalemi listesi filtresi (filtre sözleşmesi T13, bölüm 4).
 *
 * <p>Liste her zaman projelerin değerlendirilen raporuna göre çalışır (S-05): referans yoksa en son
 * rapor, yalnızca {@code year} varsa o yılın sonuna kadarki en son rapor, {@code year} + {@code weekNumber}
 * varsa o haftaya kadarki (dahil) en son rapor. {@code weekNumber} yalnızca {@code year} ile birlikte
 * verilebilir.
 */
public record WorkItemFilter(
        Long projectId,
        Integer year,
        Integer weekNumber,
        String assignee,
        List<WorkItemStatus> statuses,
        Boolean delayed) {

    public WorkItemFilter {
        FilterValues.requireYear(year, weekNumber, "weekNumber");
        assignee = FilterValues.text(assignee);
        statuses = FilterValues.values(statuses);
    }

    /** Referans noktanın anahtarı ({@code year * 100 + hafta}); referans yoksa null, yani en son rapor. */
    public Integer referenceKey() {
        return FilterValues.referenceKey(year, weekNumber);
    }
}
