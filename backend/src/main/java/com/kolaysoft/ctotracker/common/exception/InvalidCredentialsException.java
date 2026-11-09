package com.kolaysoft.ctotracker.common.exception;

/**
 * Giriste e-posta ya da parola hatali; 401 INVALID_CREDENTIALS'a donusur. Kayitli olmayan e-posta ile
 * yanlis parola ayni mesaji alir, boylece hangi e-postalarin kayitli oldugu disaridan ogrenilemez.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-posta ya da parola hatalı.");
    }
}
