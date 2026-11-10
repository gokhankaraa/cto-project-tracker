package com.kolaysoft.ctotracker.entity;

/** Takip kaydinin turu. Risk seviyesi yalnizca {@link #RISK} ve {@link #ENGEL} kayitlarinda vardir. */
public enum TrackingItemType {
    /** Gerceklesebilecek bir sorun. */
    RISK,
    /** Gerceklesmis, isi durduran ya da yavaslatan bir sorun. */
    ENGEL,
    /** Verilmesi gereken (acik) ya da verilmis (kapali) bir karar. */
    KARAR,
    /** Yapilacak bir is; sorumlusu ve takip tarihi vardir. */
    AKSIYON;

    public boolean hasRiskLevel() {
        return this == RISK || this == ENGEL;
    }
}
