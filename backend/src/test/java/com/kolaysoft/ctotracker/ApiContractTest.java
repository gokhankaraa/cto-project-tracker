package com.kolaysoft.ctotracker;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kolaysoft.ctotracker.dto.AuditEventResponse;
import com.kolaysoft.ctotracker.dto.DashboardProjectSummary;
import com.kolaysoft.ctotracker.dto.DashboardSummaryResponse;
import com.kolaysoft.ctotracker.dto.DashboardTrendResponse;
import com.kolaysoft.ctotracker.dto.HealthResponse;
import com.kolaysoft.ctotracker.dto.LoginRequest;
import com.kolaysoft.ctotracker.dto.LoginResponse;
import com.kolaysoft.ctotracker.dto.PageResponse;
import com.kolaysoft.ctotracker.dto.ProjectRequest;
import com.kolaysoft.ctotracker.dto.ProjectResponse;
import com.kolaysoft.ctotracker.dto.TrackingItemRequest;
import com.kolaysoft.ctotracker.dto.TrackingItemResponse;
import com.kolaysoft.ctotracker.dto.TrendWeek;
import com.kolaysoft.ctotracker.dto.UserResponse;
import com.kolaysoft.ctotracker.dto.WeeklyReportRequest;
import com.kolaysoft.ctotracker.dto.WeeklyReportResponse;
import com.kolaysoft.ctotracker.dto.WorkItemRequest;
import com.kolaysoft.ctotracker.dto.WorkItemResponse;
import com.kolaysoft.ctotracker.entity.AuditAction;
import com.kolaysoft.ctotracker.entity.AuditEntityType;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;

/**
 * Backend ile on yuz arasindaki sozlesme: on yuzun elle yazilan tipleri ({@code frontend/src/api}) backend DTO'lari ve
 * enum'lariyla ayni mi. Bir DTO'ya alan eklenir, kaldirilir ya da adi degisirse; bir enum'a deger eklenir ya da sirasi
 * degisirse burada kirilir (TypeScript derleyicisi backend'i goremez). Java tarafi reflection ile okunur.
 */
class ApiContractTest {

    /** JSON olarak giden ya da gelen her DTO ve on yuzdeki karsiligi. */
    private static final Map<Class<? extends Record>, String> DTOS = Map.ofEntries(
            Map.entry(ProjectResponse.class, "Project"),
            Map.entry(ProjectRequest.class, "ProjectRequest"),
            Map.entry(WeeklyReportResponse.class, "WeeklyReport"),
            Map.entry(WeeklyReportRequest.class, "WeeklyReportRequest"),
            Map.entry(WorkItemResponse.class, "WorkItem"),
            Map.entry(WorkItemRequest.class, "WorkItemRequest"),
            Map.entry(TrackingItemResponse.class, "TrackingItem"),
            Map.entry(TrackingItemRequest.class, "TrackingItemRequest"),
            Map.entry(UserResponse.class, "User"),
            Map.entry(LoginRequest.class, "LoginRequest"),
            Map.entry(LoginResponse.class, "LoginResponse"),
            Map.entry(PageResponse.class, "PageResponse"),
            Map.entry(DashboardSummaryResponse.class, "DashboardSummary"),
            Map.entry(DashboardProjectSummary.class, "DashboardProjectSummary"),
            Map.entry(DashboardTrendResponse.class, "DashboardTrend"),
            Map.entry(TrendWeek.class, "TrendWeek"),
            Map.entry(HealthResponse.class, "HealthResponse"),
            Map.entry(AuditEventResponse.class, "AuditEvent"),
            Map.entry(AuditEventResponse.Change.class, "AuditChange"));

    /** Enum'lar ve on yuzdeki deger dizileri (sira da ayni olmali: secim kutulari bu sirayi gosterir). */
    private static final Map<Class<? extends Enum<?>>, String> ENUMS = Map.of(
            ProjectStatus.class, "PROJECT_STATUSES",
            OverallStatus.class, "OVERALL_STATUSES",
            RiskLevel.class, "RISK_LEVELS",
            ProgressStage.class, "PROGRESS_STAGES",
            WorkItemStatus.class, "WORK_ITEM_STATUSES",
            TrackingItemType.class, "TRACKING_ITEM_TYPES",
            TrackingItemStatus.class, "TRACKING_ITEM_STATUSES",
            Role.class, "ROLES",
            AuditAction.class, "AUDIT_ACTIONS",
            AuditEntityType.class, "AUDIT_ENTITY_TYPES");

    @Test
    @DisplayName("Her DTO'nun alanlari on yuzdeki TypeScript tipiyle ayni")
    void dtoFieldsMatchTypeScript() {
        String source = FrontendSources.readApiSources();
        DTOS.forEach((dto, tsName) -> {
            Set<String> javaFields = Arrays.stream(dto.getRecordComponents()).map(RecordComponent::getName)
                    .collect(Collectors.toSet());
            assertThat(FrontendSources.interfaceFields(source, tsName))
                    .as("%s <-> TypeScript %s", dto.getSimpleName(), tsName)
                    .containsExactlyInAnyOrderElementsOf(javaFields);
        });
    }

    @Test
    @DisplayName("Her enum'un degerleri on yuzde ayni ve ayni sirada")
    void enumValuesMatchTypeScript() {
        String types = FrontendSources.read("api/types.ts");
        ENUMS.forEach((type, tsName) -> assertThat(FrontendSources.constArray(types, tsName))
                .as("%s <-> TypeScript %s", type.getSimpleName(), tsName)
                .containsExactlyElementsOf(Arrays.stream(type.getEnumConstants()).map(Enum::name).toList()));
    }

    @Test
    @DisplayName("Yeni bir DTO bu teste eklenmeden gecmez")
    void everyDtoIsCovered() throws IOException {
        Path dtoDir = Path.of("src/main/java/com/kolaysoft/ctotracker/dto");
        Set<String> covered = DTOS.keySet().stream()
                .map(Class::getSimpleName).collect(Collectors.toSet());
        try (Stream<Path> files = Files.list(dtoDir)) {
            // dto/filter alt paketi JSON degil, istek parametrelerinin icteki karsiligi.
            Set<String> dtoFiles = files.filter(f -> f.toString().endsWith(".java"))
                    .map(f -> f.getFileName().toString().replace(".java", "")).collect(Collectors.toSet());
            assertThat(covered).containsAll(dtoFiles);
        }
    }
}
