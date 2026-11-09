package com.kolaysoft.ctotracker.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.kolaysoft.ctotracker.common.error.ApiError;
import com.kolaysoft.ctotracker.common.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Guvenlik filtrelerinde olusan 401/403'u, controller hatalariyla ayni {@code ApiError} govdesiyle yazar.
 * Bu hatalar istek controller'a ulasmadan olustugu icin GlobalExceptionHandler'a dusmez.
 */
@Component
public class ApiErrorWriter {

    private final JsonMapper jsonMapper;

    public ApiErrorWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                      ErrorCode code, String message) throws IOException {
        ApiError body = ApiError.of(status.value(), code, message, request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
