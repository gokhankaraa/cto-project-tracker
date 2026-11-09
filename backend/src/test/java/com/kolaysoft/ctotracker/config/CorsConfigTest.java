package com.kolaysoft.ctotracker.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.kolaysoft.ctotracker.TestcontainersConfiguration;

/**
 * CORS ayarinin testi: varsayilan izinli adres Vite gelistirme sunucusudur (http://localhost:5173).
 *
 * <p>Ayarlari {@code HealthControllerTest} ile ayni oldugu icin Spring ayni test baglamini (ve ayni
 * PostgreSQL konteynerini) yeniden kullanir.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class CorsConfigTest {

    private static final String FRONTEND = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Izinli adresten on kontrol (preflight) istegi kabul edilir")
    void preflightFromFrontendIsAllowed() throws Exception {
        mockMvc.perform(options("/api/projects/1/reports")
                        .header(HttpHeaders.ORIGIN, FRONTEND)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    @DisplayName("Izinli adresten gelen istegin yanitinda izin basligi bulunur")
    void simpleRequestFromFrontendGetsAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/api/health").header(HttpHeaders.ORIGIN, FRONTEND))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND));
    }

    @Test
    @DisplayName("Listede olmayan adresten gelen istek 403 ile reddedilir")
    void unknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/reports")
                        .header(HttpHeaders.ORIGIN, "http://baska-site.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
