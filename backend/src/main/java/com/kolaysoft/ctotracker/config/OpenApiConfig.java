package com.kolaysoft.ctotracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Swagger UI'da gorunen API bilgileri. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI ctoTrackerOpenApi() {
        // Swagger UI'da "Authorize" dugmesi: /api/auth/login'den alinan token girilir, sonraki denemeler
        // Authorization: Bearer basligiyla gider.
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("POST /api/auth/login yanitindaki token")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .info(new Info()
                .title("Haftalik Proje Durum Raporlama ve CTO Takip Sistemi API")
                .version("0.0.1-SNAPSHOT")
                .description("""
                        Proje yoneticilerinin haftalik proje durum raporlarini girdigi,
                        CTO'nun tum proje portfoyunu tek noktadan izledigi sistemin backend API'si.
                        """)
                .contact(new Contact().name("Gokhan Kara")));
    }
}
