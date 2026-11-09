package com.kolaysoft.ctotracker.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.kolaysoft.ctotracker.common.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 401: istek giris gerektiriyor ama token yok, gecersiz ya da suresi dolmus. Yanit standart
 * {@code WWW-Authenticate: Bearer} basligini ve {@code ApiError} govdesini tasir.
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiErrorWriter writer;

    public ApiAuthenticationEntryPoint(ApiErrorWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        // Token'in neden reddedildigi (imza, sure) istemciye ayrintili soylenmez.
        String message = exception instanceof InvalidBearerTokenException
                ? "Oturum gecersiz ya da suresi dolmus; yeniden giris yapin."
                : "Bu islem icin giris yapmalisiniz.";
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        writer.write(request, response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, message);
    }
}
