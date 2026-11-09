package com.kolaysoft.ctotracker.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;

import com.kolaysoft.ctotracker.TestcontainersConfiguration;
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.repository.UserRepository;

/**
 * GET /api/users: ada gore sirali liste, rol filtresi, e-postanin yanitta yer almamasi.
 * Ayarlari HealthControllerTest ile ayni oldugu icin ayni test baglamini kullanir; veriler
 * {@code @Transactional} ile test sonunda geri alinir.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
// Butun istekler giris ister; bu testler yetki degil is kurallarini sinar, Admin olarak calisir.
@WithMockUser(roles = "ADMIN")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        user("zeynep@example.com", "Zeynep Kaya", Role.PROJECT_MANAGER);
        user("ahmet@example.com", "Ahmet Demir", Role.PROJECT_MANAGER);
        user("cto@example.com", "Can Ozturk", Role.CTO);
    }

    private void user(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        userRepository.save(u);
    }

    @Test
    @DisplayName("Kullanicilar ada gore sirali doner; e-posta yanitta yer almaz")
    void listsUsersSortedByName() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].fullName").value(contains("Ahmet Demir", "Can Ozturk", "Zeynep Kaya")))
                .andExpect(jsonPath("$[0].role").value("PROJECT_MANAGER"))
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    @DisplayName("Rol filtresi yalnizca o roldeki kullanicilari doner")
    void filtersByRole() throws Exception {
        mockMvc.perform(get("/api/users").param("role", "PROJECT_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].fullName").value(contains("Ahmet Demir", "Zeynep Kaya")));
    }

    @Test
    @DisplayName("Tanimsiz rol 400 INVALID_REQUEST")
    void unknownRoleReturns400() throws Exception {
        mockMvc.perform(get("/api/users").param("role", "MUDUR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
