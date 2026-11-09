package com.kolaysoft.ctotracker.common.exception;

/** Istenen kayit bulunamadiginda firlatilir; 404 Not Found'a donusur. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /** Ornek: {@code new ResourceNotFoundException("Proje", 42)} -> "Proje bulunamadi: 42". */
    public ResourceNotFoundException(String resourceName, Object id) {
        super("%s bulunamadi: %s".formatted(resourceName, id));
    }
}
