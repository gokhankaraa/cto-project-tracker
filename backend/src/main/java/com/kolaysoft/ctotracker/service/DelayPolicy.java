package com.kolaysoft.ctotracker.service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItem;
import com.kolaysoft.ctotracker.entity.WorkItemStatus;
import com.kolaysoft.ctotracker.repository.WorkItemRepository;
import com.kolaysoft.ctotracker.repository.spec.WorkItemSpecifications;

/**
 * Gecikme kuralı (T13 analiz notu, K-T13-01). Dashboard satırı, iş kalemi yanıtı ve filtreler
 * bu kuralı kullanır; sorgu tarafındaki karşılığı {@link WorkItemSpecifications#delayed(Boolean)}.
 *
 * <p>Referans tarih "bugün" değil, iş kaleminin bağlı olduğu raporun tarihidir. Rapor haftalık bir
 * anlık görüntü olduğu için sonuç saate bağlı olmaz ve geçmiş haftalar kendi tarihlerine göre
 * değerlendirilir.
 */
@Component
public class DelayPolicy {

    private final WorkItemRepository workItemRepository;

    public DelayPolicy(WorkItemRepository workItemRepository) {
        this.workItemRepository = workItemRepository;
    }

    /**
     * İş kalemi gecikmiş mi: durumu GECIKTI ise, ya da planlanan tarihi raporun tarihinden önce
     * olduğu halde henüz TAMAMLANDI değilse.
     */
    public boolean isDelayed(WorkItem item) {
        if (item.getStatus() == WorkItemStatus.GECIKTI) {
            return true;
        }
        LocalDate plannedDate = item.getPlannedDate();
        return plannedDate != null
                && item.getStatus() != WorkItemStatus.TAMAMLANDI
                && plannedDate.isBefore(item.getWeeklyReport().getReportDate());
    }

    /**
     * Rapor (yani projenin o haftası) gecikmede mi: genel durumu GECIKMELI ise ya da raporda en az
     * bir gecikmiş iş kalemi varsa.
     */
    public boolean isDelayed(WeeklyReport report) {
        return report.getOverallStatus() == OverallStatus.GECIKMELI
                || workItemRepository.exists(
                        WorkItemSpecifications.inReport(report.getId())
                                .and(WorkItemSpecifications.delayed(true)));
    }
}
