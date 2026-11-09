package com.kolaysoft.ctotracker.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.kolaysoft.ctotracker.entity.User;

/**
 * Giris yapan kullanici icin imzali JWT uretir. Token'da kullanicinin id'si ({@code sub}), e-postasi,
 * adi ve rolu bulunur; parola ya da ozeti asla bulunmaz. Token {@code app.auth.token-ttl} sonunda gecersiz
 * olur (varsayilan 8 saat); yenileme yoktur, sure dolunca yeniden giris yapilir.
 */
@Service
public class TokenService {

    static final String ISSUER = "cto-project-tracker";

    private final JwtEncoder jwtEncoder;
    private final Duration tokenTtl;
    private final Clock clock;

    /** Uygulamanin kullandigi yapici; saat parametreli olan testler icindir (suresi dolmus token). */
    @Autowired
    public TokenService(JwtEncoder jwtEncoder, @Value("${app.auth.token-ttl}") Duration tokenTtl) {
        this(jwtEncoder, tokenTtl, Clock.systemUTC());
    }

    TokenService(JwtEncoder jwtEncoder, Duration tokenTtl, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.tokenTtl = tokenTtl;
        this.clock = clock;
    }

    public IssuedToken issue(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(tokenTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim(SecurityConfig.ROLE_CLAIM, user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    /** Uretilen token ve gecerlilik sonu. */
    public record IssuedToken(String token, Instant expiresAt) {
    }
}
