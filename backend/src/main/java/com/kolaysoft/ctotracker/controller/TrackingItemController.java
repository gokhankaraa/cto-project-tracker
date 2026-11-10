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
import com.kolaysoft.ctotracker.dto.TrackingItemRequest;
import com.kolaysoft.ctotracker.dto.TrackingItemResponse;
import com.kolaysoft.ctotracker.dto.filter.TrackingItemFilter;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;
import com.kolaysoft.ctotracker.service.TrackingItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Risk, engel, karar ve aksiyon kayitlari. Olusturma/listeleme proje altindan
 * ({@code /api/projects/{projectId}/tracking-items}); tekil islemler kayit id'si uzerinden
 * ({@code /api/tracking-items/{id}}) yapilir; projeler arasi filtreli ve sayfali liste {@code GET /api/tracking-items}
 * uzerindendir (filtre sozlesmesi, bolum 4a). Yazma yetkisi raporlarinkiyle aynidir ({@code Permissions}).
 */
@Tag(name = "TrackingItem", description = "Risk, engel, karar ve aksiyon kayıtları")
@RestController
public class TrackingItemController {

    private final TrackingItemService trackingItemService;

    public TrackingItemController(TrackingItemService trackingItemService) {
        this.trackingItemService = trackingItemService;
    }

    @Operation(summary = "Projenin risk, engel, karar ve aksiyon kayıtlarını listele",
            description = "Kapanmamış kayıtlar en yakın takip tarihine göre üstte, kapalı kayıtlar en altta.")
    @GetMapping(value = "/api/projects/{projectId}/tracking-items", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TrackingItemResponse> listByProject(@PathVariable Long projectId) {
        return trackingItemService.findByProject(projectId);
    }

    @Operation(summary = "Projeye risk, engel, karar ya da aksiyon kaydı ekle",
            description = "Risk seviyesi risk ve engelde zorunlu, karar ve aksiyonda verilmez. Kapanmamış kaydın "
                    + "sorumlusu ve takip tarihi zorunludur; takip tarihi bugünden önce olamaz.")
    @PostMapping(value = "/api/projects/{projectId}/tracking-items",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TrackingItemResponse> create(@PathVariable Long projectId,
                                                       @Valid @RequestBody TrackingItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trackingItemService.create(projectId, request));
    }

    @Operation(summary = "Risk, engel, karar ve aksiyon kayıtlarını projeler arası filtreli ve sayfalı listele",
            description = "Farklı filtreler VE ile, çok değerli bir parametrenin değerleri VEYA ile birleşir. Gecikme "
                    + "bugüne göredir. Varsayılan sıralama: followUpDate,asc (takip tarihi olmayan kayıtlar sonda).")
    @GetMapping(value = "/api/tracking-items", produces = MediaType.APPLICATION_JSON_VALUE)
    public PageResponse<TrackingItemResponse> search(
            @Parameter(description = "Yalnızca bu proje", example = "1")
            @RequestParam(required = false) Long projectId,

            @Parameter(description = "Tür; birden çok değer virgülle verilebilir", example = "RISK,ENGEL")
            @RequestParam(required = false) List<TrackingItemType> type,

            @Parameter(description = "Durum; birden çok değer virgülle verilebilir. Kapanmamış kayıtlar için "
                    + "ACIK,DEVAM_EDIYOR", example = "ACIK,DEVAM_EDIYOR")
            @RequestParam(required = false) List<TrackingItemStatus> status,

            @Parameter(description = "Risk seviyesi (yalnızca risk ve engel kayıtlarında bulunur); birden çok değer "
                    + "virgülle verilebilir", example = "YUKSEK")
            @RequestParam(required = false) List<RiskLevel> riskLevel,

            @Parameter(description = "true: yalnızca takibi gecikmiş (kapanmamış ve takip tarihi bugünden önce), "
                    + "false: yalnızca gecikmemiş kayıtlar", example = "true")
            @RequestParam(required = false) Boolean overdue,

            @Parameter(description = "Sorumlu; büyük/küçük harfe duyarsız 'içerir' araması", example = "zeynep")
            @RequestParam(required = false)
            @Size(max = 255, message = "Sorumlu en fazla 255 karakter olabilir.") String assignee,

            @Parameter(description = "Sayfa numarası (0'dan başlar)", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Sayfa numarası 0 veya daha büyük olmalıdır.") int page,

            @Parameter(description = "Sayfa boyutu (1-100)", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Sayfa boyutu en az 1 olmalıdır.")
            @Max(value = 100, message = "Sayfa boyutu en fazla 100 olabilir.") int size,

            @Parameter(description = "Sıralama: 'alan' veya 'alan,asc|desc'. Alanlar: followUpDate, createdAt, title, "
                    + "projectName. Varsayılan: followUpDate,asc", example = "createdAt,desc")
            @RequestParam(required = false) String sort) {

        TrackingItemFilter filter = new TrackingItemFilter(projectId, type, status, riskLevel, overdue, assignee);
        return trackingItemService.search(filter, page, size, sort);
    }

    @Operation(summary = "Takip kaydı detayı")
    @GetMapping(value = "/api/tracking-items/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TrackingItemResponse get(@PathVariable Long id) {
        return trackingItemService.findById(id);
    }

    @Operation(summary = "Takip kaydını güncelle",
            description = "Kaydı kapatmak için status KAPANDI gönderilir; kapanış tarihini sunucu yazar. Değiştirilmeyen "
                    + "geçmiş bir takip tarihi korunabilir, yeni tarih bugünden önce olamaz.")
    @PutMapping(value = "/api/tracking-items/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TrackingItemResponse update(@PathVariable Long id, @Valid @RequestBody TrackingItemRequest request) {
        return trackingItemService.update(id, request);
    }

    @Operation(summary = "Takip kaydını sil")
    @DeleteMapping("/api/tracking-items/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        trackingItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
