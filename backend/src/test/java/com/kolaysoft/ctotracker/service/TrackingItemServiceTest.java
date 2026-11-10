package com.kolaysoft.ctotracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kolaysoft.ctotracker.common.exception.InvalidParameterException;
import com.kolaysoft.ctotracker.common.exception.ResourceNotFoundException;
import com.kolaysoft.ctotracker.config.ClockConfig;
import com.kolaysoft.ctotracker.dto.TrackingItemRequest;
import com.kolaysoft.ctotracker.dto.TrackingItemResponse;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.TrackingItem;
import com.kolaysoft.ctotracker.entity.TrackingItemStatus;
import com.kolaysoft.ctotracker.entity.TrackingItemType;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
import com.kolaysoft.ctotracker.repository.TrackingItemRepository;
import com.kolaysoft.ctotracker.security.CurrentUser;
import com.kolaysoft.ctotracker.security.Viewer;

/** TrackingItemService is kurallari. Saat 2026-11-10 (Istanbul) ogleye sabitlenir. */
@ExtendWith(MockitoExtension.class)
class TrackingItemServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T09:00:00Z"), ClockConfig.ZONE);

    @Mock
    private TrackingItemRepository trackingItemRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CurrentUser currentUser;

    private TrackingItemService service;

    private Project project;

    @BeforeEach
    void setUp() {
        // Bu testler is kurallarini sinar; oturumdaki kullanici her projeyi goren Admin'dir.
        lenient().when(currentUser.viewer()).thenReturn(new Viewer(99L, Role.ADMIN));
        lenient().when(trackingItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new TrackingItemService(trackingItemRepository, projectRepository, currentUser, CLOCK);

        User owner = new User();
        owner.setId(1L);
        project = new Project();
        project.setId(1L);
        project.setName("Atlas IK");
        project.setOwner(owner);
        lenient().when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    }

    private static TrackingItemRequest risk(TrackingItemStatus status, LocalDate followUp) {
        return new TrackingItemRequest(TrackingItemType.RISK, "API gec gelebilir", null, RiskLevel.YUKSEK,
                "Entegrasyon gecikir", "Takvim yapilacak", "Ayse", followUp, status);
    }

    private static TrackingItemRequest action(String assignee, LocalDate followUp, TrackingItemStatus status) {
        return new TrackingItemRequest(TrackingItemType.AKSIYON, "Kapsam toplantisi", null, null,
                null, null, assignee, followUp, status);
    }

    private TrackingItem stored(TrackingItemStatus status, LocalDate followUp, LocalDate closedDate) {
        TrackingItem item = new TrackingItem();
        item.setId(7L);
        item.setProject(project);
        item.setType(TrackingItemType.RISK);
        item.setTitle("API gec gelebilir");
        item.setRiskLevel(RiskLevel.YUKSEK);
        item.setAssignee("Ayse");
        item.setFollowUpDate(followUp);
        item.setStatus(status);
        item.setClosedDate(closedDate);
        item.setCreatedAt(Instant.parse("2026-11-01T08:00:00Z"));
        when(trackingItemRepository.findById(7L)).thenReturn(Optional.of(item));
        return item;
    }

    private static void assertFieldError(Runnable call, String field) {
        assertThatThrownBy(call::run)
                .isInstanceOf(InvalidParameterException.class)
                .extracting(e -> ((InvalidParameterException) e).getParameter())
                .isEqualTo(field);
    }

    @Test
    @DisplayName("Olusturma: proje baglanir, olusturma zamani saatten gelir, acik kayitta kapanis tarihi yok")
    void createsOpenItem() {
        TrackingItemResponse response = service.create(1L, risk(TrackingItemStatus.ACIK, TODAY.plusDays(7)));

        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.projectName()).isEqualTo("Atlas IK");
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-11-10T09:00:00Z"));
        assertThat(response.closedDate()).isNull();
        assertThat(response.overdue()).isFalse();
    }

    @Test
    @DisplayName("Risk ve engelde risk seviyesi zorunlu; karar ve aksiyonda verilemez")
    void riskLevelDependsOnType() {
        TrackingItemRequest riskWithoutLevel = new TrackingItemRequest(TrackingItemType.ENGEL, "Erisim yok", null,
                null, null, null, "Mehmet", TODAY, TrackingItemStatus.ACIK);
        assertFieldError(() -> service.create(1L, riskWithoutLevel), "riskLevel");

        TrackingItemRequest decisionWithLevel = new TrackingItemRequest(TrackingItemType.KARAR, "Saglayici", null,
                RiskLevel.ORTA, null, null, "Zeynep", TODAY, TrackingItemStatus.ACIK);
        assertFieldError(() -> service.create(1L, decisionWithLevel), "riskLevel");

        verify(trackingItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kapanmamis kaydin sorumlusu ve takip tarihi zorunlu; bosluktan olusan sorumlu da bos sayilir")
    void openItemNeedsAssigneeAndFollowUp() {
        assertFieldError(() -> service.create(1L, action(null, TODAY, TrackingItemStatus.ACIK)), "assignee");
        assertFieldError(() -> service.create(1L, action("   ", TODAY, TrackingItemStatus.DEVAM_EDIYOR)), "assignee");
        assertFieldError(() -> service.create(1L, action("Ayse", null, TrackingItemStatus.ACIK)), "followUpDate");
    }

    @Test
    @DisplayName("Kapali kayit sorumlusuz ve tarihsiz olusturulabilir; kapanis tarihi bugun")
    void closedItemNeedsNeither() {
        TrackingItemResponse response = service.create(1L, action(null, null, TrackingItemStatus.KAPANDI));

        assertThat(response.closedDate()).isEqualTo(TODAY);
        assertThat(response.assignee()).isNull();
        assertThat(response.overdue()).isFalse();
    }

    @Test
    @DisplayName("Olusturmada takip tarihi bugunden once olamaz; bugun olabilir")
    void followUpCannotStartInThePast() {
        assertFieldError(() -> service.create(1L, action("Ayse", TODAY.minusDays(1), TrackingItemStatus.ACIK)),
                "followUpDate");

        assertThat(service.create(1L, action("Ayse", TODAY, TrackingItemStatus.ACIK)).overdue()).isFalse();
    }

    @Test
    @DisplayName("Guncellemede degismeyen gecmis takip tarihi korunur ve kayit gecikmis gorunur")
    void unchangedPastFollowUpIsKept() {
        stored(TrackingItemStatus.ACIK, TODAY.minusDays(3), null);

        TrackingItemResponse response = service.update(7L, risk(TrackingItemStatus.DEVAM_EDIYOR, TODAY.minusDays(3)));

        assertThat(response.followUpDate()).isEqualTo(TODAY.minusDays(3));
        assertThat(response.overdue()).isTrue();
    }

    @Test
    @DisplayName("Guncellemede baska bir gecmis tarihe tasimak reddedilir")
    void changedFollowUpCannotBeInThePast() {
        stored(TrackingItemStatus.ACIK, TODAY.minusDays(3), null);

        assertFieldError(() -> service.update(7L, risk(TrackingItemStatus.ACIK, TODAY.minusDays(1))), "followUpDate");
    }

    @Test
    @DisplayName("Gecikmis kayit kapaninca gecikme biter ve kapanis tarihi bugun olur")
    void closingAnOverdueItem() {
        TrackingItem item = stored(TrackingItemStatus.ACIK, TODAY.minusDays(3), null);

        TrackingItemResponse response = service.update(7L, risk(TrackingItemStatus.KAPANDI, TODAY.minusDays(3)));

        assertThat(response.overdue()).isFalse();
        assertThat(item.getClosedDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("Kapali kalan kaydin ilk kapanis tarihi korunur; yeniden acilinca silinir")
    void closedDateFollowsStatus() {
        TrackingItem item = stored(TrackingItemStatus.KAPANDI, null, TODAY.minusDays(5));

        service.update(7L, risk(TrackingItemStatus.KAPANDI, null));
        assertThat(item.getClosedDate()).isEqualTo(TODAY.minusDays(5));

        service.update(7L, risk(TrackingItemStatus.ACIK, TODAY.plusDays(2)));
        assertThat(item.getClosedDate()).isNull();
    }

    @Test
    @DisplayName("Proje listesi: acik kayitlar takip tarihine gore, kapalilar en altta")
    void projectListOrder() {
        TrackingItem later = new TrackingItem();
        later.setId(1L);
        later.setStatus(TrackingItemStatus.ACIK);
        later.setFollowUpDate(TODAY.plusDays(9));
        TrackingItem closed = new TrackingItem();
        closed.setId(2L);
        closed.setStatus(TrackingItemStatus.KAPANDI);
        closed.setClosedDate(TODAY);
        TrackingItem sooner = new TrackingItem();
        sooner.setId(3L);
        sooner.setStatus(TrackingItemStatus.DEVAM_EDIYOR);
        sooner.setFollowUpDate(TODAY.plusDays(1));
        for (TrackingItem item : List.of(later, closed, sooner)) {
            item.setProject(project);
            item.setType(TrackingItemType.AKSIYON);
        }
        when(trackingItemRepository.findByProjectId(1L)).thenReturn(List.of(later, closed, sooner));

        assertThat(service.findByProject(1L)).extracting(TrackingItemResponse::id).containsExactly(3L, 1L, 2L);
    }

    @Test
    @DisplayName("Proje yoneticisi baskasinin projesindeki kayda ve projesine 404 alir")
    void othersItemsAreNotFound() {
        when(currentUser.viewer()).thenReturn(new Viewer(2L, Role.PROJECT_MANAGER));
        stored(TrackingItemStatus.ACIK, TODAY, null);

        assertThatThrownBy(() -> service.findById(7L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.create(1L, risk(TrackingItemStatus.ACIK, TODAY)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(trackingItemRepository, never()).save(any());
    }
}
