package com.kolaysoft.ctotracker.common.exception;

/**
 * Tek basina gecerli ama birlikte ya da bicim olarak gecersiz istek parametrelerinde firlatilir;
 * 400 VALIDATION_ERROR'a donusur ve hatali parametre {@code fieldErrors} icinde doner
 * (filtre sozlesmesi, bolum 5). Ornekler: weekFrom &gt; weekTo, year olmadan
 * verilen hafta parametresi, tanimsiz siralama alani.
 */
public class InvalidParameterException extends RuntimeException {

    private final String parameter;

    public InvalidParameterException(String parameter, String message) {
        super(message);
        this.parameter = parameter;
    }

    /** Hatanin raporlanacagi parametre adi (or. "weekTo", "sort"). */
    public String getParameter() {
        return parameter;
    }
}
