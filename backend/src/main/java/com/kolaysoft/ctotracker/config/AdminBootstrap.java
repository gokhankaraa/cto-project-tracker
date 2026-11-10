package com.kolaysoft.ctotracker.config;

import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.repository.UserRepository;

/**
 * Uretimde ilk admin: veritabaninda hic kullanici yoksa {@code ADMIN_EMAIL} / {@code ADMIN_PASSWORD} /
 * {@code ADMIN_NAME} ile bir kez olusturulur. Kayit ekrani ya da acik bir endpoint yoktur: ilk admin'i yalnizca
 * sunucunun ortam degiskenlerine erisen kisi belirler.
 *
 * <p>Kullanici varsa hicbir sey yapmaz; degerler okunmaz, parola sifirlanmaz (yeniden baslatmak guvenlidir).
 * Kullanici yokken degerler eksik ya da gecersizse uygulama BASLAMAZ: kimsenin giris yapamadigi bir kurulum
 * sessizce ayakta kalmasin.
 *
 * <p>Demo profilinde calismaz (kullanicilari ornek veri olusturur). Testler {@code app.bootstrap.enabled=false}
 * ile kapatir; davranisi {@code AdminBootstrapTest} ve {@code ProductionSetupIntegrationTest} sinar.
 */
@Component
@Profile("!demo")
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    static final int MIN_PASSWORD_LENGTH = 12;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap.admin-email:}") String email,
                          @Value("${app.bootstrap.admin-password:}") String password,
                          @Value("${app.bootstrap.admin-name:Sistem Yöneticisi}") String name) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Veritabanında kullanıcı yok. İlk admin için ADMIN_EMAIL ve ADMIN_PASSWORD "
                    + "verin (yerel geliştirmede demo profilini seçin).");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalizedEmail).matches()) {
            throw new IllegalStateException("ADMIN_EMAIL geçerli bir e-posta adresi değil.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD en az " + MIN_PASSWORD_LENGTH + " karakter olmalı.");
        }

        User admin = new User();
        admin.setEmail(normalizedEmail);
        admin.setFullName(name == null || name.isBlank() ? "Sistem Yöneticisi" : name.trim());
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(admin);
        // Parola loglanmaz.
        log.info("Ilk admin olusturuldu: {}. ADMIN_PASSWORD artik ortamdan kaldirilabilir.", normalizedEmail);
    }
}
