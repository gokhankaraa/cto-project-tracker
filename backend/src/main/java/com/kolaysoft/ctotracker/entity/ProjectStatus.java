package com.kolaysoft.ctotracker.entity;

/**
 * Projenin genel durumu (on analiz, bolum 4).
 * MVP'de kodda sabit enum olarak tutulur; admin tarafindan dinamik yonetilmez.
 */
public enum ProjectStatus {
    PLANLANDI,
    DEVAM_EDIYOR,
    TAMAMLANDI,
    RISKLI,
    BLOKE
}
