package com.kolaysoft.ctotracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Frontend'in (farkli bir adreste calisan React uygulamasi) API'ye tarayicidan erisebilmesi icin CORS.
 *
 * <p>Izin verilen adresler {@code app.cors.allowed-origins} ayarindan gelir (virgulle ayrilmis liste,
 * ortam degiskeni {@code CORS_ALLOWED_ORIGINS}); varsayilan Vite gelistirme sunucusudur
 * ({@code http://localhost:5173}). Listede olmayan bir adresten gelen tarayici istegi 403 ile reddedilir.
 * Yalnizca {@code /api/**} acilir; Swagger UI zaten uygulamanin kendi adresinden calisir.
 *
 * <p>Cerez/kimlik bilgisi gonderimi ({@code allowCredentials}) kapali: henuz oturum yok. Kimlik
 * dogrulama (T14) geldiginde bu karar yeniden ele alinir.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept")
                // Tarayici on kontrol (preflight) yanitini 1 saat onbellekte tutar.
                .maxAge(3600);
    }
}
