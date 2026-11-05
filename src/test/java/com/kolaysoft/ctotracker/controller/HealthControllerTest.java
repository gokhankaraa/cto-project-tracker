package com.kolaysoft.ctotracker.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.kolaysoft.ctotracker.TestcontainersConfiguration;

/** Health endpoint'i, veritabani baglantisinin gercekten calistigini da dogruluyor. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/health uygulama ve veritabani durumunu UP olarak doner")
    void healthReturnsUpWithDatabaseConnection() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.databaseStatus").value("UP"))
                .andExpect(jsonPath("$.application").value("cto-project-tracker-backend"))
                .andExpect(jsonPath("$.database").value(org.hamcrest.Matchers.containsString("PostgreSQL")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
