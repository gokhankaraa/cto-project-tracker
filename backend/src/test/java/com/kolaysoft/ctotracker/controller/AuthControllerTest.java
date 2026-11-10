package com.kolaysoft.ctotracker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.kolaysoft.ctotracker.TestcontainersConfiguration;
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.repository.UserRepository;
import com.kolaysoft.ctotracker.security.TokenServiceTestAccess;

/**
 * Giris ve token kontrolu uctan uca (gercek filtre zinciri, gercek token). Kullanicilar test icinde
 * olusturulur ve {@code @Transactional} ile geri alinir.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    private static final String PASSWORD = "Gizli-Parola-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    private User cto;

    @BeforeEach
    void setUp() {
        cto = user("zeynep@example.com", "Zeynep Demir", Role.CTO, PASSWORD);
        user("parolasiz@example.com", "Parolasi Yok", Role.PROJECT_MANAGER, null);
    }

    private User user(String email, String fullName, Role role, String password) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(password == null ? null : passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    private String loginBody(String email, String password) {
        return """
                { "email": "%s", "password": "%s" }
                """.formatted(email, password);
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    @DisplayName("Dogru e-posta ve parola token doner; token ile korumali istekler ve /me calisir")
    void loginReturnsTokenThatAuthenticatesRequests() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody("Zeynep@Example.com ", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.user.id").value(cto.getId()))
                .andExpect(jsonPath("$.user.fullName").value("Zeynep Demir"))
                .andExpect(jsonPath("$.user.role").value("CTO"))
                // Parola ve ozeti hicbir yanitta yer almaz.
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.email").doesNotExist());

        String token = login("zeynep@example.com", PASSWORD);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cto.getId()))
                .andExpect(jsonPath("$.role").value("CTO"));
        mockMvc.perform(get("/api/projects").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Yanlis parola ve kayitli olmayan e-posta ayni 401 INVALID_CREDENTIALS'i alir")
    void wrongCredentialsAreIndistinguishable() throws Exception {
        for (String[] credentials : new String[][] {
                {"zeynep@example.com", "yanlis-parola"},
                {"kimse@example.com", PASSWORD},
                {"parolasiz@example.com", PASSWORD}}) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(loginBody(credentials[0], credentials[1])))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("E-posta ya da parola hatalı."));
        }
    }

    @Test
    @DisplayName("Bos alanli giris istegi 400 VALIDATION_ERROR (iki alan hatasi)")
    void invalidLoginRequestReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody(" ", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    @DisplayName("Token olmadan korumali istek 401 UNAUTHORIZED, ApiError bicimi ve WWW-Authenticate basligi")
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Bu işlem için giriş yapmalısınız."))
                .andExpect(jsonPath("$.path").value("/api/projects"));
    }

    @Test
    @DisplayName("Bozuk, baska anahtarla imzalanmis ya da suresi dolmus token 401")
    void invalidOrExpiredTokenReturns401() throws Exception {
        String valid = login("zeynep@example.com", PASSWORD);
        String tampered = valid.substring(0, valid.length() - 4) + "abcd";
        String expired = TokenServiceTestAccess.issueAt(jwtEncoder, cto,
                Clock.fixed(Instant.now().minus(Duration.ofHours(9)), ZoneOffset.UTC), Duration.ofHours(8));

        for (String token : new String[] {"bozuk-token", tampered, expired}) {
            mockMvc.perform(get("/api/projects").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Oturum geçersiz ya da süresi dolmuş; yeniden giriş yapın."));
        }
    }

    @Test
    @DisplayName("Kullanici silinmisse gecerli token ile /me 401 doner")
    void meReturns401WhenUserNoLongerExists() throws Exception {
        String token = login("zeynep@example.com", PASSWORD);
        userRepository.delete(cto);
        userRepository.flush();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Saglik kontrolu token istemez (API dokumantasyonu: MvpIntegrationTest, demo profili)")
    void publicEndpointsNeedNoToken() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Parola ozeti BCrypt'tir; parola duz metin saklanmaz")
    void passwordIsStoredAsBcryptHash() {
        String hash = userRepository.findById(cto.getId()).orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2").doesNotContain(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
    }

    @Test
    @DisplayName("32 bayttan kisa JWT sirri ile uygulama baslamaz")
    void shortJwtSecretIsRejected() {
        assertThatThrownBy(() -> TokenServiceTestAccess.hmacKey("kisa-sir"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("en az 32 bayt");
    }
}
