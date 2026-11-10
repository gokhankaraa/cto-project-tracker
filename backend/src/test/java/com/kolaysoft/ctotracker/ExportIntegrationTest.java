package com.kolaysoft.ctotracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.config.ClockConfig;
import com.kolaysoft.ctotracker.export.DashboardExportService;

/**
 * CTO ozetinin Excel ciktisi HTTP uzerinden: uretilen dosya POI ile geri okunur ve icerigi seed verisine gore
 * dogrulanir (proje 1 Atlas IK, 2 e-Belge, 3 Nova POS; 5 takip kaydindan 1'i kapali).
 */
@SpringBootTest
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class ExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private Workbook download(String query) throws Exception {
        byte[] body = mockMvc.perform(get("/api/dashboard/export" + query))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return new XSSFWorkbook(new ByteArrayInputStream(body));
    }

    private static String text(Sheet sheet, int row, int column) {
        Row r = sheet.getRow(row);
        Cell cell = r == null ? null : r.getCell(column);
        return cell == null ? null : cell.getStringCellValue();
    }

    /** Ilk sutundaki etiketin yanindaki sayi (Ozet sayfasi). */
    private static double counter(Sheet summary, String label) {
        for (Row row : summary) {
            Cell first = row.getCell(0);
            if (first != null && first.getCellType() == CellType.STRING && label.equals(first.getStringCellValue())) {
                return row.getCell(1).getNumericCellValue();
            }
        }
        throw new AssertionError("Sayac yok: " + label);
    }

    private static List<String> column(Sheet sheet, int column) {
        List<String> values = new ArrayList<>();
        IntStream.rangeClosed(1, sheet.getLastRowNum()).forEach(i -> values.add(text(sheet, i, column)));
        return values;
    }

    @Test
    @DisplayName("Dort sayfa; dosya adi bugunun tarihiyle; Excel turu ve indirme basligi")
    void fileAndSheets() throws Exception {
        String today = LocalDate.now(ClockConfig.ZONE).toString();
        mockMvc.perform(get("/api/dashboard/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", DashboardExportService.CONTENT_TYPE))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment; filename=\"cto-ozeti-" + today + ".xlsx\"")));

        try (Workbook workbook = download("")) {
            assertThat(IntStream.range(0, workbook.getNumberOfSheets()).mapToObj(workbook::getSheetName))
                    .containsExactly("Özet", "Projeler", "Riskler ve aksiyonlar", "Trend");

            Sheet summary = workbook.getSheet("Özet");
            assertThat(text(summary, 2, 1)).isEqualTo("Yok (tüm projeler, en son rapor)");
            assertThat(counter(summary, "Toplam proje")).isEqualTo(3);
            assertThat(counter(summary, "Geciken")).isEqualTo(1);
            assertThat(counter(summary, "Açık engel")).isEqualTo(1);
            assertThat(counter(summary, "Gecikmiş takip")).isEqualTo(1);
            assertThat(DateUtil.isCellDateFormatted(summary.getRow(1).getCell(1))).isTrue();
        }
    }

    @Test
    @DisplayName("Projeler: dashboard satirlari, Turkce etiketler; ilerleme sayi, raporsuz projede bos")
    void projectsSheet() throws Exception {
        try (Workbook workbook = download("")) {
            Sheet projects = workbook.getSheet("Projeler");
            assertThat(text(projects, 0, 0)).isEqualTo("Proje");
            assertThat(column(projects, 0)).containsExactly("Atlas IK", "e-Belge", "Nova POS");

            Row atlas = projects.getRow(1);
            assertThat(atlas.getCell(6).getStringCellValue()).isEqualTo("Test");
            assertThat(atlas.getCell(7).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(atlas.getCell(7).getNumericCellValue()).isEqualTo(75);
            assertThat(atlas.getCell(11).getStringCellValue()).isEqualTo("Evet");
            assertThat(atlas.getCell(14).getStringCellValue()).isEqualTo("Yüksek");

            Row nova = projects.getRow(3);
            assertThat(nova.getCell(6).getStringCellValue()).isEqualTo("Rapor yok");
            assertThat(nova.getCell(7)).isNull();
            // Baslik satiri sabit, sutunlarda filtre acik.
            assertThat(projects.getPaneInformation().isFreezePane()).isTrue();
        }
    }

    @Test
    @DisplayName("Riskler ve aksiyonlar: kapanmamis kayitlar, gecikmis once; takip tarihi tarih hucresi")
    void trackingSheet() throws Exception {
        try (Workbook workbook = download("")) {
            Sheet items = workbook.getSheet("Riskler ve aksiyonlar");
            assertThat(items.getLastRowNum()).isEqualTo(4);
            assertThat(column(items, 7)).doesNotContain("Kapandı", "Karar verildi").contains("Karar bekleniyor");

            Row first = items.getRow(1);
            assertThat(first.getCell(0).getStringCellValue()).isEqualTo("Aksiyon");
            assertThat(first.getCell(6).getStringCellValue()).isEqualTo("Evet");
            assertThat(DateUtil.isCellDateFormatted(first.getCell(5))).isTrue();
            assertThat(first.getCell(5).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.now(ClockConfig.ZONE).minusDays(2));
        }
    }

    @Test
    @DisplayName("Trend: ayni referansla biten 12 hafta")
    void trendSheet() throws Exception {
        try (Workbook workbook = download("?year=2026&weekNumber=31")) {
            Sheet trend = workbook.getSheet("Trend");
            assertThat(trend.getLastRowNum()).isEqualTo(12);
            Row last = trend.getRow(12);
            assertThat(last.getCell(1).getNumericCellValue()).isEqualTo(31);
            assertThat(last.getCell(10).getNumericCellValue()).isEqualTo(1);
            assertThat(DateUtil.isCellDateFormatted(last.getCell(2))).isTrue();
        }
    }

    @Test
    @DisplayName("Dashboard filtreleri dosyaya da uygulanir ve Ozet'te yazilir")
    void filtersApply() throws Exception {
        try (Workbook workbook = download("?riskLevel=YUKSEK")) {
            assertThat(text(workbook.getSheet("Özet"), 2, 1)).isEqualTo("risk: Yüksek");
            assertThat(column(workbook.getSheet("Projeler"), 0)).containsExactly("e-Belge");
            assertThat(column(workbook.getSheet("Riskler ve aksiyonlar"), 2)).containsOnly("e-Belge");
        }
    }

    @Test
    @DisplayName("Esittir ile baslayan baslik formul degil metin olarak yazilir")
    void textIsNeverAFormula() throws Exception {
        mockMvc.perform(post("/api/projects/3/tracking-items").contentType(MediaType.APPLICATION_JSON).content("""
                        { "type": "AKSIYON", "title": "=HYPERLINK(\\"http://ornek\\")", "assignee": "Ayse",
                          "followUpDate": "%s", "status": "ACIK" }
                        """.formatted(LocalDate.now(ClockConfig.ZONE).plusDays(1))))
                .andExpect(status().isCreated());

        try (Workbook workbook = download("?projectId=3")) {
            Cell title = workbook.getSheet("Riskler ve aksiyonlar").getRow(1).getCell(1);
            assertThat(title.getCellType()).isEqualTo(CellType.STRING);
            assertThat(title.getStringCellValue()).startsWith("=HYPERLINK");
        }
    }

    @Test
    @DisplayName("Accept basligindan bagimsiz: JSON isteyene de dosya, hatali parametreye JSON 400")
    void contentNegotiation() throws Exception {
        mockMvc.perform(get("/api/dashboard/export").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", DashboardExportService.CONTENT_TYPE));
        mockMvc.perform(get("/api/dashboard/export?weekNumber=31")
                        .accept(MediaType.parseMediaType(DashboardExportService.CONTENT_TYPE), MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("weekNumber"));
    }
}
