package com.kolaysoft.ctotracker.dto;

import java.time.LocalDate;

import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Takip kaydi olusturma/guncelleme istegi. Bagli oldugu proje URL yolundan gelir.
 * Alanlar arasi kurallar (risk seviyesi, acik kaydin sorumlusu ve takip tarihi) serviste denetlenir.
 */
@Schema(description = "Risk / engel / karar / aksiyon kaydı oluşturma ve güncelleme isteği")
public record TrackingItemRequest(

        @Schema(description = "Kaydın türü", example = "RISK")
        @NotNull(message = "Tür zorunludur.")
        TrackingItemType type,

        @Schema(description = "Başlık", example = "Entegrasyon API'si geç teslim edilebilir")
        @NotBlank(message = "Başlık zorunludur.")
        @Size(max = 255, message = "Başlık en fazla 255 karakter olabilir.")
        String title,

        @Schema(description = "Açıklama")
        @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir.")
        String description,

        @Schema(description = "Risk seviyesi: risk ve engel kayıtlarında zorunlu, karar ve aksiyon kayıtlarında "
                + "verilmez", example = "YUKSEK")
        RiskLevel riskLevel,

        @Schema(description = "Etki: gerçekleşirse ne olur", example = "Rapor ekranı entegrasyonu gecikir")
        @Size(max = 2000, message = "Etki en fazla 2000 karakter olabilir.")
        String impact,

        @Schema(description = "Aksiyon planı: ne yapılacak", example = "Müşteriyle haftalık teslim takvimi")
        @Size(max = 2000, message = "Aksiyon planı en fazla 2000 karakter olabilir.")
        String actionPlan,

        @Schema(description = "Sorumlu (serbest metin); kapalı olmayan kayıtta zorunlu", example = "Ayşe Yılmaz")
        @Size(max = 255, message = "Sorumlu en fazla 255 karakter olabilir.")
        String assignee,

        @Schema(description = "Takip tarihi; kapalı olmayan kayıtta zorunlu. Yeni ya da değiştirilen tarih "
                + "bugünden önce olamaz", example = "2026-11-20")
        LocalDate followUpDate,

        @Schema(description = "Durum", example = "ACIK")
        @NotNull(message = "Durum zorunludur.")
        TrackingItemStatus status) {
}
