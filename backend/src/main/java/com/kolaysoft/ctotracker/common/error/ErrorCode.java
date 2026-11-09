package com.kolaysoft.ctotracker.common.error;

/**
 * API'nin dondugu hata kodlari. Arayuz tarafi HTTP durum kodundan bagimsiz olarak
 * bu kodla dallanabilsin diye mesajdan ayri bir alan olarak tutuluyor.
 */
public enum ErrorCode {

    /** Alan bazli dogrulama hatasi (zorunlu alan, format, aralik). */
    VALIDATION_ERROR,

    /** Istek govdesi/parametresi okunamadi: bozuk JSON, tanimsiz enum, hatali tarih. */
    INVALID_REQUEST,

    /** Istenen kayit bulunamadi. */
    RESOURCE_NOT_FOUND,

    /** Benzersizlik ihlali: ayni proje+hafta raporu, kayitli e-posta. */
    DUPLICATE_RESOURCE,

    /** Kaynak baska kayitlar tarafindan kullanildigi icin islem yapilamiyor (or. raporu olan proje silinemez). */
    RESOURCE_IN_USE,

    /** Is kurali ihlali: ornegin ilerleme kademesinde sira atlanmasi. */
    BUSINESS_RULE_VIOLATION,

    /** Endpoint bu HTTP metodunu desteklemiyor. */
    METHOD_NOT_ALLOWED,

    /** Beklenmeyen sunucu hatasi; teknik detay istemciye sizdirilmaz. */
    INTERNAL_ERROR
}
