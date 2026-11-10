package com.kolaysoft.ctotracker.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Token sirrinin kontrolu: bos ya da repository'deki gelistirme sirri uretimde kabul edilmez. */
class JwtSecretTest {

    private static final String DEV_SECRET = "dev-only-jwt-secret-do-not-use-in-production-0123456789";

    @Test
    @DisplayName("Sir verilmemisse uygulama baslamaz; mesaj ne yapilacagini soyler")
    void missingSecret() {
        assertThatThrownBy(() -> SecurityConfig.checkedSecret("", false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_SECRET tanımlı değil");
        assertThatThrownBy(() -> SecurityConfig.checkedSecret(null, true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_SECRET tanımlı değil");
    }

    @Test
    @DisplayName("Gelistirme sirri yalnizca demo profilinde kabul edilir")
    void devSecretOnlyInDemo() {
        assertThat(SecurityConfig.checkedSecret(DEV_SECRET, true)).isEqualTo(DEV_SECRET);
        assertThatThrownBy(() -> SecurityConfig.checkedSecret(DEV_SECRET, false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("geliştirme değeri");
    }

    @Test
    @DisplayName("Kendi rastgele sirri her profilde kabul edilir")
    void ownSecret() {
        String secret = "Zx8q1Rk2Lw0pT7vN3mB5cY6uH4gF9dS2aE1jK8oP7iU=";
        assertThat(SecurityConfig.checkedSecret(secret, false)).isEqualTo(secret);
        assertThat(SecurityConfig.hmacKey(secret).getAlgorithm()).isEqualTo("HmacSHA256");
    }
}
