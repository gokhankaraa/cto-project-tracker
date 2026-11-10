package com.kolaysoft.ctotracker.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kolaysoft.ctotracker.entity.Role;
import com.kolaysoft.ctotracker.entity.User;
import com.kolaysoft.ctotracker.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    private static final String PASSWORD = "uzun-ve-gizli-parola-42";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(any())).thenAnswer(invocation -> "{hash}" + invocation.getArgument(0));
    }

    private AdminBootstrap bootstrap(String email, String password, String name) {
        return new AdminBootstrap(userRepository, passwordEncoder, email, password, name);
    }

    @Test
    @DisplayName("Kullanici yoksa admin bir kez olusturulur: e-posta kucuk harf ve kirpilmis, parola ozetlenmis")
    void createsTheFirstAdmin() {
        when(userRepository.count()).thenReturn(0L);

        bootstrap("  Yonetici@Ornek.COM ", PASSWORD, " İlk Yönetici ").run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User admin = captor.getValue();
        assertThat(admin.getEmail()).isEqualTo("yonetici@ornek.com");
        assertThat(admin.getFullName()).isEqualTo("İlk Yönetici");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getPasswordHash()).isEqualTo("{hash}" + PASSWORD);
    }

    @Test
    @DisplayName("Ad verilmezse varsayilan ad")
    void defaultName() {
        when(userRepository.count()).thenReturn(0L);

        bootstrap("yonetici@ornek.com", PASSWORD, " ").run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFullName()).isEqualTo("Sistem Yöneticisi");
    }

    @Test
    @DisplayName("Kullanici varsa hicbir sey yapmaz: degerler okunmaz, parola sifirlanmaz")
    void doesNothingWhenUsersExist() {
        when(userRepository.count()).thenReturn(3L);

        bootstrap("", "", "").run();
        bootstrap("baska@ornek.com", "baska-bir-parola-99", "Baska").run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Kullanici yokken degerler eksikse uygulama baslamaz")
    void refusesToStartWithoutValues() {
        when(userRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> bootstrap("", PASSWORD, "").run())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ADMIN_EMAIL ve ADMIN_PASSWORD");
        assertThatThrownBy(() -> bootstrap("yonetici@ornek.com", " ", "").run())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ADMIN_EMAIL ve ADMIN_PASSWORD");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Gecersiz e-posta ya da kisa parola reddedilir")
    void rejectsInvalidValues() {
        when(userRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> bootstrap("yonetici", PASSWORD, "").run())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ADMIN_EMAIL");
        assertThatThrownBy(() -> bootstrap("yonetici@ornek.com", "kisa-parola", "").run())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("en az 12 karakter");
        verify(userRepository, never()).save(any());
    }
}
