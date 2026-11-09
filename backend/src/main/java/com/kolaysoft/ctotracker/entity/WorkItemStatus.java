package com.kolaysoft.ctotracker.entity;

/**
 * Bir iş kaleminin (WorkItem) durumu (on analiz, bölüm 4).
 * Rapor ilerleme kademesinden (ProgressStage) farklı olarak sıra/geçiş kuralı yoktur;
 * iş kalemi bu durumlardan herhangi birini alabilir.
 */
public enum WorkItemStatus {
    PLANLANDI,
    DEVAM_EDIYOR,
    TESTTE,
    TAMAMLANDI,
    GECIKTI,
    RISKLI,
    BLOKE
}
