package com.kolaysoft.ctotracker.dto.filter;

import java.util.List;
import java.util.Objects;

import com.kolaysoft.ctotracker.common.exception.InvalidParameterException;
import com.kolaysoft.ctotracker.entity.WeeklyReport;

/** Filtre kayıtlarının ortak normalleştirme kuralları (filtre sözleşmesi, bölüm 1). */
final class FilterValues {

    /** Yalnızca yıl verildiğinde referans nokta yılın son olası haftasıdır. */
    private static final int LAST_WEEK_OF_YEAR = 53;

    private FilterValues() {
    }

    /**
     * Çok değerli bir parametreyi null içermeyen, tekrarsız ve değiştirilemez bir listeye çevirir.
     * Parametre verilmemişse boş liste döner; boş liste "filtre yok" demektir.
     *
     * <p>Null'ların elenmesi gerekli: {@code ?riskLevel=ORTA,} gibi bir istekte Spring sondaki boş
     * parçayı enum'a çevirirken null üretir.
     */
    static <E> List<E> values(List<E> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).distinct().toList();
    }

    /** Boş veya yalnızca boşluktan oluşan metni "verilmemiş" (null) sayar, kalan metni kırpar. */
    static String text(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /**
     * Hafta numarası yıl olmadan anlamsızdır (2026'nın 29. haftası ile 2027'ninki farklıdır), bu yüzden
     * bir hafta parametresi verildiyse {@code year} de verilmiş olmalıdır. Aksi halde 400
     * VALIDATION_ERROR döner ve hata hafta parametresinde raporlanır (filtre sözleşmesi, bölüm 5).
     */
    static void requireYear(Integer year, Integer week, String weekParameter) {
        if (week != null && year == null) {
            throw new InvalidParameterException(weekParameter,
                    "Hafta (" + weekParameter + ") yil (year) ile birlikte verilmelidir.");
        }
    }

    /**
     * Değerlendirilen raporu seçen referans noktanın anahtarı ({@link WeeklyReport#weekKey}):
     * yıl yoksa null (en son rapor), yalnızca yıl varsa o yılın sonu, yıl + hafta varsa o hafta.
     */
    static Integer referenceKey(Integer year, Integer week) {
        if (year == null) {
            return null;
        }
        return WeeklyReport.weekKey(year, week != null ? week : LAST_WEEK_OF_YEAR);
    }
}
