package com.kolaysoft.ctotracker.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.diagnostics.FailureAnalysis;

import com.kolaysoft.ctotracker.common.exception.StartupConfigurationException;

class StartupConfigurationFailureAnalyzerTest {

    @Test
    @DisplayName("Bean olusturma hatasinin icindeki yapilandirma hatasi bulunur; ozet sorun ve yapilacak is")
    void analyzesNestedConfigurationError() {
        BeanCreationException failure = new BeanCreationException("authService", "olusturulamadi",
                new BeanCreationException("jwtSigningKey", "olusturulamadi",
                        new StartupConfigurationException("JWT_SECRET tanımlı değil.", "Rastgele bir değer verin.")));

        FailureAnalysis analysis = new StartupConfigurationFailureAnalyzer().analyze(failure);

        assertThat(analysis.getDescription()).isEqualTo("JWT_SECRET tanımlı değil.");
        assertThat(analysis.getAction()).isEqualTo("Rastgele bir değer verin.");
    }

    @Test
    @DisplayName("Baska bir hata analiz edilmez (Spring Boot'un kendi analizcilerine kalir)")
    void ignoresOtherErrors() {
        assertThat(new StartupConfigurationFailureAnalyzer().analyze(new IllegalStateException("baska"))).isNull();
    }

    @Test
    @DisplayName("Analizci spring.factories'te kayitli")
    void isRegistered() throws IOException {
        boolean registered = false;
        for (URL url : Collections.list(getClass().getClassLoader().getResources("META-INF/spring.factories"))) {
            try (var in = url.openStream()) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                registered |= content.contains("org.springframework.boot.diagnostics.FailureAnalyzer")
                        && content.contains(StartupConfigurationFailureAnalyzer.class.getName());
            }
        }
        assertThat(registered).isTrue();
    }
}
