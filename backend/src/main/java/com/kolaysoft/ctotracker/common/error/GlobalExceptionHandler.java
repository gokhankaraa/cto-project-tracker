package com.kolaysoft.ctotracker.common.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.kolaysoft.ctotracker.common.exception.BusinessRuleException;
import com.kolaysoft.ctotracker.common.exception.DuplicateResourceException;
import com.kolaysoft.ctotracker.common.exception.InvalidParameterException;
import com.kolaysoft.ctotracker.common.exception.ResourceInUseException;
import com.kolaysoft.ctotracker.common.exception.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tum controller'lar icin merkezi hata yonetimi.
 * Hangi hata olursa olsun istemciye tek bir govde formati ({@link ApiError}) doner.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** @Valid ile isaretli istek govdesindeki alan hatalari. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        List<ApiFieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Gonderilen alanlar dogrulama kurallarina uymuyor.", request, fieldErrors);
    }

    /** Path/query parametreleri uzerindeki dogrulama hatalari. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(HandlerMethodValidationException ex,
                                                              HttpServletRequest request) {
        List<ApiFieldError> fieldErrors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ApiFieldError(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();

        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Gonderilen parametreler dogrulama kurallarina uymuyor.", request, fieldErrors);
    }

    /** Birlikte gecersiz parametreler: ters aralik (weekFrom > weekTo), tanimsiz siralama alani. */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ApiError> handleInvalidParameter(InvalidParameterException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Gonderilen parametreler dogrulama kurallarina uymuyor.", request,
                List.of(new ApiFieldError(ex.getParameter(), ex.getMessage())));
    }

    /** Bozuk JSON, tanimsiz enum degeri veya hatali tarih formati. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest(HttpMessageNotReadableException ex,
                                                            HttpServletRequest request) {
        log.debug("Istek govdesi okunamadi: {}", ex.getMessage());

        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST,
                "Istek govdesi okunamadi. Alan tiplerini ve enum degerlerini kontrol edin.", request);
    }

    /** Path/query parametresi beklenen tipe cevrilemedi (or. tanimsiz enum, sayi yerine metin). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        log.debug("Parametre tipi uyumsuz: {}", ex.getMessage());

        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST,
                "'%s' parametresinin degeri gecersiz.".formatted(ex.getName()), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage(), request);
    }

    /** Tanimsiz bir URL cagrildiginda da ayni hata formati donsun. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleUnknownPath(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                "Boyle bir endpoint bulunmuyor.", request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_RESOURCE, ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BUSINESS_RULE_VIOLATION, ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<ApiError> handleResourceInUse(ResourceInUseException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorCode.RESOURCE_IN_USE, ex.getMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED,
                "%s metodu bu endpoint icin desteklenmiyor.".formatted(ex.getMethod()), request);
    }

    /** Son savunma hatti: teknik detay sunucuda loglanir, istemciye sizdirilmaz. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Beklenmeyen hata: {} {}", request.getMethod(), request.getRequestURI(), ex);

        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "Beklenmeyen bir sunucu hatasi olustu.", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, ErrorCode code, String message,
                                           HttpServletRequest request) {
        return build(status, code, message, request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, ErrorCode code, String message,
                                           HttpServletRequest request, List<ApiFieldError> fieldErrors) {
        ApiError body = ApiError.of(status.value(), code, message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
