package com.kolaysoft.ctotracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Giris istegi. */
@Schema(description = "Giris istegi")
public record LoginRequest(

        @Schema(description = "E-posta (buyuk/kucuk harf fark etmez)", example = "cto@example.com")
        // Bicim dogrulanmaz: bicimi bozuk e-posta zaten hicbir kullaniciyla eslesmez; bastaki/sondaki
        // bosluklar (kopyala-yapistir) serviste kirpilir.
        @NotBlank(message = "E-posta zorunludur.")
        @Size(max = 255, message = "E-posta en fazla 255 karakter olabilir.")
        String email,

        @Schema(description = "Parola", example = "Demo1234!")
        @NotBlank(message = "Parola zorunludur.")
        // BCrypt en fazla 72 bayt kabul eder; daha uzun parola hicbir ozetle eslesmez (Turkce harfler 2 bayttir).
        @Size(max = 72, message = "Parola en fazla 72 karakter olabilir.")
        String password) {
}
