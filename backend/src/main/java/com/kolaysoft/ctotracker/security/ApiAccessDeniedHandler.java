package com.kolaysoft.ctotracker.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.kolaysoft.ctotracker.common.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 403: kullanici giris yapmis ama rolu bu isleme yetmiyor. */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    static final String MESSAGE = "Bu islem icin yetkiniz yok.";

    private final ApiErrorWriter writer;

    public ApiAccessDeniedHandler(ApiErrorWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        writer.write(request, response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, MESSAGE);
    }
}
