package com.kolaysoft.ctotracker.controller;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kolaysoft.ctotracker.dto.LoginRequest;
import com.kolaysoft.ctotracker.dto.LoginResponse;
import com.kolaysoft.ctotracker.dto.UserResponse;
import com.kolaysoft.ctotracker.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Giris ve oturumdaki kullanici. */
@Tag(name = "Kimlik dogrulama", description = "Giris ve oturumdaki kullanici")
@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Giris",
            description = "E-posta ve parola dogruysa imzali bir JWT doner. Hataliysa 401 INVALID_CREDENTIALS; "
                    + "e-postanin mi parolanin mi hatali oldugu soylenmez.")
    @SecurityRequirements // giris icin token gerekmez
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Oturumdaki kullanici",
            description = "Token'in sahibi olan kullanici. Token yoksa ya da gecersizse 401.")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        // Token gecerli ama kullanici o arada silinmisse oturum da gecersiz sayilir.
        return authService.currentUser(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new InsufficientAuthenticationException("Kullanıcı artık yok."));
    }
}
