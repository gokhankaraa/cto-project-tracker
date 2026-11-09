package com.kolaysoft.ctotracker.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.common.exception.InvalidCredentialsException;
import com.kolaysoft.ctotracker.dto.LoginRequest;
import com.kolaysoft.ctotracker.dto.LoginResponse;
import com.kolaysoft.ctotracker.dto.UserResponse;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.repository.UserRepository;
import com.kolaysoft.ctotracker.security.TokenService;

/** Giris: e-posta + parola dogrulanir, dogruysa imzali bir token uretilir. */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /**
     * Kullanici bulunamadiginda da bir BCrypt karsilastirmasi yapilir; boylece yanit suresi kayitli
     * olmayan e-postayi ele vermez (zamanlama saldirisi).
     */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("parola-yok-karsilastirma-icin");
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> user = userRepository.findByEmail(email);
        String hash = user.map(User::getPasswordHash).orElse(null);

        boolean matches = passwordEncoder.matches(request.password(), hash != null ? hash : dummyHash);
        if (user.isEmpty() || hash == null || !matches) {
            throw new InvalidCredentialsException();
        }

        TokenService.IssuedToken token = tokenService.issue(user.get());
        return new LoginResponse(token.token(), token.expiresAt(), UserResponse.from(user.get()));
    }

    /** Token'daki kullanici; token gecerli ama kullanici silinmisse bos. */
    public Optional<UserResponse> currentUser(Long userId) {
        return userRepository.findById(userId).map(UserResponse::from);
    }
}
