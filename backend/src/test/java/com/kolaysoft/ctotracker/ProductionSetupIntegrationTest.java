package com.kolaysoft.ctotracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.kolaysoft.ctotracker.config.AdminBootstrap;
import com.kolaysoft.ctotracker.config.DataSeeder;
import com.kolaysoft.ctotracker.repository.UserRepository;

/**
 * Uretim ayarlari uctan uca: demo profili olmadan acilan uygulama. Ilk admin ortam degiskenlerinden (burada
 * ozellik olarak) olusturulur, demo kullanicilari yoktur, API dokumantasyonu kapalidir, log INFO'dadir.
 * Bos ya da gelistirme sirri ile acilmama {@code JwtSecretTest}'te sinanir.
 */
@SpringBootTest(properties = {
        "app.bootstrap.enabled=true",
        "app.bootstrap.admin-email= Yonetici@Ornek.com ",
        "app.bootstrap.admin-password=" + ProductionSetupIntegrationTest.PASSWORD,
        "app.bootstrap.admin-name=İlk Yönetici"})
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class ProductionSetupIntegrationTest {

    static final String PASSWORD = "uzun-ve-gizli-parola-42";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "password": "%s" }
                        """.formatted(email, password)));
    }

    @Test
    @DisplayName("Ilk admin acilista olusturulur ve giris yapar; baska kullanici yok")
    void firstAdminCanLogIn() throws Exception {
        login("yonetici@ornek.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.fullName").value("İlk Yönetici"));
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Demo kullanicilari ve demo verisi yok")
    void noDemoData() throws Exception {
        login("admin@example.com", DataSeeder.DEMO_PASSWORD).andExpect(status().isUnauthorized());
        login("ayse@example.com", DataSeeder.DEMO_PASSWORD).andExpect(status().isUnauthorized());
        String token = com.jayway.jsonpath.JsonPath.read(login("yonetici@ornek.com", PASSWORD)
                .andReturn().getResponse().getContentAsString(), "$.token");
        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Yeniden baslatmak admin'i tekrar olusturmaz, parolasini degistirmez")
    void restartKeepsTheAdmin() {
        new AdminBootstrap(userRepository, passwordEncoder, "yeni@ornek.com", "baska-bir-parola-99", "Baska").run();

        assertThat(userRepository.count()).isEqualTo(1);
        String hash = userRepository.findByEmail("yonetici@ornek.com").orElseThrow().getPasswordHash();
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
    }

    @Test
    @DisplayName("API dokumantasyonu kapali; uygulama loglari INFO'da")
    void apiDocsOffAndInfoLogs() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
        assertThat(LoggerFactory.getLogger("com.kolaysoft.ctotracker").isDebugEnabled()).isFalse();
        assertThat(LoggerFactory.getLogger("com.kolaysoft.ctotracker").isInfoEnabled()).isTrue();
    }
}
