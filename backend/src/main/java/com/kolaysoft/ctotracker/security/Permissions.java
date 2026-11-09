package com.kolaysoft.ctotracker.security;

/**
 * Yazma islemlerinin rol kurallari ({@code @PreAuthorize} ifadeleri). Okuma her rolde serbesttir ama
 * {@link Viewer} ile sinirlidir (proje yoneticisi yalnizca kendi projelerini gorur).
 *
 * <pre>
 *                                   Admin   CTO   Proje yoneticisi
 * Dashboard, listeler, detaylar      tumu   tumu   yalnizca kendi projeleri
 * Proje olustur / duzenle / sil       ✓      ✗     ✗
 * Rapor ve is kalemi yaz / sil        ✓      ✗     yalnizca kendi projelerinde
 * </pre>
 *
 * Rol uymazsa 403; rol uyar ama kayit kullaniciya gorunmuyorsa 404 (servislerdeki gorunurluk kontrolu).
 */
public final class Permissions {

    /** Proje tanimlari: yalnizca Admin. */
    public static final String ADMIN_ONLY = "hasRole('ADMIN')";

    /** Haftalik rapor ve is kalemi yazma: Admin ve proje yoneticisi (kendi projesinde). CTO yalnizca okur. */
    public static final String REPORT_WRITERS = "hasAnyRole('ADMIN', 'PROJECT_MANAGER')";

    private Permissions() {
    }
}
