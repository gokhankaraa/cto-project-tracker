package com.kolaysoft.ctotracker.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kolaysoft.ctotracker.config.ClockConfig;
import com.kolaysoft.ctotracker.dto.DashboardProjectSummary;
import com.kolaysoft.ctotracker.dto.DashboardSummaryResponse;
import com.kolaysoft.ctotracker.dto.TrackingItemResponse;
import com.kolaysoft.ctotracker.dto.TrendWeek;
import com.kolaysoft.ctotracker.dto.filter.DashboardFilter;
import com.kolaysoft.ctotracker.dto.filter.TrendFilter;
import com.kolaysoft.ctotracker.entity.OverallStatus;
import com.kolaysoft.ctotracker.service.DashboardService;
import com.kolaysoft.ctotracker.service.TrackingItemService;
import com.kolaysoft.ctotracker.service.TrendService;

/**
 * CTO ozetinin Excel (.xlsx) ciktisi: dashboard'da gorulenin dosya hali, ayni filtrelerle.
 *
 * <ul>
 *   <li><b>Özet</b>: olusturulma zamani, uygulanan filtreler, sayaclar</li>
 *   <li><b>Projeler</b>: dashboard tablosunun satirlari</li>
 *   <li><b>Riskler ve aksiyonlar</b>: listelenen projelerin kapanmamis kayitlari</li>
 *   <li><b>Trend</b>: ayni referansla biten son {@value TrendService#DEFAULT_WEEKS} hafta</li>
 * </ul>
 *
 * Veri mevcut servislerden gelir; gorunurluk (proje yoneticisi yalnizca kendi projeleri) onlarda uygulanir.
 * Sayilar sayi, tarihler tarih hucresi olarak yazilir. Metinler her zaman metin hucresidir: "=" ile baslayan bir
 * baslik formul olarak calismaz. Sutun genislikleri sabittir (otomatik genislik font ister; sunucu imajinda yok).
 */
@Service
@Transactional(readOnly = true)
public class DashboardExportService {

    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DashboardService dashboardService;
    private final TrackingItemService trackingItemService;
    private final TrendService trendService;
    private final Clock clock;

    public DashboardExportService(DashboardService dashboardService, TrackingItemService trackingItemService,
                                  TrendService trendService, Clock clock) {
        this.dashboardService = dashboardService;
        this.trackingItemService = trackingItemService;
        this.trendService = trendService;
        this.clock = clock;
    }

    /** Dosya adi: cto-ozeti-2026-11-10.xlsx (bugunun tarihi, Turkiye saatiyle). */
    public String fileName() {
        return "cto-ozeti-" + LocalDate.now(clock) + ".xlsx";
    }

