package com.kolaysoft.ctotracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

/** Swagger UI'da gorunen API bilgileri. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ctoTrackerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Haftalik Proje Durum Raporlama ve CTO Takip Sistemi API")
                .version("0.0.1-SNAPSHOT")
                .description("""
                        Proje yoneticilerinin haftalik proje durum raporlarini girdigi,
                        CTO'nun tum proje portfoyunu tek noktadan izledigi sistemin backend API'si.
                        """)
                .contact(new Contact().name("Gokhan Kara")));
    }
}
