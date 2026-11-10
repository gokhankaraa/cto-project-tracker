package com.kolaysoft.ctotracker.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Projenin risk, engel, karar ya da aksiyon kaydi (takip kaydi). Haftaya degil projeye baglidir:
 * bir risk kapanana kadar haftalar boyunca yasar. Kurallar {@code TrackingItemService}'te,
 * veriyi tutarli birakanlari ayrica V5 migration'indaki CHECK kisitlarindadir.
 */
@Entity
@Table(name = "tracking_items")
@Getter
@Setter
@NoArgsConstructor
public class TrackingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackingItemType type;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    /** Yalnizca risk ve engel kayitlarinda; digerlerinde null. */
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    /** Etki: gerceklesirse (ya da gerceklestiginde) ne olur. */
    @Column(length = 2000)
    private String impact;

    /** Aksiyon plani: ne yapilacak (risk azaltma, cozum, karar icin gereken adimlar). */
    @Column(length = 2000)
    private String actionPlan;

    /** Sorumlu (serbest metin; sistemde hesabi olmayan biri de olabilir). */
    private String assignee;

    /** Kaydin bir sonraki kontrol tarihi. Acik kayitta zorunlu. */
    private LocalDate followUpDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackingItemStatus status;

    /** Kapanis tarihi: kayit kapaninca sunucu yazar, yeniden acilinca silinir. */
    private LocalDate closedDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
