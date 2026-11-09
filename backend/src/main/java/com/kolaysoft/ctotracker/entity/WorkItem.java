package com.kolaysoft.ctotracker.entity;

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
 * Bir haftalık rapora bağlı iş kalemi (on analiz, bölüm 4 ve H-05).
 *
 * <p>Her iş kalemi bir haftalık rapora bağlıdır; başlık ve durum zorunludur.
 * Sorumlu (assignee) MVP'de kullanıcı kaydına bağlanmayan serbest bir metin alanıdır.
 * Rapor silindiğinde ona bağlı iş kalemleri de silinir (ilişki WeeklyReport tarafında
 * cascade/orphanRemoval ile tanımlıdır).
 */
@Entity
@Table(name = "work_items")
@Getter
@Setter
@NoArgsConstructor
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_report_id", nullable = false)
    private WeeklyReport weeklyReport;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    /** Sorumlu; MVP'de serbest metin (kullanıcı kaydına bağlanmaz). */
    private String assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkItemStatus status;

    private LocalDate plannedDate;

    private LocalDate completedDate;
}
