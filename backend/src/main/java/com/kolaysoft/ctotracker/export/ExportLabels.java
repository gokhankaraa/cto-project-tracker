package com.kolaysoft.ctotracker.export;

import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;

/**
 * Excel ciktisindaki Turkce etiketler. Ekrandakilerle ayni olmali: frontend/src/lib/labels.ts. Bir enum'a deger
 * eklenince switch'ler derleme hatasi verir (default yok), boylece etiket eksik kalmaz.
 */
final class ExportLabels {

    private ExportLabels() {
    }

    static String of(ProjectStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PLANLANDI -> "Planlandı";
            case DEVAM_EDIYOR -> "Devam ediyor";
            case TAMAMLANDI -> "Tamamlandı";
            case RISKLI -> "Riskli";
            case BLOKE -> "Bloke";
        };
    }

    static String of(OverallStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case YOLUNDA -> "Yolunda";
            case RISKLI -> "Riskli";
            case GECIKMELI -> "Gecikmeli";
            case BLOKE -> "Bloke";
        };
    }

    static String of(RiskLevel level) {
        if (level == null) {
            return null;
        }
        return switch (level) {
            case DUSUK -> "Düşük";
            case ORTA -> "Orta";
            case YUKSEK -> "Yüksek";
        };
    }

    static String of(ProgressStage stage) {
        if (stage == null) {
            return null;
        }
        return switch (stage) {
            case ANALIZ -> "Analiz";
            case GELISTIRME -> "Geliştirme";
            case TEST -> "Test";
            case TAMAMLANDI -> "Tamamlandı";
        };
    }

    static String of(TrackingItemType type) {
        return switch (type) {
            case RISK -> "Risk";
            case ENGEL -> "Engel";
            case KARAR -> "Karar";
            case AKSIYON -> "Aksiyon";
        };
    }

    /** Karar kaydinda durumlar ne oldugunu soyler (ekrandaki gibi): acik karar "bekleniyor". */
    static String of(TrackingItemType type, TrackingItemStatus status) {
        if (type == TrackingItemType.KARAR) {
            return switch (status) {
                case ACIK -> "Karar bekleniyor";
                case DEVAM_EDIYOR -> "Görüşülüyor";
                case KAPANDI -> "Karar verildi";
            };
        }
        return switch (status) {
            case ACIK -> "Açık";
            case DEVAM_EDIYOR -> "Devam ediyor";
            case KAPANDI -> "Kapandı";
        };
    }
}
