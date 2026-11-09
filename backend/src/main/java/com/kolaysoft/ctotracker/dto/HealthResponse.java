package com.kolaysoft.ctotracker.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/** Uygulamanin ve veritabani baglantisinin durumu. */
@Schema(description = "Saglik kontrolu yaniti")
public record HealthResponse(

        @Schema(description = "Uygulamanin genel durumu", example = "UP", allowableValues = {"UP", "DEGRADED"})
        String status,

        @Schema(description = "Uygulama adi", example = "cto-project-tracker-backend")
        String application,

        @Schema(description = "Veritabani baglantisinin durumu", example = "UP", allowableValues = {"UP", "DOWN"})
        String databaseStatus,

        @Schema(description = "Bagli veritabani urunu ve surumu", example = "PostgreSQL 17.11")
        String database,

        @Schema(description = "Kontrolun yapildigi an", example = "2026-07-22T14:30:00+03:00")
        OffsetDateTime timestamp) {
}
