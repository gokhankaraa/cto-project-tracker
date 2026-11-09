package com.kolaysoft.ctotracker.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CTO dashboard portfoy ozeti (on analiz H-06).
 * Proje ozet satirlari ve CTO'nun hizli bakisi icin temel sayaclar.
 * Filtre verildiginde sayaclar filtrelenmis kumeden hesaplanir (T13 analiz notu K-T13-06).
 */
@Schema(description = "CTO dashboard portfoy ozeti")
public record DashboardSummaryResponse(

        @Schema(description = "Toplam proje sayisi", example = "3")
        int totalProjects,

        @Schema(description = "Yuksek riskli proje sayisi (son rapora gore)", example = "1")
        long highRiskProjects,

        @Schema(description = "Hic raporu olmayan proje sayisi", example = "1")
        long projectsWithoutReport,

        @Schema(description = "Toplam canli task sayisi (tum projelerin son raporlarindan)", example = "3")
        long totalLiveTasks,

        @Schema(description = "Gecikmedeki proje sayisi", example = "0")
        long delayedProjects,

        @Schema(description = "Proje ozet satirlari")
        List<DashboardProjectSummary> projects) {
}
