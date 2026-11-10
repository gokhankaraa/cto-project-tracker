package com.kolaysoft.ctotracker.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Uygulamanin "bugun"u. Takip kayitlarindaki tarih kurallari (gecmis takip tarihi, gecikme) bugune
 * baglidir; saat buradan alinir ki testler tarihi sabitleyebilsin. Gunun degistigi an kullanicilarin
 * saatine gore olsun diye Turkiye saati kullanilir (sunucu UTC'de calissa da).
 */
@Configuration
public class ClockConfig {

    public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
