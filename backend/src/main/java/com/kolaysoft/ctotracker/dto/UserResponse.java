package com.kolaysoft.ctotracker.dto;

import com.kolaysoft.ctotracker.entity.Role;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Kullanici yaniti: frontend'de sorumlu proje yoneticisi secimi ve filtresi icin. E-posta bilerek
 * yer almaz; secim icin gerekmiyor (kimlik dogrulama T14 ile gelecek).
 */
@Schema(description = "Kullanici yaniti")
public record UserResponse(
        @Schema(description = "Kullanici id'si", example = "1")
        Long id,

        @Schema(description = "Ad soyad", example = "Ayse Yilmaz")
        String fullName,

        @Schema(description = "Rol", example = "PROJECT_MANAGER")
        Role role) {
}
