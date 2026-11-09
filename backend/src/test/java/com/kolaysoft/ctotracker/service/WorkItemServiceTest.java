package com.kolaysoft.ctotracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kolaysoft.ctotracker.common.exception.ResourceNotFoundException;
import com.kolaysoft.ctotracker.dto.WorkItemRequest;
import com.kolaysoft.ctotracker.dto.WorkItemResponse;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;
import com.kolaysoft.ctotracker.repository.WeeklyReportRepository;
import com.kolaysoft.ctotracker.repository.WorkItemRepository;

/** WorkItemService birim testleri (Spring context'siz, Mockito ile). */
@ExtendWith(MockitoExtension.class)
class WorkItemServiceTest {

    @Mock
    private WorkItemRepository workItemRepository;

    @Mock
    private WeeklyReportRepository weeklyReportRepository;

    @Mock
    private DelayPolicy delayPolicy;

    @InjectMocks
    private WorkItemService service;

    private WeeklyReport report(Long id) {
        // Yanit artik proje bilgisini de iceriyor (T13); rapora bir proje baglanir.
        Project p = new Project();
        p.setId(1L);
        p.setName("Atlas IK");
        WeeklyReport r = new WeeklyReport();
        r.setId(id);
        r.setProject(p);
        r.setWeekNumber(30);
        return r;
    }

    private WorkItemRequest request(String title, WorkItemStatus status) {
        return new WorkItemRequest(title, "aciklama", "Ayse", status, null, null);
    }

    private WorkItem existing(Long id, Long reportId) {
        WorkItem w = new WorkItem();
        w.setId(id);
        w.setWeeklyReport(report(reportId));
        w.setTitle("mevcut");
        w.setStatus(WorkItemStatus.PLANLANDI);
        return w;
    }

    @Test
    @DisplayName("Olusturma: gecerli rapor ile is kalemi olusur ve rapora baglanir")
    void createBindsToReport() {
        when(weeklyReportRepository.findById(1L)).thenReturn(Optional.of(report(1L)));
        when(workItemRepository.save(any())).thenAnswer(inv -> {
            WorkItem w = inv.getArgument(0);
            w.setId(10L);
            return w;
        });

        WorkItemResponse response = service.create(1L, request("API gelistir", WorkItemStatus.DEVAM_EDIYOR));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.weeklyReportId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("API gelistir");
        assertThat(response.status()).isEqualTo(WorkItemStatus.DEVAM_EDIYOR);
    }

    @Test
    @DisplayName("Olusturma: olmayan rapor 404 ResourceNotFound")
    void createRejectsMissingReport() {
        when(weeklyReportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, request("x", WorkItemStatus.PLANLANDI)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(workItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guncelleme: alanlar guncellenir")
    void updateChangesFields() {
        when(workItemRepository.findById(10L)).thenReturn(Optional.of(existing(10L, 1L)));
        when(workItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkItemResponse response = service.update(10L, request("Guncellenmis", WorkItemStatus.TAMAMLANDI));

        assertThat(response.title()).isEqualTo("Guncellenmis");
        assertThat(response.status()).isEqualTo(WorkItemStatus.TAMAMLANDI);
    }

    @Test
    @DisplayName("Guncelleme: olmayan is kalemi 404")
    void updateRejectsMissing() {
        when(workItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("x", WorkItemStatus.PLANLANDI)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Detay: olmayan is kalemi 404")
    void findByIdRejectsMissing() {
        when(workItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Listeleme: olmayan rapor icin 404")
    void findByReportRejectsMissingReport() {
        when(weeklyReportRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findByReport(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Silme: olmayan is kalemi 404")
    void deleteRejectsMissing() {
        when(workItemRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(workItemRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Silme: mevcut is kalemi silinir")
    void deleteRemovesExisting() {
        when(workItemRepository.existsById(10L)).thenReturn(true);

        service.delete(10L);

        verify(workItemRepository).deleteById(10L);
    }
}
