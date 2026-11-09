package com.kolaysoft.ctotracker.entity;

/**
 * Haftalik raporun genel durum notu (on analiz, ekran E-04).
 * Takvim ve risk seviyesinden ayri bir kavramdir.
 */
public enum OverallStatus {
    YOLUNDA,
    RISKLI,
    GECIKMELI,
    BLOKE
}
