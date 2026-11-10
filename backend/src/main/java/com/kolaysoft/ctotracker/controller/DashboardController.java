package com.kolaysoft.ctotracker.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kolaysoft.ctotracker.dto.DashboardSummaryResponse;
import com.kolaysoft.ctotracker.dto.DashboardTrendResponse;
import com.kolaysoft.ctotracker.dto.filter.DashboardFilter;
import com.kolaysoft.ctotracker.dto.filter.TrendFilter;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.export.DashboardExportService;
import com.kolaysoft.ctotracker.service.DashboardService;
import com.kolaysoft.ctotracker.service.TrendService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * CTO dashboard endpoint'i (on analiz H-06). Tum projelerin son durumunu tek ozet olarak doner.
 *
 * <p>Filtre parametreleri filtre sozlesmesinin 2. bolumune gore tanimlidir. Parametreler tek tek
 * {@code @RequestParam} olarak alinir (sozlesme S-01): boylece tip hatalari mevcut
 * {@code 400 INVALID_REQUEST} yolundan, aralik hatalari {@code 400 VALIDATION_ERROR} yolundan doner.
 */
@Tag(name = "Dashboard", description = "CTO portfoy ozeti")
@RestController
@RequestMapping(value = "/api/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
public class DashboardController {

    private final DashboardService dashboardService;
    private final TrendService trendService;
    private final DashboardExportService exportService;

    public DashboardController(DashboardService dashboardService, TrendService trendService,
                               DashboardExportService exportService) {
        this.dashboardService = dashboardService;
        this.trendService = trendService;
        this.exportService = exportService;
    }

    @Operation(summary = "Portföyün haftalık trendi",
            description = "Referans haftada biten penceredeki her hafta için portföyün o haftaki hali: her proje o haftaya "
                    + "kadarki en son raporuyla değerlendirilir (summary'deki hafta referansı kuralı). Risk, engel, karar ve "
                    + "aksiyon sayıları hafta sonunda açık olan kayıtlardır (oluşturulma ve kapanış tarihinden). Proje "
                    + "filtreleri summary'dekilerle aynıdır; rapora dayalı filtre yoktur.")
    @GetMapping("/trend")
    public DashboardTrendResponse trend(
            @Parameter(description = "Yalnizca bu proje", example = "1")
            @RequestParam(required = false) Long projectId,

            @Parameter(description = "Proje durumu; birden cok deger virgulle verilebilir", example = "DEVAM_EDIYOR")
            @RequestParam(required = false) List<ProjectStatus> projectStatus,

            @Parameter(description = "Sorumlu proje yoneticisinin kullanici id'si", example = "1")
            @RequestParam(required = false) Long ownerId,

            @Parameter(description = "Pencerenin bittiği yıl. Yalnızca yıl verilirse o yılın son haftası; verilmezse "
                    + "bu hafta", example = "2026")
            @RequestParam(required = false)
            @Min(value = 2000, message = "Yıl en az 2000 olmalıdır.")
            @Max(value = 2100, message = "Yıl en fazla 2100 olmalıdır.") Integer year,

            @Parameter(description = "Pencerenin bittiği hafta (1-53), yalnızca year ile birlikte; year olmadan 400 döner",
                    example = "31")
            @RequestParam(required = false)
            @Min(value = 1, message = "Hafta numarası en az 1 olmalıdır.")
            @Max(value = 53, message = "Hafta numarası en fazla 53 olmalıdır.") Integer weekNumber,

            @Parameter(description = "Pencere uzunluğu, hafta (1-52)", example = "12")
            @RequestParam(defaultValue = "" + TrendService.DEFAULT_WEEKS)
            @Min(value = 1, message = "Hafta sayısı en az 1 olmalıdır.")
            @Max(value = TrendService.MAX_WEEKS, message = "Hafta sayısı en fazla 52 olabilir.") int weeks) {

        return trendService.getTrend(new TrendFilter(projectId, projectStatus, ownerId, year, weekNumber, weeks));
    }

    @Operation(summary = "CTO portfoy ozeti",
            description = "Tum projelerin degerlendirilen haftalik rapora (varsayilan en son rapor; year ve "
                    + "weekNumber ile gecmis bir nokta) gore durumunu, ilerlemesini, riskini, canli task sayisini "
                    + "ve gecikme bilgisini tek ozette doner. Farkli filtreler VE ile, cok degerli bir parametrenin degerleri VEYA ile birlesir (or. riskLevel=ORTA,YUKSEK). "
                    + "Sayaclar filtrelenmis kumeden hesaplanir.")
    @GetMapping("/summary")
    public DashboardSummaryResponse summary(
            @Parameter(description = "Yalnizca bu proje", example = "1")
            @RequestParam(required = false) Long projectId,

            @Parameter(description = "Proje durumu; birden cok deger virgulle verilebilir", example = "DEVAM_EDIYOR")
            @RequestParam(required = false) List<ProjectStatus> projectStatus,

            @Parameter(description = "Sorumlu proje yoneticisinin kullanici id'si", example = "1")
            @RequestParam(required = false) Long ownerId,

            @Parameter(description = "Referans yil (raporun anlattigi haftanin ISO yili). Yalnizca yil verilirse her proje "
                    + "icin o yilin sonuna kadarki en son rapor esas alinir", example = "2026")
            @RequestParam(required = false)
            @Min(value = 2000, message = "Yıl en az 2000 olmalıdır.")
            @Max(value = 2100, message = "Yıl en fazla 2100 olmalıdır.") Integer year,

            @Parameter(description = "Referans hafta (1-53), yalnizca year ile birlikte: her proje icin bu yil ve "
                    + "haftaya kadarki en son rapor esas alinir (onceki yillar dahil); o noktaya kadar raporu "
                    + "olmayan projeler hasReport=false ile listelenir. year olmadan 400 doner", example = "29")
            @RequestParam(required = false)
            @Min(value = 1, message = "Hafta numarası en az 1 olmalıdır.")
            @Max(value = 53, message = "Hafta numarası en fazla 53 olmalıdır.") Integer weekNumber,

            @Parameter(description = "Raporun genel durumu; birden cok deger virgulle verilebilir", example = "RISKLI")
            @RequestParam(required = false) List<OverallStatus> overallStatus,

            @Parameter(description = "Raporun risk seviyesi; birden cok deger virgulle verilebilir",
                    example = "ORTA,YUKSEK")
            @RequestParam(required = false) List<RiskLevel> riskLevel,

            @Parameter(description = "true: yalnizca gecikmedeki projeler, false: yalnizca gecikmede olmayanlar",
                    example = "true")
            @RequestParam(required = false) Boolean delayed) {

        return dashboardService.getSummary(new DashboardFilter(
                projectId, projectStatus, ownerId, year, weekNumber, overallStatus, riskLevel, delayed));
    }

    @Operation(summary = "CTO özetini Excel (.xlsx) olarak indir",
            description = "Dashboard'da görülenin dosya hali, aynı filtrelerle. Sayfalar: Özet (sayaçlar ve uygulanan "
                    + "filtreler), Projeler (dashboard satırları), Riskler ve aksiyonlar (listelenen projelerin kapanmamış "
                    + "kayıtları), Trend (aynı referansla biten son 12 hafta).")
    @ApiResponse(responseCode = "200", description = "Excel dosyası",
            content = @Content(mediaType = DashboardExportService.CONTENT_TYPE))
    // produces verilmez: hatali parametrenin 400 govdesi JSON'dur ve "yalnizca xlsx" diyen bir esleme bunu 406'ya
    // cevirirdi. Basarili yanitin tipi asagida acikca konur; Spring onceden konmus tipi oldugu gibi kullanir.
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "Yalnizca bu proje", example = "1")
            @RequestParam(required = false) Long projectId,

            @Parameter(description = "Proje durumu; birden cok deger virgulle verilebilir", example = "DEVAM_EDIYOR")
            @RequestParam(required = false) List<ProjectStatus> projectStatus,

            @Parameter(description = "Sorumlu proje yoneticisinin kullanici id'si", example = "1")
            @RequestParam(required = false) Long ownerId,

            @Parameter(description = "Referans yil (raporun anlattigi haftanin ISO yili). Yalnizca yil verilirse her proje "
                    + "icin o yilin sonuna kadarki en son rapor esas alinir", example = "2026")
            @RequestParam(required = false)
            @Min(value = 2000, message = "Yıl en az 2000 olmalıdır.")
            @Max(value = 2100, message = "Yıl en fazla 2100 olmalıdır.") Integer year,

            @Parameter(description = "Referans hafta (1-53), yalnizca year ile birlikte: her proje icin bu yil ve "
                    + "haftaya kadarki en son rapor esas alinir (onceki yillar dahil); o noktaya kadar raporu "
                    + "olmayan projeler hasReport=false ile listelenir. year olmadan 400 doner", example = "29")
            @RequestParam(required = false)
            @Min(value = 1, message = "Hafta numarası en az 1 olmalıdır.")
            @Max(value = 53, message = "Hafta numarası en fazla 53 olmalıdır.") Integer weekNumber,

            @Parameter(description = "Raporun genel durumu; birden cok deger virgulle verilebilir", example = "RISKLI")
            @RequestParam(required = false) List<OverallStatus> overallStatus,

            @Parameter(description = "Raporun risk seviyesi; birden cok deger virgulle verilebilir",
                    example = "ORTA,YUKSEK")
            @RequestParam(required = false) List<RiskLevel> riskLevel,

            @Parameter(description = "true: yalnizca gecikmedeki projeler, false: yalnizca gecikmede olmayanlar",
                    example = "true")
            @RequestParam(required = false) Boolean delayed) {

        byte[] file = exportService.export(new DashboardFilter(
                projectId, projectStatus, ownerId, year, weekNumber, overallStatus, riskLevel, delayed));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(exportService.fileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(DashboardExportService.CONTENT_TYPE))
                .body(file);
    }
}
