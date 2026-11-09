package com.kolaysoft.ctotracker.entity;

/**
 * Sistemdeki kullanici rolleri (on analiz, bolum 3).
 * MVP'de rol bilgisi tutulur; erisim kisitlamasi (RBAC) sonraki asamaya birakilmistir.
 */
public enum Role {

    /** Ana veri giren kullanici; sorumlu oldugu projelere haftalik rapor girer. */
    PROJECT_MANAGER,

    /** Tum projeleri izleyen kullanici; dashboard'dan portfoyu gorur, duzenleme yapmaz. */
    CTO,

    /** Kullanici ve proje tanimlarini yoneten kullanici. */
    ADMIN
}
