package com.kolaysoft.ctotracker.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kolaysoft.ctotracker.dto.AuditEventResponse;
import com.kolaysoft.ctotracker.dto.PageResponse;
import com.kolaysoft.ctotracker.entity.AuditEntityType;
import com.kolaysoft.ctotracker.service.HistoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Projenin degisiklik gecmisi (filtre sozlesmesi, bolum 4b). Yalnizca okunur: gecmisi degistiren ya da silen
 * endpoint yoktur. Gorunurluk projeninkiyle aynidir.
 */
@Tag(name = "History", description = "Değişiklik geçmişi")
@RestController
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Operation(summary = "Projenin değişiklik geçmişi",
            description = "Projeye, raporlarına, iş kalemlerine ve risk/aksiyon kayıtlarına API üzerinden yapılan "
                    + "ekleme, güncelleme ve silmeler; en yeni üstte. Güncellemede yalnızca değişen alanlar gelir. "
                    + "Değerler ham gelir (enum adı, ISO tarih).")
    @GetMapping(value = "/api/projects/{projectId}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public PageResponse<AuditEventResponse> history(
            @PathVariable Long projectId,

            @Parameter(description = "Yalnızca bu türdeki kayıtlar", example = "TAKIP_KAYDI")
            @RequestParam(required = false) AuditEntityType entityType,

            @Parameter(description = "Yalnızca bu kayıt; entityType ile birlikte verilir", example = "4")
            @RequestParam(required = false) Long entityId,

            @Parameter(description = "Sayfa numarası (0'dan başlar)", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Sayfa numarası 0 veya daha büyük olmalıdır.") int page,

            @Parameter(description = "Sayfa boyutu (1-100)", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Sayfa boyutu en az 1 olmalıdır.")
            @Max(value = 100, message = "Sayfa boyutu en fazla 100 olabilir.") int size) {

        return historyService.findByProject(projectId, entityType, entityId, page, size);
    }
}
