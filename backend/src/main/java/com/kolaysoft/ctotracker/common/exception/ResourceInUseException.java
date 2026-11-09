package com.kolaysoft.ctotracker.common.exception;

/**
 * Bir kaynak, kendisine bagli baska kayitlar oldugu icin silinemedigunde firlatilir;
 * 409 Conflict'e donusur. Ornek: haftalik raporlari olan bir projenin silinmeye calisilmasi.
 */
public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String message) {
        super(message);
    }
}
