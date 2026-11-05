package com.kolaysoft.ctotracker;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Spring testleri icin gercek PostgreSQL.
 *
 * <p>Testler uygulamayla ayni veritabani motorunda (docker-compose.yml ile ayni imaj) calisir;
 * sema da uygulamadaki gibi Flyway migration'lariyla kurulur. {@code @ServiceConnection}
 * baglanti bilgilerini konteynerden alir, bu yuzden application.yml'deki adres testlerde kullanilmaz.
 *
 * <p>Konteyner bir Spring bean'idir: her test uygulama baglami (application context) bos bir
 * veritabaniyla baslar ve baglam kapaninca konteyner de durdurulur. Testleri calistirmak icin
 * Docker'in acik olmasi gerekir.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
