package com.kolaysoft.ctotracker.dto.filter;

import java.util.List;

import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;

/**
 * CTO dashboard filtresi (filtre sözleşmesi T13, bölüm 2).
 *
 * <p>Tekil değerlerde null, çok değerli alanlarda boş liste "filtre yok" demektir.
 * {@code year} ve {@code weekNumber} bir filtre değil, değerlendirilen raporu seçen referans noktadır
 * (K-T13-05, V2): her proje için bu noktaya kadarki en son rapor esas alınır. {@code weekNumber}
 * yalnızca {@code year} ile birlikte verilebilir.
 */
public record DashboardFilter(
        Long projectId,
        List<ProjectStatus> projectStatuses,
        Long ownerId,
        Integer year,
        Integer weekNumber,
        List<OverallStatus> overallStatuses,
        List<RiskLevel> riskLevels,
        Boolean delayed) {

    public DashboardFilter {
        FilterValues.requireYear(year, weekNumber, "weekNumber");
        projectStatuses = FilterValues.values(projectStatuses);
        overallStatuses = FilterValues.values(overallStatuses);
        riskLevels = FilterValues.values(riskLevels);
    }

    /** Hiç filtre içermeyen dashboard isteği (MVP'deki davranış). */
    public static DashboardFilter none() {
        return new DashboardFilter(null, null, null, null, null, null, null, null);
    }

    /**
     * Referans noktanın anahtarı ({@code year * 100 + hafta}); referans yoksa null, yani en son rapor.
     * Yalnızca yıl verildiyse o yılın sonuna kadarki en son rapor seçilir.
     */
    public Integer referenceKey() {
        return FilterValues.referenceKey(year, weekNumber);
    }

    /**
     * Değerlendirilen rapora dayalı bir koşul (rapor durumu, risk, gecikme) var mı.
     * Varsa raporu olmayan projeler listelenmez (K-T13-04).
     */
    public boolean hasReportCriteria() {
        return !overallStatuses.isEmpty() || !riskLevels.isEmpty() || delayed != null;
    }
}
