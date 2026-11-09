package com.kolaysoft.ctotracker.entity;

/**
 * Haftalik raporun ilerleme kademesi (on analiz, K-01 karari).
 *
 * <p>Ilerleme serbest bir yuzde alani yerine tanimli kademeler uzerinden tutulur;
 * her kademe sabit bir yuzdeye karsilik gelir. Yuzde elle girilmez, kademeden turer.
 * Enum tanim sirasi ayni zamanda ilerleme sirasidir (sira atlanamaz, geriye donulemez);
 * bu kural {@code WeeklyReportService} icinde uygulanir.
 */
public enum ProgressStage {

    ANALIZ(25),
    GELISTIRME(50),
    TEST(75),
    TAMAMLANDI(100);

    private final int percentage;

    ProgressStage(int percentage) {
        this.percentage = percentage;
    }

    /** Bu kademenin karsilik geldigi tamamlanma yuzdesi (25/50/75/100). */
    public int getPercentage() {
        return percentage;
    }
}
