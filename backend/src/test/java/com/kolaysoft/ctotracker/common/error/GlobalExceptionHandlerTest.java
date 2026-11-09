package com.kolaysoft.ctotracker.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.test.context.support.WithMockUser;

import com.kolaysoft.ctotracker.TestcontainersConfiguration;
import com.kolaysoft.ctotracker.common.exception.BusinessRuleException;
import com.kolaysoft.ctotracker.common.exception.DuplicateResourceException;
import com.kolaysoft.ctotracker.common.exception.ResourceNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Hata formatinin sozlesmesini dogrular: her hata ayni govdeyle
 * (timestamp, status, code, message, path) ve dogru HTTP kodu ile doner.
 *
 * <p>Gercek endpoint'ler heniz gelistirilmedigi icin yalnizca teste ozel bir
 * controller kullaniliyor; uygulama kodunda demo endpoint birakilmiyor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({GlobalExceptionHandlerTest.TestEndpoints.class, TestcontainersConfiguration.class})
// Butun istekler giris ister; bu testler yetki degil is kurallarini sinar, Admin olarak calisir.
@WithMockUser(roles = "ADMIN")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Zorunlu alan bos gonderilirse 400 ve alan bazli hata listesi doner")
    void validationErrorReturnsFieldErrors() throws Exception {
        String body = """
                { "title": "", "weekNumber": 99 }
                """;

        mockMvc.perform(post("/test-errors/validate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/test-errors/validate"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    @DisplayName("Bozuk JSON gonderilirse 400 INVALID_REQUEST doner")
    void unreadableBodyReturnsInvalidRequest() throws Exception {
        mockMvc.perform(post("/test-errors/validate").contentType(MediaType.APPLICATION_JSON).content("{ bozuk"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Bulunamayan kayit 404 RESOURCE_NOT_FOUND doner")
    void notFoundReturns404() throws Exception {
        mockMvc.perform(get("/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Proje bulunamadi: 42"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    @DisplayName("Benzersizlik ihlali 409 DUPLICATE_RESOURCE doner")
    void duplicateReturns409() throws Exception {
        mockMvc.perform(get("/test-errors/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("Is kurali ihlali 400 BUSINESS_RULE_VIOLATION doner")
    void businessRuleReturns400() throws Exception {
        mockMvc.perform(get("/test-errors/business-rule"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Ilerleme kademesinde sira atlanamaz."));
    }

    @Test
    @DisplayName("Beklenmeyen hatada 500 doner ve teknik detay sizdirilmaz")
    void unexpectedErrorHidesTechnicalDetail() throws Exception {
        mockMvc.perform(get("/test-errors/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Beklenmeyen bir sunucu hatasi olustu."));
    }

    @Test
    @DisplayName("Controller icindeki yetki reddi 500 degil 403 FORBIDDEN olur")
    void accessDeniedInControllerReturns403() throws Exception {
        mockMvc.perform(get("/test-errors/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Bu islem icin yetkiniz yok."));
    }

    @Test
    @DisplayName("Tanimsiz endpoint cagrildiginda da ayni hata formati doner")
    void unknownPathUsesSameErrorFormat() throws Exception {
        mockMvc.perform(get("/api/boyle-bir-endpoint-yok"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Gecersiz enum parametresi 400 INVALID_REQUEST doner (500 degil)")
    void typeMismatchReturns400() throws Exception {
        mockMvc.perform(get("/test-errors/enum-param").param("level", "COK_YUKSEK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @TestConfiguration
    @RestController
    @RequestMapping("/test-errors")
    static class TestEndpoints {

        enum SampleLevel { DUSUK, ORTA, YUKSEK }

        record SampleRequest(@NotBlank(message = "Baslik zorunludur.") String title,
                             @Min(value = 1, message = "Hafta numarasi en az 1 olmalidir.")
                             @Max(value = 53, message = "Hafta numarasi en fazla 53 olmalidir.") int weekNumber) {
        }

        @PostMapping("/validate")
        String validate(@Valid @RequestBody SampleRequest request) {
            return request.title();
        }

        @org.springframework.web.bind.annotation.GetMapping("/enum-param")
        String enumParam(@org.springframework.web.bind.annotation.RequestParam SampleLevel level) {
            return level.name();
        }

        @org.springframework.web.bind.annotation.GetMapping("/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Proje", 42);
        }

        @org.springframework.web.bind.annotation.GetMapping("/duplicate")
        String duplicate() {
            throw new DuplicateResourceException("Bu proje ve hafta icin rapor zaten mevcut.");
        }

        @org.springframework.web.bind.annotation.GetMapping("/business-rule")
        String businessRule() {
            throw new BusinessRuleException("Ilerleme kademesinde sira atlanamaz.");
        }

        @org.springframework.web.bind.annotation.GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("veritabani baglantisi koptu");
        }

        @org.springframework.web.bind.annotation.GetMapping("/access-denied")
        String accessDenied() {
            throw new org.springframework.security.access.AccessDeniedException("rol yetmiyor");
        }
    }
}
