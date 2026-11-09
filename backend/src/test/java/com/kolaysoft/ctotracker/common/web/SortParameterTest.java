package com.kolaysoft.ctotracker.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

import com.kolaysoft.ctotracker.common.exception.InvalidParameterException;

/** SortParameter birim testleri (T13 filtre sozlesmesi, S-03). */
class SortParameterTest {

    private static final Map<String, String> ALLOWED = Map.of(
            "reportDate", "reportDate",
            "weekNumber", "weekNumber",
            "projectName", "project.name");

    private static final String DEFAULT = "reportDate,desc";

    @Test
    @DisplayName("Deger verilmezse varsayilan siralama ve ayni yonde ikinci anahtar id kullanilir")
    void nullOrBlankUsesDefault() {
        for (String value : new String[] {null, "", "   "}) {
            List<Sort.Order> orders = SortParameter.parse(value, ALLOWED, DEFAULT).toList();

            assertThat(orders).hasSize(2);
            assertThat(orders.get(0).getProperty()).isEqualTo("reportDate");
            assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
            assertThat(orders.get(1).getProperty()).isEqualTo("id");
            assertThat(orders.get(1).getDirection()).isEqualTo(Sort.Direction.DESC);
        }
    }

    @Test
    @DisplayName("Yon verilmezse artan; API alan adi entity yoluna eslenir")
    void fieldWithoutDirectionIsAscendingAndMapped() {
        Sort.Order primary = SortParameter.parse("projectName", ALLOWED, DEFAULT).toList().get(0);

        assertThat(primary.getProperty()).isEqualTo("project.name");
        assertThat(primary.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("Bosluklar kirpilir, yon buyuk/kucuk harfe duyarsizdir")
    void trimsAndAcceptsUppercaseDirection() {
        Sort.Order primary = SortParameter.parse("  weekNumber , DESC ", ALLOWED, DEFAULT).toList().get(0);

        assertThat(primary.getProperty()).isEqualTo("weekNumber");
        assertThat(primary.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Birincil siralama buyuk/kucuk harfe duyarsiz ve bos degerler sonda; id anahtari degil")
    void primaryOrderIgnoresCaseAndPutsNullsLast() {
        List<Sort.Order> orders = SortParameter.parse("projectName,asc", ALLOWED, DEFAULT).toList();

        assertThat(orders.get(0).isIgnoreCase()).isTrue();
        assertThat(orders.get(0).getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);
        assertThat(orders.get(1).isIgnoreCase()).isFalse();
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' reddedilir")
    @ValueSource(strings = {"password", "project.name", "reportDate,yukari", "reportDate,desc,x", ",asc"})
    @DisplayName("Tanimsiz alan, entity yolu, gecersiz yon veya bicim 'sort' parametresinde 400'e donusur")
    void invalidValuesAreRejected(String value) {
        assertThatThrownBy(() -> SortParameter.parse(value, ALLOWED, DEFAULT))
                .isInstanceOf(InvalidParameterException.class)
                .satisfies(ex -> assertThat(((InvalidParameterException) ex).getParameter()).isEqualTo("sort"))
                .hasMessageContaining("projectName, reportDate, weekNumber");
    }
}
