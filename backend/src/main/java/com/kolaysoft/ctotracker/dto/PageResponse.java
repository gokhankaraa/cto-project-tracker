package com.kolaysoft.ctotracker.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sayfali liste yaniti (T13 filtre sozlesmesi, S-02).
 * Spring'in {@code Page} nesnesi dogrudan dondurulmez; boylece JSON yapisi Spring surumunden
 * bagimsiz ve sabit kalir.
 */
@Schema(description = "Sayfali liste yaniti")
public record PageResponse<T>(

        @Schema(description = "Bu sayfadaki kayitlar")
        List<T> content,

        @Schema(description = "Sayfa numarasi (0'dan baslar)", example = "0")
        int page,

        @Schema(description = "Sayfa boyutu", example = "20")
        int size,

        @Schema(description = "Filtreye uyan toplam kayit sayisi", example = "3")
        long totalElements,

        @Schema(description = "Toplam sayfa sayisi", example = "1")
        int totalPages) {

    /** Spring Data sayfasini, kayitlari {@code mapper} ile donusturerek yanita cevirir. */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<? super E, ? extends T> mapper) {
        List<T> content = page.getContent().stream().<T>map(mapper).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
