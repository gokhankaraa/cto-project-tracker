package com.kolaysoft.ctotracker.security;

import java.time.Clock;
import java.time.Duration;

import javax.crypto.SecretKey;

import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.kolaysoft.ctotracker.entity.User;

/**
 * Testlerin paket ici yardimcilara erismesi icin: gecmis bir anda uretilmis (suresi dolmus) token ve
 * anahtar uzunlugu kontrolu. Uretim kodundaki erisim duzeyleri testler icin genisletilmez.
 */
public final class TokenServiceTestAccess {

    private TokenServiceTestAccess() {
    }

    public static String issueAt(JwtEncoder encoder, User user, Clock clock, Duration ttl) {
        return new TokenService(encoder, ttl, clock).issue(user).token();
    }

    public static SecretKey hmacKey(String secret) {
        return SecurityConfig.hmacKey(secret);
    }
}