    public byte[] export(DashboardFilter filter) {
        DashboardSummaryResponse summary = dashboardService.getSummary(filter);
        List<Long> projectIds = summary.projects().stream().map(DashboardProjectSummary::projectId).toList();
        List<TrackingItemResponse> openItems = trackingItemService.openItemsOf(projectIds);
        List<TrendWeek> trend = trendService.getTrend(new TrendFilter(filter.projectId(), filter.projectStatuses(),
                filter.ownerId(), filter.year(), filter.weekNumber(), TrendService.DEFAULT_WEEKS)).weeks();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            writeSummary(workbook.createSheet("Özet"), styles, filter, summary);
            writeProjects(workbook.createSheet("Projeler"), styles, summary.projects());
            writeTrackingItems(workbook.createSheet("Riskler ve aksiyonlar"), styles, openItems);
            writeTrend(workbook.createSheet("Trend"), styles, trend);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Excel dosyası oluşturulamadı.", e);
        }
    }

    // --- Sayfalar ---

    private void writeSummary(Sheet sheet, Styles styles, DashboardFilter filter, DashboardSummaryResponse summary) {
        sheet.setColumnWidth(0, 32 * 256);
        sheet.setColumnWidth(1, 60 * 256);
        int r = 0;
        Row title = sheet.createRow(r++);
        text(title, 0, "CTO portföy özeti", styles.title);
        Row created = sheet.createRow(r++);
        text(created, 0, "Oluşturulma", null);
        dateTime(created, 1, LocalDateTime.now(clock.withZone(ClockConfig.ZONE)), styles.dateTime);
        Row filters = sheet.createRow(r++);
        text(filters, 0, "Filtreler", null);
        text(filters, 1, describe(filter), null);
        r++;

        text(sheet.createRow(r++), 0, "Projeler (en son rapora göre)", styles.header);
        r = counter(sheet, r, "Toplam proje", summary.totalProjects());
        r = counter(sheet, r, "Yüksek riskli", summary.highRiskProjects());
        r = counter(sheet, r, "Geciken", summary.delayedProjects());
        r = counter(sheet, r, "Raporu olmayan", summary.projectsWithoutReport());
        r = counter(sheet, r, "Canlı task", summary.totalLiveTasks());
        r++;
        text(sheet.createRow(r++), 0, "Riskler ve aksiyonlar (bugünkü durum)", styles.header);
        r = counter(sheet, r, "Açık risk", summary.openRisks());
        r = counter(sheet, r, "Açık engel", summary.openBlockers());
        r = counter(sheet, r, "Bekleyen karar", summary.pendingDecisions());
        counter(sheet, r, "Gecikmiş takip", summary.overdueTrackingItems());
    }

    private void writeProjects(Sheet sheet, Styles styles, List<DashboardProjectSummary> projects) {
        header(sheet, styles, new String[] {"Proje", "Müşteri", "Sorumlu", "Proje durumu", "Rapor yılı", "Rapor haftası",
                "Aşama", "İlerleme (%)", "Genel durum", "Risk", "Canlı task", "Gecikmede", "Açık kayıt",
                "Gecikmiş kayıt", "En yüksek açık risk"},
                new int[] {22, 22, 20, 15, 11, 13, 13, 13, 13, 10, 11, 11, 11, 14, 19});
        int r = 1;
        for (DashboardProjectSummary p : projects) {
            Row row = sheet.createRow(r++);
            text(row, 0, p.name(), null);
            text(row, 1, p.customer(), null);
            text(row, 2, p.ownerFullName(), null);
            text(row, 3, ExportLabels.of(p.projectStatus()), null);
            number(row, 4, p.lastYear());
            number(row, 5, p.lastWeekNumber());
            text(row, 6, p.hasReport() ? ExportLabels.of(p.progressStage()) : "Rapor yok", null);
            number(row, 7, p.hasReport() ? p.progressPercentage() : null);
            text(row, 8, ExportLabels.of(p.overallStatus()), null);
            text(row, 9, ExportLabels.of(p.riskLevel()), null);
            number(row, 10, p.liveTaskCount());
            text(row, 11, p.delayed() ? "Evet" : "Hayır", null);
            number(row, 12, p.openTrackingItems());
            number(row, 13, p.overdueTrackingItems());
            text(row, 14, ExportLabels.of(p.highestOpenRiskLevel()), null);
        }
        finishTable(sheet, 15, r);
    }

    private void writeTrackingItems(Sheet sheet, Styles styles, List<TrackingItemResponse> items) {
        header(sheet, styles, new String[] {"Tür", "Başlık", "Proje", "Risk seviyesi", "Sorumlu", "Takip tarihi",
                "Gecikmede", "Durum", "Etki", "Aksiyon planı", "Açıklama", "Oluşturulma"},
                new int[] {10, 36, 18, 13, 20, 13, 11, 18, 36, 36, 36, 18});
        int r = 1;
        for (TrackingItemResponse item : items) {
            Row row = sheet.createRow(r++);
            text(row, 0, ExportLabels.of(item.type()), null);
            text(row, 1, item.title(), null);
            text(row, 2, item.projectName(), null);
            text(row, 3, ExportLabels.of(item.riskLevel()), null);
            text(row, 4, item.assignee(), null);
            date(row, 5, item.followUpDate(), styles.date);
            text(row, 6, item.overdue() ? "Evet" : "Hayır", null);
            text(row, 7, ExportLabels.of(item.type(), item.status()), null);
            text(row, 8, item.impact(), null);
            text(row, 9, item.actionPlan(), null);
            text(row, 10, item.description(), null);
            dateTime(row, 11, LocalDateTime.ofInstant(item.createdAt(), ClockConfig.ZONE), styles.dateTime);
        }
        finishTable(sheet, 12, r);
    }

    private void writeTrend(Sheet sheet, Styles styles, List<TrendWeek> weeks) {
        header(sheet, styles, new String[] {"Yıl", "Hafta", "Hafta başı", "Proje", "Raporlu", "Yolunda", "Riskli",
                "Gecikmeli", "Bloke", "Yüksek riskli", "Geciken", "Ort. ilerleme (%)", "Açık risk", "Açık engel",
                "Bekleyen karar", "Açık aksiyon", "Açılan kayıt", "Kapanan kayıt"},
                new int[] {8, 8, 12, 9, 10, 10, 9, 11, 9, 13, 10, 17, 11, 11, 15, 13, 13, 14});
        int r = 1;
        for (TrendWeek w : weeks) {
            Row row = sheet.createRow(r++);
            number(row, 0, w.year());
            number(row, 1, w.weekNumber());
            date(row, 2, w.weekStart(), styles.date);
            number(row, 3, w.totalProjects());
            number(row, 4, w.projectsWithReport());
            number(row, 5, w.projectsByStatus().get(OverallStatus.YOLUNDA));
            number(row, 6, w.projectsByStatus().get(OverallStatus.RISKLI));
            number(row, 7, w.projectsByStatus().get(OverallStatus.GECIKMELI));
            number(row, 8, w.projectsByStatus().get(OverallStatus.BLOKE));
            number(row, 9, w.highRiskProjects());
            number(row, 10, w.delayedProjects());
            number(row, 11, w.averageProgress());
            number(row, 12, w.openRisks());
            number(row, 13, w.openBlockers());
            number(row, 14, w.pendingDecisions());
            number(row, 15, w.openActions());
            number(row, 16, w.openedTrackingItems());
            number(row, 17, w.closedTrackingItems());
        }
        finishTable(sheet, 18, r);
    }

    // --- Yardimcilar ---

    /** Uygulanan filtrelerin okunur hali; Excel'i acan kisi neye baktigini bilsin. */
    static String describe(DashboardFilter f) {
        List<String> parts = new ArrayList<>();
        if (f.year() != null) {
            parts.add(f.weekNumber() == null ? f.year() + " sonuna kadarki en son rapor"
                    : f.year() + " yılı " + f.weekNumber() + ". haftaya kadarki en son rapor");
        }
        if (f.projectId() != null) {
            parts.add("proje id " + f.projectId());
        }
        if (f.ownerId() != null) {
            parts.add("sorumlu id " + f.ownerId());
        }
        if (!f.projectStatuses().isEmpty()) {
            parts.add("proje durumu: " + f.projectStatuses().stream().map(ExportLabels::of).collect(Collectors.joining(", ")));
        }
        if (!f.overallStatuses().isEmpty()) {
            parts.add("genel durum: " + f.overallStatuses().stream().map(ExportLabels::of).collect(Collectors.joining(", ")));
        }
        if (!f.riskLevels().isEmpty()) {
            parts.add("risk: " + f.riskLevels().stream().map(ExportLabels::of).collect(Collectors.joining(", ")));
        }
        if (f.delayed() != null) {
            parts.add(f.delayed() ? "yalnızca gecikenler" : "gecikmeyenler");
        }
        return parts.isEmpty() ? "Yok (tüm projeler, en son rapor)" : String.join("; ", parts);
    }

    private static int counter(Sheet sheet, int r, String label, long value) {
        Row row = sheet.createRow(r);
        text(row, 0, label, null);
        row.createCell(1).setCellValue(value);
        return r + 1;
    }

    private static void header(Sheet sheet, Styles styles, String[] titles, int[] widths) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            text(row, i, titles[i], styles.header);
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    /** Baslik satiri kaydirinca sabit kalir, sutunlarda Excel filtresi acilir. */
    private static void finishTable(Sheet sheet, int columns, int rows) {
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(rows - 1, 0), 0, columns - 1));
    }

    /** Bos deger hucre olusturmaz; metin her zaman metin hucresidir (formul olarak yorumlanmaz). */
    private static void text(Row row, int column, String value, CellStyle style) {
        if (value == null) {
            return;
        }
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static void number(Row row, int column, Number value) {
        if (value != null) {
            row.createCell(column).setCellValue(value.doubleValue());
        }
    }

    private static void date(Row row, int column, LocalDate value, CellStyle style) {
        if (value != null) {
            Cell cell = row.createCell(column);
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }
    }

    private static void dateTime(Row row, int column, LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /** Calisma kitabinin ortak hucre bicimleri (her hucre icin yeni stil olusturmak dosyayi bozar). */
    private static final class Styles {

        final CellStyle header;
        final CellStyle title;
        final CellStyle date;
        final CellStyle dateTime;

        Styles(Workbook workbook) {
            CreationHelper helper = workbook.getCreationHelper();
            Font bold = workbook.createFont();
            bold.setBold(true);
            header = workbook.createCellStyle();
            header.setFont(bold);
            Font big = workbook.createFont();
            big.setBold(true);
            big.setFontHeightInPoints((short) 14);
            title = workbook.createCellStyle();
            title.setFont(big);
            date = workbook.createCellStyle();
            date.setDataFormat(helper.createDataFormat().getFormat("dd.mm.yyyy"));
            dateTime = workbook.createCellStyle();
            dateTime.setDataFormat(helper.createDataFormat().getFormat("dd.mm.yyyy hh:mm"));
        }
    }
}
