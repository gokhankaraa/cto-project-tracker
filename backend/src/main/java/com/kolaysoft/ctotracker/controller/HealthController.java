package com.kolaysoft.ctotracker.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kolaysoft.ctotracker.dto.HealthResponse;
import com.kolaysoft.ctotracker.service.HealthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Health", description = "Uygulama ve veritabani saglik kontrolu")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @Operation(summary = "Saglik kontrolu",
            description = "Uygulamanin ayakta oldugunu ve veritabani baglantisinin calistigini dogrular.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public HealthResponse health() {
        return healthService.check();
    }
}
