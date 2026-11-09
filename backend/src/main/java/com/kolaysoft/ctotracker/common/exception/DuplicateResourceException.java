package com.kolaysoft.ctotracker.common.exception;

/**
 * Benzersizlik kurali ihlal edildiginde firlatilir; 409 Conflict'e donusur.
 * Ornek: ayni proje ve hafta icin ikinci rapor, kayitli bir e-posta ile kullanici olusturma.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
