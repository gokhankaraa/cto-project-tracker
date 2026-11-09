package com.kolaysoft.ctotracker.common.exception;

/**
 * Alan bazli dogrulamayla yakalanamayan is kurali ihlallerinde firlatilir; 400 Bad Request'e donusur.
 * Ornek: ilerleme kademesinde sira atlanmasi (ANALIZ -> TEST).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
