package com.kolaysoft.ctotracker.controller;

import java.util.List;

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
import com.kolaysoft.ctotracker.dto.WorkItemRequest;
import com.kolaysoft.ctotracker.dto.WorkItemResponse;
import com.kolaysoft.ctotracker.dto.filter.WorkItemFilter;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;
import com.kolaysoft.ctotracker.service.WorkItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * İş kalemi endpoint'leri. Olusturma/listeleme rapor altindan
 * ({@code /api/reports/{reportId}/work-items}); tekil islemler is kalemi id'si uzerinden
 * ({@code /api/work-items/{id}}) yapilir (on analiz, bolum 5). Projeler arasi filtreli ve sayfali
 * liste {@code GET /api/work-items} uzerindendir (filtre sozlesmesi, bolum 4).
 */
@Tag(name = "WorkItem", description = "İş kalemi yönetimi")
@RestController
public class WorkItemController {

    private final WorkItemService workItemService;

    public WorkItemController(WorkItemService workItemService) {
        this.workItemService = workItemService;
    }

    @Operation(summary = "Rapora ait iş kalemlerini listele")
    @GetMapping(value = "/api/reports/{reportId}/work-items", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WorkItemResponse> listByReport(@PathVariable Long reportId) {
        return workItemService.findByReport(reportId);
    }

    @Operation(summary = "Rapora iş kalemi oluştur")
    @PostMapping(value = "/api/reports/{reportId}/work-items",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkItemResponse> create(@PathVariable Long reportId,
                                                   @Valid @RequestBody WorkItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workItemService.create(reportId, request));
    }

    @Operation(summary = "İş kalemlerini projeler arası filtreli ve sayfalı listele",
            description = "Her projenin değerlendirilen raporundaki iş kalemleri listelenir: year yoksa en son rapor, "
                    + "yalnızca year varsa o yılın sonuna, year + weekNumber varsa o haftaya kadarki en son rapor. "
                    + "Farklı filtreler VE ile, çok değerli bir parametrenin değerleri VEYA ile birleşir. "
                    + "Varsayılan sıralama: plannedDate,asc (tarihsiz kayıtlar sonda).")
    @GetMapping(value = "/api/work-items", produces = MediaType.APPLICATION_JSON_VALUE)
    public PageResponse<WorkItemResponse> search(
            @Parameter(description = "Yalnızca bu proje", example = "1")
            @RequestParam(required = false) Long projectId,

            @Parameter(description = "Referans yıl (raporun anlattığı haftanın ISO yılı). Yalnızca yıl verilirse her proje "
                    + "için o yılın sonuna kadarki en son rapor", example = "2026")
            @RequestParam(required = false)
            @Min(value = 2000, message = "Yil en az 2000 olmalidir.")
            @Max(value = 2100, message = "Yil en fazla 2100 olmalidir.") Integer year,

            @Parameter(description = "Referans hafta (1-53), yalnızca year ile birlikte: her proje için bu yıl ve "
                    + "haftaya kadarki en son rapor (önceki yıllar dahil). year olmadan 400 döner", example = "29")
            @RequestParam(required = false)
            @Min(value = 1, message = "Hafta numarasi en az 1 olmalidir.")
            @Max(value = 53, message = "Hafta numarasi en fazla 53 olmalidir.") Integer weekNumber,

            @Parameter(description = "Sorumlu; büyük/küçük harfe duyarsız 'içerir' araması", example = "mehmet")
            @RequestParam(required = false)
            @Size(max = 255, message = "Sorumlu en fazla 255 karakter olabilir.") String assignee,

            @Parameter(description = "İş kalemi durumu; birden çok değer virgülle verilebilir",
                    example = "DEVAM_EDIYOR,GECIKTI")
            @RequestParam(required = false) List<WorkItemStatus> status,

            @Parameter(description = "true: yalnızca gecikmiş, false: yalnızca gecikmemiş iş kalemleri",
                    example = "true")
            @RequestParam(required = false) Boolean delayed,

            @Parameter(description = "Sayfa numarası (0'dan başlar)", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Sayfa numarasi 0 veya daha buyuk olmalidir.") int page,

            @Parameter(description = "Sayfa boyutu (1-100)", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Sayfa boyutu en az 1 olmalidir.")
            @Max(value = 100, message = "Sayfa boyutu en fazla 100 olabilir.") int size,

            @Parameter(description = "Sıralama: 'alan' veya 'alan,asc|desc'. Alanlar: plannedDate, title, "
                    + "projectName. Varsayılan: plannedDate,asc", example = "title,asc")
            @RequestParam(required = false) String sort) {

        WorkItemFilter filter = new WorkItemFilter(projectId, year, weekNumber, assignee, status, delayed);
        return workItemService.search(filter, page, size, sort);
    }

    @Operation(summary = "İş kalemi detayı")
    @GetMapping(value = "/api/work-items/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public WorkItemResponse get(@PathVariable Long id) {
        return workItemService.findById(id);
    }

    @Operation(summary = "İş kalemi güncelle")
    @PutMapping(value = "/api/work-items/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WorkItemResponse update(@PathVariable Long id, @Valid @RequestBody WorkItemRequest request) {
        return workItemService.update(id, request);
    }

    @Operation(summary = "İş kalemi sil")
    @DeleteMapping("/api/work-items/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
