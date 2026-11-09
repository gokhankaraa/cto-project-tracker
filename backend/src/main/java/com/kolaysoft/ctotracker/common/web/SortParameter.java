package com.kolaysoft.ctotracker.common.web;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;

import com.kolaysoft.ctotracker.common.exception.InvalidParameterException;

/**
 * {@code sort} sorgu parametresini cozer (filtre sozlesmesi, bolum 3-4 ve S-03).
 *
 * <p>Bicim: {@code alan}, {@code alan,asc} veya {@code alan,desc} (yon verilmezse artan). Yalnizca izin
 * listesindeki API alan adlari kabul edilir ve entity yoluna eslenir (or. {@code projectName -> project.name}).
 * Boylece entity alan adlari disari sizmaz ve tanimsiz bir alan Spring Data'dan istisna olarak cikip
 * 500'e dusmek yerine 400 doner. Sayfalar arasi kayma olmasin diye siralamaya her zaman ayni yonde
 * ikinci anahtar olarak {@code id} eklenir. Bos (NULL) degerler yon ne olursa olsun her zaman sona konur.
 */
public final class SortParameter {

    private static final String PARAMETER = "sort";

    private SortParameter() {
    }

    /**
     * @param value        istemcinin gonderdigi deger; null veya bos ise {@code defaultValue} kullanilir
     * @param allowed      API alan adi -&gt; entity yolu
     * @param defaultValue varsayilan siralama, ayni bicimde (or. {@code "reportDate,desc"})
     * @throws InvalidParameterException bicim, alan veya yon gecersizse
     */
    public static Sort parse(String value, Map<String, String> allowed, String defaultValue) {
        String effective = value == null || value.isBlank() ? defaultValue : value.strip();
        String[] parts = effective.split(",", -1);
        if (parts.length > 2) {
            throw invalid(effective, allowed);
        }

        String path = allowed.get(parts[0].strip());
        if (path == null) {
            throw invalid(effective, allowed);
        }

        Sort.Direction direction = parts.length == 1
                ? Sort.Direction.ASC
                : Sort.Direction.fromOptionalString(parts[1].strip())
                        .orElseThrow(() -> invalid(effective, allowed));

        // ignoreCase: metin alanlari (or. projectName) buyuk/kucuk harfe duyarsiz siralanir; aksi halde sonuc
        // veritabaninin collation ayarina baglidir (or. "C" collation'da buyuk harfle baslayan "Nova POS",
        // kucuk harfle baslayan "e-Belge"den once gelir). Spring Data bunu yalnizca String tipindeki alanlara
        // uygular; tarih/sayi alanlari etkilenmez.
        // nullsLast: tarihsiz kayitlar (or. plannedDate) her iki yonde de sonda. PostgreSQL'in varsayilani
        // artan siralamada NULL'lari sona, azalan siralamada ise basa koymaktir; acikca belirtilir.
        Sort.Order primary = new Sort.Order(direction, path).ignoreCase().nullsLast();
        return Sort.by(primary, new Sort.Order(direction, "id"));
    }

    private static InvalidParameterException invalid(String value, Map<String, String> allowed) {
        String fields = allowed.keySet().stream().sorted().collect(Collectors.joining(", "));
        return new InvalidParameterException(PARAMETER,
                "Gecersiz siralama: '%s'. Bicim 'alan' veya 'alan,asc|desc' olmalidir; izin verilen alanlar: %s."
                        .formatted(value, fields));
    }
}
