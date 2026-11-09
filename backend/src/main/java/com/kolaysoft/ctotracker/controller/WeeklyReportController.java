package com.kolaysoft.ctotracker.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kolaysoft.ctotracker.dto.PageResponse;
import com.kolaysoft.ctotracker.dto.WeeklyReportRequest;
import com.kolaysoft.ctotracker.dto.WeeklyReportResponse;
import com.kolaysoft.ctotracker.dto.filter.ReportFilter;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.service.WeeklyReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Haftalik rapor endpoint'leri. Olusturma/listeleme proje altindan
 * ({@code /api/projects/{projectId}/reports}); tekil islemler rapor id'si uzerinden
 * ({@code /api/reports/{id}}) yapilir (on analiz, bolum 5). Projeler arasi filtreli ve sayfali
 * liste {@code GET /api/reports} uzerindendir (filtre sozlesmesi, bolum 3).
 */
@Tag(name = "WeeklyReport", description = "Haftalik rapor yonetimi")
@RestController
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    public WeeklyReportController(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    @Operation(summary = "Projeye ait haftalik raporlari listele")
    @GetMapping(value = "/api/projects/{projectId}/reports", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WeeklyReportResponse> listByProject(@PathVariable Long projectId) {
        return weeklyReportService.findByProject(projectId);
    }

    @Operation(summary = "Projeye haftalik rapor olustur")
    @PostMapping(value = "/api/projects/{projectId}/reports",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WeeklyReportResponse> create(@PathVariable Long projectId,
                                                       @Valid @RequestBody WeeklyReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(weeklyReportService.create(projectId, request));
    }

    @Operation(summary = "Haftalik raporlari projeler arasi filtreli ve sayfali listele",
            description = "Farkli filtreler VE ile, cok degerli bir parametrenin degerleri VEYA ile birlesir. "
                    + "Aralik uclari dahildir. Hafta araligi year ile birlikte o yilin icinde uygulanir. "
                    + "Varsayilan siralama: en yeni rapor en ustte (reportDate,desc).")
    @GetMapping(value = "/api/reports", produces = MediaType.APPLICATION_JSON_VALUE)
    public PageResponse<WeeklyReportResponse> search(
            @Parameter(description = "Yalnizca bu proje", example = "1")
            @RequestParam(required = false) Long projectId,

            @Parameter(description = "Sorumlu proje yoneticisinin kullanici id'si", example = "1")
            @RequestParam(required = false) Long ownerId,

            @Parameter(description = "Raporun yili (anlattigi haftanin ISO yili)", example = "2026")
            @RequestParam(required = false)
            @Min(value = 2000, message = "Yıl en az 2000 olmalıdır.")
            @Max(value = 2100, message = "Yıl en fazla 2100 olmalıdır.") Integer year,

            @Parameter(description = "Baslangic haftasi (dahil, 1-53); yalnizca year ile birlikte, o yilin icinde",
                    example = "29")
            @RequestParam(required = false)
            @Min(value = 1, message = "Hafta numarası en az 1 olmalıdır.")
            @Max(value = 53, message = "Hafta numarası en fazla 53 olmalıdır.") Integer weekFrom,

            @Parameter(description = "Bitis haftasi (dahil, 1-53); yalnizca year ile birlikte, weekFrom'dan kucuk "
                    + "olamaz", example = "30")
            @RequestParam(required = false)
            @Min(value = 1, message = "Hafta numarası en az 1 olmalıdır.")
            @Max(value = 53, message = "Hafta numarası en fazla 53 olmalıdır.") Integer weekTo,

            @Parameter(description = "Baslangic rapor tarihi (dahil, yyyy-MM-dd)", example = "2026-07-20")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,

            @Parameter(description = "Bitis rapor tarihi (dahil, yyyy-MM-dd); dateFrom'dan once olamaz",
                    example = "2026-07-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,

            @Parameter(description = "Raporun genel durumu; birden cok deger virgulle verilebilir", example = "RISKLI")
            @RequestParam(required = false) List<OverallStatus> overallStatus,

            @Parameter(description = "Risk seviyesi; birden cok deger virgulle verilebilir", example = "ORTA,YUKSEK")
            @RequestParam(required = false) List<RiskLevel> riskLevel,

            @Parameter(description = "Ilerleme kademesi; birden cok deger virgulle verilebilir", example = "TEST")
            @RequestParam(required = false) List<ProgressStage> progressStage,

            @Parameter(description = "Sayfa numarasi (0'dan baslar)", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Sayfa numarası 0 veya daha büyük olmalıdır.") int page,

            @Parameter(description = "Sayfa boyutu (1-100)", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Sayfa boyutu en az 1 olmalıdır.")
            @Max(value = 100, message = "Sayfa boyutu en fazla 100 olabilir.") int size,

            @Parameter(description = "Siralama: 'alan' veya 'alan,asc|desc'. Alanlar: reportDate, weekNumber, "
                    + "projectName. Varsayilan: reportDate,desc", example = "weekNumber,asc")
            @RequestParam(required = false) String sort) {

        ReportFilter filter = new ReportFilter(projectId, ownerId, year, weekFrom, weekTo, dateFrom, dateTo,
                overallStatus, riskLevel, progressStage);
        return weeklyReportService.search(filter, page, size, sort);
    }

    @Operation(summary = "Haftalik rapor detayi")
    @GetMapping(value = "/api/reports/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public WeeklyReportResponse get(@PathVariable Long id) {
        return weeklyReportService.findById(id);
    }

    @Operation(summary = "Haftalik rapor guncelle")
    @PutMapping(value = "/api/reports/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WeeklyReportResponse update(@PathVariable Long id, @Valid @RequestBody WeeklyReportRequest request) {
        return weeklyReportService.update(id, request);
    }

    @Operation(summary = "Haftalik rapor sil")
    @DeleteMapping("/api/reports/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        weeklyReportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
