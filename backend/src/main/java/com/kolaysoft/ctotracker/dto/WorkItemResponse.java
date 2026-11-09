package com.kolaysoft.ctotracker.dto;

import java.time.LocalDate;

import com.kolaysoft.ctotracker.entity.WorkItemStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * İş kalemi yanıtı. Proje ve hafta bilgisi, projeler arası listede (T13) kaydın nereye ait olduğunu
 * göstermek için bağlı rapordan türetilir.
 */
@Schema(description = "İş kalemi yanıtı")
public record WorkItemResponse(

        @Schema(description = "İş kalemi id'si", example = "1")
        Long id,

        @Schema(description = "Bağlı olduğu haftalık rapor id'si", example = "1")
        Long weeklyReportId,

        @Schema(description = "Raporun ait olduğu proje id'si (T13)", example = "1")
        Long projectId,

        @Schema(description = "Raporun ait olduğu projenin adı (T13)", example = "Atlas IK")
        String projectName,

        @Schema(description = "Bağlı olduğu raporun yılı (raporun anlattığı haftanın ISO yılı)", example = "2026")
        Integer year,

        @Schema(description = "Bağlı olduğu raporun hafta numarası (T13)", example = "29")
        Integer weekNumber,

        @Schema(description = "Başlık", example = "Rapor formu API'si")
        String title,

        @Schema(description = "Açıklama")
        String description,

        @Schema(description = "Sorumlu", example = "Ayşe Yılmaz")
        String assignee,

        @Schema(description = "Durum", example = "DEVAM_EDIYOR")
        WorkItemStatus status,

        @Schema(description = "Planlanan tarih", example = "2026-07-28")
        LocalDate plannedDate,

        @Schema(description = "Tamamlanan tarih", example = "2026-07-31")
        LocalDate completedDate,

        @Schema(description = "Gecikmiş mi: durumu GECIKTI ya da planlanan tarihi raporun tarihinden önce olup "
                + "tamamlanmamış (T13 analiz notu K-T13-01)", example = "false")
        boolean delayed) {
}
