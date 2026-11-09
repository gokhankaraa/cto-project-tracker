package com.kolaysoft.ctotracker.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kolaysoft.ctotracker.dto.UserResponse;
import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Kullanici listesi (salt okunur). Frontend'de projeye sorumlu atarken ve dashboard'u sorumluya gore
 * suzerken kullanilir. Kullanici ekleme/duzenleme kimlik dogrulama ile birlikte (T14) ele alinacak.
 */
@Tag(name = "User", description = "Kullanici listesi")
@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Kullanicilari listele", description = "Ada gore sirali. role verilirse yalnizca o roldekiler.")
    @GetMapping
    public List<UserResponse> list(
            @Parameter(description = "Yalnizca bu roldeki kullanicilar", example = "PROJECT_MANAGER")
            @RequestParam(required = false) Role role) {
        return userService.findAll(role);
    }
}
