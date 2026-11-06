-- V2: haftalik rapora yil (T13 analiz notu, bolum 10, soru 2).
-- Hafta numarasi yil icermedigi icin 2026'nin 29. haftasi ile 2027'nin 29. haftasi ayni raporla
-- cakisiyordu. Yil ayri girilmez; rapor tarihinin ISO hafta-yilindan turetilir (Java tarafinda
-- WeeklyReport#setReportDate, IsoFields.WEEK_BASED_YEAR). Ornek: 2027-01-01 (Cuma) ISO'ya gore
-- 2026'nin 53. haftasindadir, bu yuzden yili 2026'dir.

ALTER TABLE weekly_reports ADD COLUMN year INTEGER;

-- Mevcut raporlar ayni kuralla doldurulur, sonra kolon zorunlu yapilir.
UPDATE weekly_reports SET year = EXTRACT(ISOYEAR FROM report_date);
ALTER TABLE weekly_reports ALTER COLUMN year SET NOT NULL;

-- Benzersizlik artik proje + yil + hafta. Yeni kisit da project_id ile basladigi icin
-- proje bazli sorgularda (ve yabanci anahtarda) indeks olarak kullanilmaya devam eder.
ALTER TABLE weekly_reports DROP CONSTRAINT uk_weekly_report_project_week;
ALTER TABLE weekly_reports
    ADD CONSTRAINT uk_weekly_report_project_year_week UNIQUE (project_id, year, week_number);
