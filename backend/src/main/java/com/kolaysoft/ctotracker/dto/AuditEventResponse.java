package com.kolaysoft.ctotracker.dto;

import java.time.Instant;
import java.util.List;

import com.kolaysoft.ctotracker.entity.AuditAction;
import com.kolaysoft.ctotracker.entity.AuditEntityType;

/**
 * Degisiklik gecmisinin bir olayi. Degerler ham gelir (enum adi, ISO tarih, sayi); Turkce etiketleri arayuz basar.
 *
 * @param actorId   yazan kullanici; kullanici silinmisse null (ad yine dolu)
 * @param summary   kaydin o anki okunur adi (baslik, proje adi ya da "2026 / 45. hafta")
 * @param changes   guncellemede degisen alanlar, eklemede doldurulan alanlar, silmede bos
 */
public record AuditEventResponse(
        Long id,
        Instant occurredAt,
        Long actorId,
        String actorName,
        AuditAction action,
        AuditEntityType entityType,
        Long entityId,
        String summary,
        List<Change> changes) {

    /** Tek alan: eklemede oldValue, alani bosaltan guncellemede newValue null'dir. */
    public record Change(String field, String oldValue, String newValue) {
    }
}
