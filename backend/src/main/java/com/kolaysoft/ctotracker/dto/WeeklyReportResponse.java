package com.kolaysoft.ctotracker.dto;

import java.time.LocalDate;

import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.RiskLevel;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Haftalik rapor yaniti.
 * {@code progressPercentage} ilerleme kademesinden turer; ayrica saklanmaz.
 */
@Schema(description = "Haftalik rapor yaniti")
public record WeeklyReportResponse(

        @Schema(description = "Rapor id'si", example = "1")
        Long id,

        @Schema(description = "Bagli oldugu proje id'si", example = "1")
        Long projectId,

        @Schema(description = "Bagli oldugu projenin adi (projeler arasi listede okunabilirlik icin, T13)",
                example = "Atlas IK")
        String projectName,

        @Schema(description = "Yil: raporun anlattigi haftanin ISO yili; istekte gonderilmez, hafta ve rapor tarihinden turetilir",
                example = "2026")
        Integer year,

        @Schema(description = "Hafta numarasi", example = "30")
        Integer weekNumber,

        @Schema(description = "Rapor tarihi", example = "2026-07-27")
        LocalDate reportDate,

        @Schema(description = "Ilerleme kademesi", example = "GELISTIRME")
        ProgressStage progressStage,

        @Schema(description = "Ilerleme yuzdesi (kademeden turer)", example = "50")
        int progressPercentage,

        @Schema(description = "Canli task sayisi (durumu DEVAM_EDIYOR olan is kalemleri)", example = "2")
        long liveTaskCount,

        @Schema(description = "Genel durum", example = "YOLUNDA")
        OverallStatus overallStatus,

        @Schema(description = "Risk seviyesi", example = "ORTA")
        RiskLevel riskLevel,

        @Schema(description = "O hafta yapilanlar")
        String done,

        @Schema(description = "Gelecek hafta yapilacaklar")
        String planned,

        @Schema(description = "Riskler / engeller")
        String risks,

        @Schema(description = "Genel durum notu")
        String note) {
}
