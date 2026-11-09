-- V3: raporun yili, rapor tarihinin degil anlattigi haftanin yilidir.
-- V2 yili rapor tarihinin ISO yilindan turetiyordu. Rapor, anlattigi haftanin ertesi gunlerinde
-- yazilabildigi icin (or. 2026'nin 53. haftasinin raporu 4 Ocak 2027 pazartesi) bu, yil sinirinda yanlis
-- yili (2027 / 53. hafta) veriyordu. Yeni kural (Java: WeeklyReport#yearOf(Integer, LocalDate)):
-- hafta numarasinin rapor tarihine en yakin oldugu yil; adaylar tarihin ISO yili, bir oncesi ve bir sonrasidir,
-- o yilda bu hafta yoksa aday degildir. Hicbir adayda yoksa yil degismez.
--
-- to_date(..., 'IYYY-IW-ID') ISO yil + hafta + gunden tarih uretir. Yilda olmayan 53. hafta hata vermeden
-- sonraki yila tasar; bu yuzden uretilen tarihin ISO yili adayla ayni degilse o yilda bu hafta yoktur.
-- Seed verisinde degisen kayit yoktur; bu migration yalnizca yil sinirindaki hatali kayitlari duzeltir.

UPDATE weekly_reports r
SET year = fixed.year
FROM (
    SELECT w.id,
           (SELECT c.year
            FROM generate_series(CAST(EXTRACT(ISOYEAR FROM w.report_date) AS INTEGER) - 1,
                                 CAST(EXTRACT(ISOYEAR FROM w.report_date) AS INTEGER) + 1) AS c(year)
            CROSS JOIN LATERAL (
                SELECT to_date(c.year || '-' || w.week_number || '-1', 'IYYY-IW-ID') AS monday
            ) m
            WHERE EXTRACT(ISOYEAR FROM m.monday) = c.year
            ORDER BY abs(m.monday - w.report_date)
            LIMIT 1) AS year
    FROM weekly_reports w
) fixed
WHERE r.id = fixed.id
  AND fixed.year IS NOT NULL
  AND fixed.year <> r.year;
