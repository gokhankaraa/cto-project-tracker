package com.kolaysoft.ctotracker.common.error;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tum hata yanitlarinin ortak govdesi (on analiz, bolum 11).
 * Alan bazli hatalar yoksa {@code fieldErrors} yanitta hic yer almaz.
 */
@Schema(description = "Standart hata yaniti")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(

        @Schema(description = "Hatanin olustugu an", example = "2026-07-22T14:30:00+03:00")
        OffsetDateTime timestamp,

        @Schema(description = "HTTP durum kodu", example = "400")
        int status,

        @Schema(description = "Makine tarafindan okunabilir hata kodu", example = "VALIDATION_ERROR")
        ErrorCode code,

        @Schema(description = "Kullaniciya gosterilebilir aciklama", example = "Gonderilen alanlar dogrulama kurallarina uymuyor.")
        String message,

        @Schema(description = "Hatanin olustugu istek yolu", example = "/api/demo/echo")
        String path,

        @Schema(description = "Alan bazli dogrulama hatalari; yalnizca VALIDATION_ERROR durumunda doner")
        List<ApiFieldError> fieldErrors) {

    public static ApiError of(int status, ErrorCode code, String message, String path) {
        return new ApiError(OffsetDateTime.now(), status, code, message, path, List.of());
    }

    public static ApiError of(int status, ErrorCode code, String message, String path, List<ApiFieldError> fieldErrors) {
        return new ApiError(OffsetDateTime.now(), status, code, message, path, fieldErrors);
    }
}
