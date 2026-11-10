package com.kolaysoft.ctotracker.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;

import io.swagger.v3.oas.annotations.media.Schema;

/** Takip kaydi yaniti. Proje adi, projeler arasi listede kaydin nereye ait oldugunu gostermek icindir. */
@Schema(description = "Risk / engel / karar / aksiyon kaydı")
public record TrackingItemResponse(

        @Schema(description = "Kayıt id'si", example = "1")
        Long id,

        @Schema(description = "Proje id'si", example = "1")
        Long projectId,

        @Schema(description = "Proje adı", example = "Atlas IK")
        String projectName,

        @Schema(description = "Tür", example = "RISK")
        TrackingItemType type,

        @Schema(description = "Başlık", example = "Entegrasyon API'si geç teslim edilebilir")
        String title,

        @Schema(description = "Açıklama")
        String description,

        @Schema(description = "Risk seviyesi (yalnızca risk ve engelde)", example = "YUKSEK")
        RiskLevel riskLevel,

        @Schema(description = "Etki")
        String impact,

        @Schema(description = "Aksiyon planı")
        String actionPlan,

        @Schema(description = "Sorumlu", example = "Ayşe Yılmaz")
        String assignee,

        @Schema(description = "Takip tarihi", example = "2026-11-20")
        LocalDate followUpDate,

        @Schema(description = "Durum", example = "ACIK")
        TrackingItemStatus status,

        @Schema(description = "Kapanış tarihi; kayıt kapanınca sunucu yazar", example = "2026-11-18")
        LocalDate closedDate,

        @Schema(description = "Oluşturulma zamanı")
        Instant createdAt,

        @Schema(description = "Takibi gecikmiş mi: kayıt kapalı değil ve takip tarihi bugünden önce", example = "false")
        boolean overdue) {
}
