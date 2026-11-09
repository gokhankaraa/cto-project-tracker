package com.kolaysoft.ctotracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sistem kullanicisi (on analiz, bolum 4).
 *
 * <p>MVP'de kimlik dogrulama (login) uygulanmadigi icin sifre alani tutulmaz;
 * bu alan auth ozelligiyle birlikte sonraki asamada eklenecektir. Kullanici
 * su an yalnizca projeye sorumlu (owner) atamak ve rol bilgisini tasimak icin vardir.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
