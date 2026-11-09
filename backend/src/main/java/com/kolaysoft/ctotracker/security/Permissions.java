package com.kolaysoft.ctotracker.security;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

import com.kolaysoft.ctotracker.entity.Role;

/**
 * Yazma islemlerinin rol kurallari. Okuma her rolde serbesttir ama {@link Viewer} ile sinirlidir (proje
 * yoneticisi yalnizca kendi projelerini gorur).
 *
 * <pre>
 *                                   Admin   CTO   Proje yoneticisi
 * Dashboard, listeler, detaylar      tumu   tumu   yalnizca kendi projeleri
 * Proje olustur / duzenle / sil       ✓      ✗     ✗
 * Rapor ve is kalemi yaz / sil        ✓      ✗     yalnizca kendi projelerinde
 * </pre>
 *
 * Kurallar adres + metot olarak guvenlik filtresinde uygulanir, controller'a gelmeden: rolu uymayan istegin
 * govdesi okunmaz, dogrulanmaz, kayit aranmaz; her zaman 403 doner (gecersiz govde 400'u ya da olmayan kayit
 * 404'u rol kontrolunun onune gecmez). Rol uyar ama kayit kullaniciya gorunmuyorsa 404 (servislerdeki
 * gorunurluk kontrolu).
 *
 * <p>Asagida olmayan her yazma istegi reddedilir: yeni bir yazma endpoint'i buraya eklenmeden calismaz,
 * boylece unutulan bir kural endpoint'i herkese acik birakmaz. Frontend ayni tabloyu
 * {@code frontend/src/auth/permissions.ts} ile izler.
 */
public final class Permissions {

    private static final String[] ADMIN_ONLY = {Role.ADMIN.name()};
    private static final String[] REPORT_WRITERS = {Role.ADMIN.name(), Role.PROJECT_MANAGER.name()};

    private Permissions() {
    }

    /** Yazma kurallarini filtre zincirine ekler; acik yollardan (giris, saglik) sonra cagrilir. */
    static void writeRules(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        // Proje tanimlari: yalnizca Admin. "*" tek bir yol parcasidir: /api/projects/1/reports buraya girmez.
        auth.requestMatchers(POST, "/api/projects").hasAnyRole(ADMIN_ONLY)
                .requestMatchers(PUT, "/api/projects/*").hasAnyRole(ADMIN_ONLY)
                .requestMatchers(DELETE, "/api/projects/*").hasAnyRole(ADMIN_ONLY)
                // Haftalik rapor ve is kalemi: Admin ve proje yoneticisi (kendi projesinde). CTO yalnizca okur.
                .requestMatchers(POST, "/api/projects/*/reports").hasAnyRole(REPORT_WRITERS)
                .requestMatchers(PUT, "/api/reports/*").hasAnyRole(REPORT_WRITERS)
                .requestMatchers(DELETE, "/api/reports/*").hasAnyRole(REPORT_WRITERS)
                .requestMatchers(POST, "/api/reports/*/work-items").hasAnyRole(REPORT_WRITERS)
                .requestMatchers(PUT, "/api/work-items/*").hasAnyRole(REPORT_WRITERS)
                .requestMatchers(DELETE, "/api/work-items/*").hasAnyRole(REPORT_WRITERS)
                // Varsayilan ret: kurali yazilmamis yazma istegi kimseye acik degil.
                .requestMatchers(POST, "/api/**").denyAll()
                .requestMatchers(PUT, "/api/**").denyAll()
                .requestMatchers(PATCH, "/api/**").denyAll()
                .requestMatchers(DELETE, "/api/**").denyAll();
    }
}
