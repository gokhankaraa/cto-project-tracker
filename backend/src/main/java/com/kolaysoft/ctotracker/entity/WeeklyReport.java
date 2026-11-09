package com.kolaysoft.ctotracker.entity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bir projenin belirli bir haftaya ait durum raporu (on analiz, bolum 4).
 *
 * <p>Bir proje bir yilin bir haftasi icin yalnizca bir rapor alabilir: {@code (project_id, year, week_number)}
 * uzerinde benzersizlik kisiti tanimlidir. Bu kural veritabani duzeyinde garanti edilir;
 * servis katmani da ayni durumu onceden kontrol edip anlamli 409 hatasi dondurur.
 *
 * <p>Yil disaridan girilmez; raporun anlattigi haftanin ISO yilidir ve hafta ile rapor tarihinden
 * birlikte hesaplanir (bkz. {@link #yearOf(Integer, LocalDate)}). Rapor, anlattigi haftanin ertesi gunlerinde
 * yazilabildigi icin (seed verisinde ertesi pazartesi) yil, rapor tarihinin yili olmayabilir: 2026'nin
 * 53. haftasinin 4 Ocak 2027'de yazilan raporu 2026 yilina aittir.
 */
@Entity
@Table(name = "weekly_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_weekly_report_project_year_week",
                columnNames = {"project_id", "year", "week_number"}))
@Getter
@Setter
@NoArgsConstructor
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private LocalDate reportDate;

    /** Anlatilan haftanin ISO yili; hafta ya da rapor tarihi degisince yeniden hesaplanir. */
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private Integer year;

    @Setter(AccessLevel.NONE)
    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    /** Ilerleme kademesi; yuzde bu kademeden turer (bkz. {@link ProgressStage}). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStage progressStage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OverallStatus overallStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    /** O hafta yapilanlar (serbest metin). */
    @Column(length = 2000)
    private String done;

    /** Gelecek hafta yapilacaklar (serbest metin). */
    @Column(length = 2000)
    private String planned;

    /** Riskler / engeller (serbest metin). */
    @Column(length = 2000)
    private String risks;

    /** Genel durum notu (serbest metin). */
    @Column(length = 2000)
    private String note;

    /** Hafta numarasini ayarlar; yil yeniden hesaplanir (sira onemsiz, bkz. {@link #setReportDate}). */
    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
        this.year = yearOf(weekNumber, reportDate);
    }

    /** Rapor tarihini ayarlar; yil yeniden hesaplanir. */
    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
        this.year = yearOf(weekNumber, reportDate);
    }

    /**
     * Raporun anlattigi haftanin yili: verilen hafta numarasinin rapor tarihine en yakin oldugu yil (tarihin
     * ISO yili, bir oncesi ve bir sonrasi arasindan; o yilda bu hafta yoksa aday degildir). Gecerli bir raporda
     * (tarih haftanin icinde ya da ertesi haftada, bkz. WeeklyReportService) bu tam olarak anlatilan haftadir;
     * adaylar arasinda yaklasik bir yil fark oldugu icin sonuc tektir. Hicbir adayda hafta yoksa (53. hafta)
     * tarihin ISO yili doner; servis bu durumu ayrica reddeder. V3 migration'i ayni kurali SQL'de uygular.
     */
    public static Integer yearOf(Integer weekNumber, LocalDate reportDate) {
        if (weekNumber == null || reportDate == null) {
            return null;
        }
        int dateYear = reportDate.get(IsoFields.WEEK_BASED_YEAR);
        return IntStream.of(dateYear, dateYear - 1, dateYear + 1)
                .filter(year -> hasWeek(year, weekNumber))
                .boxed()
                .min(Comparator.comparingLong(
                        year -> Math.abs(ChronoUnit.DAYS.between(mondayOf(year, weekNumber), reportDate))))
                .orElse(dateYear);
    }

    /** ISO yilinda bu hafta var mi: 1-52 her yilda, 53 yalnizca 53 haftalik yillarda (28 Aralik'in haftasi). */
    public static boolean hasWeek(int year, int weekNumber) {
        int weeksInYear = LocalDate.of(year, 12, 28).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return weekNumber >= 1 && weekNumber <= weeksInYear;
    }

    /** ISO yilinin verilen haftasinin pazartesisi (2026, 31 -> 2026-07-27). */
    public static LocalDate mondayOf(int year, int weekNumber) {
        return LocalDate.of(year, 1, 4)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, weekNumber)
                .with(DayOfWeek.MONDAY);
    }

    /**
     * Yil ve haftayi tek sayida birlestirir: {@code year * 100 + weekNumber} (or. 2026 h30 = 202630).
     * Hafta en fazla 53 oldugu icin bu sayinin sirasi (yil, hafta) sirasiyla aynidir; "o haftaya kadarki
     * en son rapor" tek bir karsilastirma ile bulunur. Sorgu tarafindaki karsiligi
     * {@code WorkItemSpecifications#weekKey} ve {@code WeeklyReportRepository#findLatestByProjectIdUpTo}'dur.
     */
    public static int weekKey(int year, int weekNumber) {
        return year * 100 + weekNumber;
    }

    /** Rapora bağlı iş kalemleri; rapor silinince bunlar da silinir. */
    @OneToMany(mappedBy = "weeklyReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkItem> workItems = new ArrayList<>();
}
