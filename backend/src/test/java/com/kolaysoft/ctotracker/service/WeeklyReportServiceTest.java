package com.kolaysoft.ctotracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kolaysoft.ctotracker.common.exception.BusinessRuleException;
import com.kolaysoft.ctotracker.common.exception.DuplicateResourceException;
import com.kolaysoft.ctotracker.common.exception.ResourceNotFoundException;
import com.kolaysoft.ctotracker.dto.WeeklyReportRequest;
import com.kolaysoft.ctotracker.dto.WeeklyReportResponse;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.ProgressStage;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.RiskLevel;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.repository.ProjectRepository;
import com.kolaysoft.ctotracker.repository.WeeklyReportRepository;
import com.kolaysoft.ctotracker.repository.WorkItemRepository;

/** WeeklyReportService is kurallarinin birim testleri (Spring context'siz, Mockito ile). */
@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

    @Mock
    private WeeklyReportRepository weeklyReportRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkItemRepository workItemRepository;

    @InjectMocks
    private WeeklyReportService service;

    private Project project() {
        Project p = new Project();
        p.setId(1L);
        p.setName("Atlas IK");
        return p;
    }

    private WeeklyReportRequest request(int week, ProgressStage stage) {
        return request(week, LocalDate.of(2026, 7, 27), stage);
    }

    private WeeklyReportRequest request(int week, LocalDate reportDate, ProgressStage stage) {
        return new WeeklyReportRequest(week, reportDate, stage,
                OverallStatus.YOLUNDA, RiskLevel.ORTA, "yapilanlar", "yapilacaklar", "riskler", "not");
    }

    private WeeklyReport existingReport(Long id, int week, ProgressStage stage) {
        WeeklyReport r = new WeeklyReport();
        r.setId(id);
        r.setProject(project());
        r.setWeekNumber(week);
        r.setReportDate(LocalDate.of(2026, 7, 20));
        r.setProgressStage(stage);
        r.setOverallStatus(OverallStatus.YOLUNDA);
        r.setRiskLevel(RiskLevel.ORTA);
        return r;
    }

    @Test
    @DisplayName("Olusturma: yuzde ilerleme kademesinden turer")
    void createDerivesPercentageFromStage() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project()));
        when(weeklyReportRepository.existsByProjectIdAndYearAndWeekNumber(1L, 2026, 30)).thenReturn(false);
        when(weeklyReportRepository.save(any())).thenAnswer(inv -> {
            WeeklyReport r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });

        WeeklyReportResponse response = service.create(1L, request(30, ProgressStage.GELISTIRME));

        assertThat(response.progressStage()).isEqualTo(ProgressStage.GELISTIRME);
        assertThat(response.progressPercentage()).isEqualTo(50);
        assertThat(response.projectId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Olusturma: ayni proje+hafta icin ikinci rapor 409 DuplicateResource")
    void createRejectsDuplicateProjectWeek() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project()));
        when(weeklyReportRepository.existsByProjectIdAndYearAndWeekNumber(1L, 2026, 30)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, request(30, ProgressStage.ANALIZ)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Bu proje icin 2026 yilinin 30. haftasina ait rapor zaten mevcut.");

        verify(weeklyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Olusturma: yil rapor tarihinin ISO hafta-yilindan turer (2027-01-01 Cuma -> 2026)")
    void createDerivesYearFromIsoWeekYear() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project()));
        when(weeklyReportRepository.existsByProjectIdAndYearAndWeekNumber(1L, 2026, 53)).thenReturn(false);
        when(weeklyReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WeeklyReportResponse response = service.create(1L, request(53, LocalDate.of(2027, 1, 1), ProgressStage.ANALIZ));

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.weekNumber()).isEqualTo(53);
    }

    @Test
    @DisplayName("Olusturma: olmayan proje 404 ResourceNotFound")
    void createRejectsMissingProject() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, request(30, ProgressStage.ANALIZ)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Guncelleme: kademe sirasi atlanamaz (ANALIZ -> TEST) 400 BusinessRule")
    void updateRejectsStageSkip() {
        when(weeklyReportRepository.findById(5L)).thenReturn(Optional.of(existingReport(5L, 30, ProgressStage.ANALIZ)));

        assertThatThrownBy(() -> service.update(5L, request(30, ProgressStage.TEST)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("atlanamaz");

        verify(weeklyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guncelleme: kademe geriye alinamaz (TEST -> ANALIZ) 400 BusinessRule")
    void updateRejectsStageBackward() {
        when(weeklyReportRepository.findById(5L)).thenReturn(Optional.of(existingReport(5L, 30, ProgressStage.TEST)));

        assertThatThrownBy(() -> service.update(5L, request(30, ProgressStage.ANALIZ)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("geriye");

        verify(weeklyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guncelleme: tam bir sonraki kademeye gecis kabul edilir (ANALIZ -> GELISTIRME)")
    void updateAllowsSingleStepAdvance() {
        when(weeklyReportRepository.findById(5L)).thenReturn(Optional.of(existingReport(5L, 30, ProgressStage.ANALIZ)));
        when(weeklyReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WeeklyReportResponse response = service.update(5L, request(30, ProgressStage.GELISTIRME));

        assertThat(response.progressStage()).isEqualTo(ProgressStage.GELISTIRME);
        assertThat(response.progressPercentage()).isEqualTo(50);
        verify(weeklyReportRepository).save(any());
    }

    @Test
    @DisplayName("Guncelleme: hafta degisip cakisirsa 409 DuplicateResource")
    void updateRejectsWeekCollision() {
        when(weeklyReportRepository.findById(5L)).thenReturn(Optional.of(existingReport(5L, 30, ProgressStage.ANALIZ)));
        when(weeklyReportRepository.existsByProjectIdAndYearAndWeekNumber(1L, 2026, 31)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request(31, ProgressStage.ANALIZ)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(weeklyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Guncelleme: tarih baska yila tasinip ayni haftayla cakisirsa 409 DuplicateResource")
    void updateRejectsYearCollision() {
        // Mevcut rapor 2026 hafta 30; tarih 2027'ye tasiniyor, 2027 hafta 30 zaten var
        when(weeklyReportRepository.findById(5L)).thenReturn(Optional.of(existingReport(5L, 30, ProgressStage.ANALIZ)));
        when(weeklyReportRepository.existsByProjectIdAndYearAndWeekNumber(1L, 2027, 30)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request(30, LocalDate.of(2027, 7, 26), ProgressStage.ANALIZ)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("2027 yilinin 30. haftasina");

        verify(weeklyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Detay: olmayan rapor 404 ResourceNotFound")
    void findByIdRejectsMissingReport() {
        when(weeklyReportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Listeleme: olmayan proje icin 404 ResourceNotFound")
    void findByProjectRejectsMissingProject() {
        when(projectRepository.existsById(eq(99L))).thenReturn(false);

        assertThatThrownBy(() -> service.findByProject(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
