package com.kolaysoft.ctotracker.dto;

import java.time.LocalDate;

import com.kolaysoft.ctotracker.entity.WorkItemStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * İş kalemi oluşturma/güncelleme isteği.
 * Bağlı olduğu rapor URL yolundan gelir.
 */
@Schema(description = "İş kalemi oluşturma/güncelleme isteği")
public record WorkItemRequest(

        @Schema(description = "İş kaleminin başlığı", example = "Rapor formu API'si")
        @NotBlank(message = "Başlık zorunludur.")
        @Size(max = 255, message = "Başlık en fazla 255 karakter olabilir.")
        String title,

        @Schema(description = "Açıklama")
        @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir.")
        String description,

        @Schema(description = "Sorumlu (serbest metin)", example = "Ayşe Yılmaz")
        @Size(max = 255, message = "Sorumlu en fazla 255 karakter olabilir.")
        String assignee,

        @Schema(description = "Durum", example = "DEVAM_EDIYOR")
        @NotNull(message = "Durum zorunludur.")
        WorkItemStatus status,

        @Schema(description = "Planlanan tarih", example = "2026-07-28")
        LocalDate plannedDate,

        @Schema(description = "Tamamlanan tarih", example = "2026-07-31")
        LocalDate completedDate) {
}
