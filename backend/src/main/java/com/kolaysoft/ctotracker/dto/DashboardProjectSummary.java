package com.kolaysoft.ctotracker.dto;

import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CTO dashboard'unda tek bir projenin ozet satiri (on analiz H-06).
 * Ilerleme/durum/risk bilgileri projenin DEGERLENDIRILEN raporundan turer: referans hafta verilmezse
 * en guncel rapor (en yuksek hafta), verilirse o haftaya kadarki en son rapor (filtre sozlesmesi).
 * Boyle bir rapor yoksa {@code hasReport=false} ve rapora bagli alanlar null/0/false doner.
 */
@Schema(description = "CTO dashboard proje ozet satiri")
public record DashboardProjectSummary(

        @Schema(description = "Proje id'si", example = "1")
        Long projectId,

        @Schema(description = "Proje adi", example = "Atlas IK")
        String name,

        @Schema(description = "Musteri", example = "Demo Musteri A.S.")
        String customer,

        @Schema(description = "Sorumlu proje yoneticisi", example = "Ayse Yilmaz")
        String ownerFullName,

        @Schema(description = "Projenin genel durumu", example = "DEVAM_EDIYOR")
        ProjectStatus projectStatus,

        @Schema(description = "Projenin degerlendirilen raporu var mi (referans hafta verilmisse o haftaya kadar)",
                example = "true")
        boolean hasReport,

        @Schema(description = "Son raporun yili: anlattigi haftanin ISO yili (rapor yoksa null)",
                example = "2026")
        Integer lastYear,

        @Schema(description = "Son raporun hafta numarasi (rapor yoksa null)", example = "30")
        Integer lastWeekNumber,

        @Schema(description = "Son rapordaki ilerleme kademesi (rapor yoksa null)", example = "TEST")
        ProgressStage progressStage,

        @Schema(description = "Ilerleme yuzdesi; kademeden turer, rapor yoksa 0", example = "75")
        int progressPercentage,

        @Schema(description = "Son rapordaki genel durum (rapor yoksa null)", example = "YOLUNDA")
        OverallStatus overallStatus,

        @Schema(description = "Son rapordaki risk seviyesi (rapor yoksa null)", example = "ORTA")
        RiskLevel riskLevel,

        @Schema(description = "Son rapordaki canli task sayisi (DEVAM_EDIYOR is kalemleri)", example = "2")
        long liveTaskCount,

        @Schema(description = "Proje gecikmede mi: rapor durumu GECIKMELI ya da raporda gecikmis is kalemi var "
                + "(filtreleme analizi KF-01); rapor yoksa false", example = "false")
        boolean delayed) {
}
