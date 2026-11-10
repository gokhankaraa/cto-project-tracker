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
import org.springframework.web.bind.annotation.RestController;

import com.kolaysoft.ctotracker.dto.TrackingItemRequest;
import com.kolaysoft.ctotracker.dto.TrackingItemResponse;
import com.kolaysoft.ctotracker.service.TrackingItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Risk, engel, karar ve aksiyon kayitlari. Olusturma/listeleme proje altindan
 * ({@code /api/projects/{projectId}/tracking-items}); tekil islemler kayit id'si uzerinden
 * ({@code /api/tracking-items/{id}}) yapilir. Yazma yetkisi raporlarinkiyle aynidir ({@code Permissions}).
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
