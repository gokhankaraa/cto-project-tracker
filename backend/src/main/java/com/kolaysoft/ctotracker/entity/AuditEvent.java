package com.kolaysoft.ctotracker.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Degisiklik gecmisinin bir olayi: kim, ne zaman, hangi kayitta ne yapti (V6). Yalnizca eklenir; uygulamada
 * guncelleme ve silme yolu yoktur. Kayit ve proje yabanci anahtarla bagli degildir: kayit silindikten sonra da
 * gecmisi okunur.
 */
@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** Yazan kullanici; kullanici silinirse bos kalir (ad yine okunur). */
    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name", nullable = false)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private AuditEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /** Kaydin o anki okunur adi (proje adi, "2026 / 45. hafta", baslik): kayit silinse de gecmis anlasilir. */
    @Column(nullable = false)
    private String summary;

    /** Bir sayfadaki olaylarin alanlari tek sorguda yuklenir (sayfa boyutu en fazla 100). */
    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "audit_event_changes", joinColumns = @JoinColumn(name = "audit_event_id"))
    @OrderColumn(name = "sort_order")
    private List<AuditChange> changes = new ArrayList<>();
}
