package com.kolaysoft.ctotracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Bir olayda degisen tek alan. Degerler ham metindir (enum adi, ISO tarih, sayi); Turkce etiketi arayuz basar.
 * Eklemede eski deger, silmede hic alan yoktur.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuditChange {

    @Column(name = "field", nullable = false)
    private String field;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;
}
