package com.kolaysoft.ctotracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kolaysoft.ctotracker.config.ClockConfig;
import com.kolaysoft.ctotracker.entity.AuditAction;
import com.kolaysoft.ctotracker.entity.AuditChange;
import com.kolaysoft.ctotracker.entity.AuditEntityType;
import com.kolaysoft.ctotracker.entity.AuditEvent;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.ProjectStatus;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.TrackingItem;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.repository.AuditEventRepository;
import com.kolaysoft.ctotracker.security.Actor;
import com.kolaysoft.ctotracker.security.CurrentUser;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T09:30:00Z");

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private CurrentUser currentUser;

    private AuditService service;

    @BeforeEach
    void setUp() {
        lenient().when(currentUser.actor()).thenReturn(new Actor(2L, "Mehmet Kaya"));
        service = new AuditService(auditEventRepository, currentUser, Clock.fixed(NOW, ClockConfig.ZONE));
    }

    private AuditEvent saved() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        return captor.getValue();
    }

    private static TrackingItem blocker() {
        Project project = new Project();
        project.setId(2L);
        TrackingItem item = new TrackingItem();
        item.setId(4L);
        item.setProject(project);
        item.setType(TrackingItemType.ENGEL);
        item.setTitle("Test ortamina erisim yok");
        item.setRiskLevel(RiskLevel.ORTA);
        item.setAssignee("Mehmet Kaya");
        item.setFollowUpDate(LocalDate.of(2026, 11, 13));
        item.setStatus(TrackingItemStatus.DEVAM_EDIYOR);
        return item;
    }

    @Test
    @DisplayName("Ekleme: dolu alanlar ilk degerleriyle, alan sirasinda; yazan kisi ve zaman oturumdan ve saatten")
    void createdWritesFilledFields() {
        service.created(AuditSubject.of(blocker()));

        AuditEvent event = saved();
        assertThat(event.getAction()).isEqualTo(AuditAction.OLUSTURULDU);
        assertThat(event.getEntityType()).isEqualTo(AuditEntityType.TAKIP_KAYDI);
        assertThat(event.getEntityId()).isEqualTo(4L);
        assertThat(event.getProjectId()).isEqualTo(2L);
        assertThat(event.getSummary()).isEqualTo("Test ortamina erisim yok");
        assertThat(event.getActorId()).isEqualTo(2L);
        assertThat(event.getActorName()).isEqualTo("Mehmet Kaya");
        assertThat(event.getOccurredAt()).isEqualTo(NOW);
        assertThat(event.getChanges())
                .extracting(AuditChange::getField, AuditChange::getOldValue, AuditChange::getNewValue)
                .containsExactly(
                        tuple("type", null, "ENGEL"),
                        tuple("title", null, "Test ortamina erisim yok"),
                        tuple("riskLevel", null, "ORTA"),
                        tuple("assignee", null, "Mehmet Kaya"),
                        tuple("followUpDate", null, "2026-11-13"),
                        tuple("status", null, "DEVAM_EDIYOR"));
    }

    @Test
    @DisplayName("Guncelleme: yalnizca degisen alanlar, eski ve yeni degerleriyle")
    void updatedWritesOnlyChangedFields() {
        TrackingItem item = blocker();
        AuditSubject before = AuditSubject.of(item);
        item.setRiskLevel(RiskLevel.YUKSEK);
        item.setStatus(TrackingItemStatus.KAPANDI);
        item.setClosedDate(LocalDate.of(2026, 11, 10));

        service.updated(before, AuditSubject.of(item));

        AuditEvent event = saved();
        assertThat(event.getAction()).isEqualTo(AuditAction.GUNCELLENDI);
        assertThat(event.getChanges())
                .extracting(AuditChange::getField, AuditChange::getOldValue, AuditChange::getNewValue)
                .containsExactly(
                        tuple("riskLevel", "ORTA", "YUKSEK"),
                        tuple("status", "DEVAM_EDIYOR", "KAPANDI"),
                        tuple("closedDate", null, "2026-11-10"));
    }

    @Test
    @DisplayName("Hicbir alan degismediyse olay acilmaz; bos metin ile null ayni sayilir")
    void noChangeNoEvent() {
        TrackingItem item = blocker();
        AuditSubject before = AuditSubject.of(item);
        item.setDescription("   ");

        service.updated(before, AuditSubject.of(item));

        verify(auditEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Silme: ozet kalir, alan yazilmaz")
    void deletedKeepsSummaryOnly() {
        service.deleted(AuditSubject.of(blocker()));

        AuditEvent event = saved();
        assertThat(event.getAction()).isEqualTo(AuditAction.SILINDI);
        assertThat(event.getSummary()).isEqualTo("Test ortamina erisim yok");
        assertThat(event.getChanges()).isEmpty();
    }

    @Test
    @DisplayName("Goruntu: rapor ozeti hafta, proje sorumlusu adiyla; uzun metin 500 karakterde kesilir")
    void snapshotValues() {
        User owner = new User();
        owner.setFullName("Ayse Yilmaz");
        Project project = new Project();
        project.setId(1L);
        project.setName("Atlas IK");
        project.setStatus(ProjectStatus.DEVAM_EDIYOR);
        project.setOwner(owner);
        assertThat(AuditSubject.of(project).values()).containsEntry("owner", "Ayse Yilmaz")
                .containsEntry("status", "DEVAM_EDIYOR");

        WeeklyReport report = new WeeklyReport();
        report.setProject(project);
        report.setWeekNumber(1);
        report.setReportDate(LocalDate.of(2026, 12, 30)); // 2027'nin 1. haftasi
        report.setProgressStage(ProgressStage.ANALIZ);
        report.setOverallStatus(OverallStatus.YOLUNDA);
        report.setDone("x".repeat(2000));
        AuditSubject subject = AuditSubject.of(report);
        assertThat(subject.summary()).isEqualTo("2027 / 1. hafta");
        assertThat(subject.projectId()).isEqualTo(1L);
        assertThat(subject.values().get("done")).hasSize(500).endsWith("…");
        assertThat(subject.values()).containsEntry("year", "2027").containsEntry("reportDate", "2026-12-30");
    }
}
