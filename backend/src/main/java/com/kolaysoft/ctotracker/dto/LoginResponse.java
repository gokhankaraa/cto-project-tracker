package com.kolaysoft.ctotracker.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/** Basarili giris yaniti: sonraki isteklerde {@code Authorization: Bearer <token>} olarak gonderilir. */
@Schema(description = "Basarili giris yaniti")
public record LoginResponse(

        @Schema(description = "Imzali JWT; sonraki isteklerde 'Authorization: Bearer <token>' basliginda gonderilir")
        String token,

        @Schema(description = "Token'in gecerlilik sonu (UTC)", example = "2026-11-09T20:00:00Z")
        Instant expiresAt,

        @Schema(description = "Giris yapan kullanici")
        UserResponse user) {
}
