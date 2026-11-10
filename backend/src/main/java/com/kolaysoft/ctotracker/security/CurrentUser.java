package com.kolaysoft.ctotracker.security;

import java.util.Arrays;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.kolaysoft.ctotracker.entity.Role;

/**
 * Oturumdaki kullaniciyi servislerin kullanabilecegi bicime ({@link Viewer}) cevirir. Rol, guvenlik
 * filtresinin token'dan urettigi {@code ROLE_...} yetkisinden; id, token'in {@code sub} alanindan gelir.
 */
@Component
public class CurrentUser {

    public Viewer viewer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Oturum yok.");
        }
        Role role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .filter(name -> Arrays.stream(Role.values()).anyMatch(r -> r.name().equals(name)))
                .map(Role::valueOf)
                .findFirst()
                .orElseThrow(() -> new InsufficientAuthenticationException("Oturumda rol yok."));
        Long userId = authentication.getPrincipal() instanceof Jwt jwt ? Long.valueOf(jwt.getSubject()) : null;
        return new Viewer(userId, role);
    }

    /**
     * Degisikligi yapan kisi (degisiklik gecmisi icin): id ve ad token'dan okunur, veritabanina gidilmez. Ad,
     * token alindigi andaki addir. Token'siz oturumda (testlerdeki sahte kullanici) id bos, ad oturum adidir.
     */
    public Actor actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Oturum yok.");
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String name = jwt.getClaimAsString("name");
            return new Actor(Long.valueOf(jwt.getSubject()), name != null ? name : jwt.getSubject());
        }
        return new Actor(null, authentication.getName());
    }
}
