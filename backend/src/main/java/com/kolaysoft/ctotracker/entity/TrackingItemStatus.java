package com.kolaysoft.ctotracker.entity;

/**
 * Takip kaydinin durumu. Karar kaydinda acik "karar bekleniyor", kapali "karar verildi" demektir.
 * Kapali olmayan her kayit "acik" sayilir: sorumlusu ve takip tarihi zorunludur, gecikebilir.
 */
public enum TrackingItemStatus {
    ACIK,
    DEVAM_EDIYOR,
    KAPANDI;

    public boolean isOpen() {
        return this != KAPANDI;
    }
}
