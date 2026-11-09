package com.kolaysoft.ctotracker.service;

import java.sql.DatabaseMetaData;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.kolaysoft.ctotracker.dto.HealthResponse;

/**
 * Uygulamanin ayakta olup olmadigini ve veritabani baglantisinin gercekten
 * calistigini kontrol eder. Baglanti kontrolu icin veritabanina basit bir sorgu gonderilir;
 * yalnizca bean'lerin yuklenmis olmasi baglantinin calistigini kanitlamaz.
 */
@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String STATUS_DEGRADED = "DEGRADED";

    private final JdbcTemplate jdbcTemplate;
    private final String applicationName;

    public HealthService(JdbcTemplate jdbcTemplate,
                         @Value("${spring.application.name}") String applicationName) {
        this.jdbcTemplate = jdbcTemplate;
        this.applicationName = applicationName;
    }

    public HealthResponse check() {
        String databaseStatus = STATUS_DOWN;
        String database = "bilinmiyor";

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            database = readDatabaseInfo();
            databaseStatus = STATUS_UP;
        } catch (DataAccessException ex) {
            log.error("Veritabani baglantisi dogrulanamadi", ex);
        }

        String status = STATUS_UP.equals(databaseStatus) ? STATUS_UP : STATUS_DEGRADED;
        return new HealthResponse(status, applicationName, databaseStatus, database, OffsetDateTime.now());
    }

    private String readDatabaseInfo() {
        return jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<String>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            return "%s %s".formatted(metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
        });
    }
}
