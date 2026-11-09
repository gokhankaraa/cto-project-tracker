package com.kolaysoft.ctotracker.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Kimlik dogrulama: istemci {@code POST /api/auth/login} ile aldigi JWT'yi her istekte
 * {@code Authorization: Bearer ...} basliginda gonderir. Sunucu oturum tutmaz (durumsuz); token
 * HMAC-SHA256 ile imzalanir ve ayni anahtarla dogrulanir. Rol token'in {@code role} alanindadir ve
 * {@code ROLE_PROJECT_MANAGER} / {@code ROLE_CTO} / {@code ROLE_ADMIN} yetkisine cevrilir.
 *
 * <p>Acik yollar: saglik kontrolu, giris ve API dokumantasyonu. Geri kalan her {@code /api/**} istegi
 * giris ister; yoksa 401, rol yetmiyorsa 403 doner (ikisi de {@code ApiError} bicimindedir). Yazma
 * isteklerinin rol kurallari {@link Permissions}'tadir.
 * Token tarayicida cerezle degil baslikla tasindigi icin CSRF korumasi gerekmez.
 */
@Configuration
public class SecurityConfig {

    /** Rol bilgisinin token'daki alan adi. */
    public static final String ROLE_CLAIM = "role";

    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http, ApiAuthenticationEntryPoint entryPoint,
                                           ApiAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS kurallari CorsConfig'ten gelir; on kontrol (OPTIONS) istekleri giris istemez.
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Spring'in hata yonlendirmesi; asil istegin 401/403'u onceden yazilmis olur.
                        .requestMatchers("/error").permitAll())
                // Rol kurallari acik yollardan sonra: once eslesen kural gecerlidir (giris POST'u acik kalir).
                .authorizeHttpRequests(Permissions::writeRules)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    /** Token'daki {@code role} alani -> {@code ROLE_...} yetkisi ({@code hasRole("CTO")} ile kontrol edilir). */
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public SecretKey jwtSigningKey(@Value("${app.auth.jwt-secret}") String secret) {
        return hmacKey(secret);
    }

    /**
     * HS256 anahtari. Kisa bir sir tahmin edilebilir oldugu icin en az 32 bayt istenir; daha kisasiyla
     * uygulama baslamaz (yanlis yapilandirma sessizce zayif bir anahtarla calismasin).
     */
    static SecretKey hmacKey(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.auth.jwt-secret (JWT_SECRET) en az 32 bayt olmali; verilen: " + bytes.length + " bayt.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Parola ozeti: BCrypt (yavas ve tuzlu; ayni parola her seferinde farkli ozet uretir). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
