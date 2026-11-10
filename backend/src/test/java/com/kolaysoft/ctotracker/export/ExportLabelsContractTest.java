package com.kolaysoft.ctotracker.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kolaysoft.ctotracker.FrontendSources;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;

/**
 * Excel ciktisindaki Turkce etiketler ({@link ExportLabels}) ekrandakilerle ({@code frontend/src/lib/labels.ts}) ayni
 * mi. Etiketler iki dilde iki yerde durur; biri degisip digeri unutulursa burada kirilir.
 */
class ExportLabelsContractTest {

    private static <E extends Enum<E>> Map<String, String> javaLabels(Class<E> type, Function<E, String> label) {
        return Arrays.stream(type.getEnumConstants()).collect(Collectors.toMap(Enum::name, label));
    }

    @Test
    @DisplayName("Proje durumu, genel durum, risk, asama ve takip kaydi turu etiketleri ekrandakiyle ayni")
    void enumLabelsMatchScreen() {
        String labels = FrontendSources.read("lib/labels.ts");
        assertThat(javaLabels(ProjectStatus.class, ExportLabels::of))
                .isEqualTo(FrontendSources.enumLabels(labels, "PROJECT_STATUS_LABELS"));
        assertThat(javaLabels(OverallStatus.class, ExportLabels::of))
                .isEqualTo(FrontendSources.enumLabels(labels, "OVERALL_STATUS_LABELS"));
        assertThat(javaLabels(RiskLevel.class, ExportLabels::of))
                .isEqualTo(FrontendSources.enumLabels(labels, "RISK_LEVEL_LABELS"));
        assertThat(javaLabels(ProgressStage.class, ExportLabels::of))
                .isEqualTo(FrontendSources.enumLabels(labels, "PROGRESS_STAGE_LABELS"));
        assertThat(javaLabels(TrackingItemType.class, ExportLabels::of))
                .isEqualTo(FrontendSources.enumLabels(labels, "TRACKING_ITEM_TYPE_LABELS"));
    }

    @Test
    @DisplayName("Takip kaydi durumu: kararda ozel etiketler (bekleniyor / verildi), digerlerinde genel")
    void trackingStatusLabelsMatchScreen() {
        String labels = FrontendSources.read("lib/labels.ts");
        assertThat(javaLabels(TrackingItemStatus.class, status -> ExportLabels.of(TrackingItemType.RISK, status)))
                .isEqualTo(FrontendSources.enumLabels(labels, "TRACKING_ITEM_STATUS_LABELS"));
        assertThat(javaLabels(TrackingItemStatus.class, status -> ExportLabels.of(TrackingItemType.KARAR, status)))
                .isEqualTo(FrontendSources.enumLabels(labels, "DECISION_STATUS_LABELS"));
    }
}
