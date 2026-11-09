package com.kolaysoft.ctotracker.dto;

import com.kolaysoft.ctotracker.entity.ProjectStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Proje olusturma/guncelleme istegi. */
@Schema(description = "Proje olusturma/guncelleme istegi")
public record ProjectRequest(

        @Schema(description = "Proje adi", example = "Atlas IK")
        @NotBlank(message = "Proje adi zorunludur.")
        @Size(max = 255, message = "Proje adi en fazla 255 karakter olabilir.")
        String name,

        @Schema(description = "Musteri veya demo musteri", example = "Demo Musteri A.S.")
        @Size(max = 255, message = "Musteri en fazla 255 karakter olabilir.")
        String customer,

        @Schema(description = "Proje aciklamasi", example = "Insan kaynaklari surecleri urunu")
        @Size(max = 1000, message = "Aciklama en fazla 1000 karakter olabilir.")
        String description,

        @Schema(description = "Proje durumu", example = "DEVAM_EDIYOR")
        @NotNull(message = "Proje durumu zorunludur.")
        ProjectStatus status,

        @Schema(description = "Sorumlu proje yoneticisinin kullanici id'si", example = "1")
        @NotNull(message = "Sorumlu proje yoneticisi (ownerId) zorunludur.")
        Long ownerId) {
}
