package com.kolaysoft.ctotracker.dto;

import java.time.LocalDate;
import java.util.Map;

import com.kolaysoft.ctotracker.entity.OverallStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Portfoyun bir haftadaki hali. Rapor alanlari her proje icin o haftaya kadarki en son rapordan (dashboard'daki
 * hafta referansi kuraliyla), risk / aksiyon alanlari kayitlarin olusturulma ve kapanis tarihlerinden hesaplanir.
 */
@Schema(description = "Haftalık trend noktası")
public record TrendWeek(

        @Schema(description = "ISO yılı", example = "2026")
        int year,

        @Schema(description = "ISO hafta numarası", example = "31")
        int weekNumber,

        @Schema(description = "Haftanın pazartesisi", example = "2026-07-27")
        LocalDate weekStart,

        @Schema(description = "Filtreye uyan proje sayısı", example = "3")
        int totalProjects,

        @Schema(description = "O haftaya kadar raporu olan proje sayısı", example = "2")
        int projectsWithReport,

        @Schema(description = "Genel duruma göre proje sayıları (o haftaya kadarki en son rapor); her durum yer alır")
        Map<OverallStatus, Long> projectsByStatus,

        @Schema(description = "Risk seviyesi YUKSEK olan proje sayısı", example = "1")
        long highRiskProjects,

        @Schema(description = "Gecikmedeki proje sayısı (KF-01: rapor GECIKMELI ya da gecikmiş iş kalemi)", example = "1")
        long delayedProjects,

        @Schema(description = "Raporu olan projelerin ortalama ilerlemesi (yuvarlanmış yüzde); raporlu proje yoksa null",
                example = "50")
        Integer averageProgress,

        @Schema(description = "Hafta sonunda açık risk sayısı", example = "1")
        long openRisks,

        @Schema(description = "Hafta sonunda açık engel sayısı", example = "1")
        long openBlockers,

        @Schema(description = "Hafta sonunda bekleyen karar sayısı", example = "1")
        long pendingDecisions,

        @Schema(description = "Hafta sonunda açık aksiyon sayısı", example = "1")
        long openActions,

        @Schema(description = "Hafta içinde açılan risk, engel, karar ve aksiyon kaydı", example = "2")
        long openedTrackingItems,

        @Schema(description = "Hafta içinde kapanan kayıt", example = "1")
        long closedTrackingItems) {
}
