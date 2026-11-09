package com.kolaysoft.ctotracker.dto;

import com.kolaysoft.ctotracker.entity.ProjectStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/** Proje yaniti. Entity dogrudan disa acilmaz; owner sade bir ozet olarak doner. */
@Schema(description = "Proje yaniti")
public record ProjectResponse(

        @Schema(description = "Proje id'si", example = "1")
        Long id,

        @Schema(description = "Proje adi", example = "Atlas IK")
        String name,

        @Schema(description = "Musteri", example = "Demo Musteri A.S.")
        String customer,

        @Schema(description = "Proje aciklamasi")
        String description,

        @Schema(description = "Proje durumu", example = "DEVAM_EDIYOR")
        ProjectStatus status,

        @Schema(description = "Sorumlu proje yoneticisinin id'si", example = "1")
        Long ownerId,

        @Schema(description = "Sorumlu proje yoneticisinin adi", example = "Ayse Yilmaz")
        String ownerFullName) {
}
