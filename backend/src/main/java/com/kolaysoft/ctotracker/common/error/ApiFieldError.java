package com.kolaysoft.ctotracker.common.error;

import io.swagger.v3.oas.annotations.media.Schema;

/** Tek bir alanin dogrulama hatasi. */
@Schema(description = "Alan bazli dogrulama hatasi")
public record ApiFieldError(

        @Schema(description = "Hatali alanin adi", example = "weekNumber")
        String field,

        @Schema(description = "Hatanin aciklamasi", example = "Hafta numarasi 1 ile 53 arasinda olmalidir.")
        String message) {
}
