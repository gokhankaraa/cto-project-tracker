package com.kolaysoft.ctotracker.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.kolaysoft.ctotracker.entity.AuditEntityType;
import com.kolaysoft.ctotracker.entity.Project;
import com.kolaysoft.ctotracker.entity.TrackingItem;
import com.kolaysoft.ctotracker.entity.WeeklyReport;
import com.kolaysoft.ctotracker.entity.WorkItem;

/**
 * Bir kaydin degisiklik gecmisi icin anlik goruntusu: turu, id'si, projesi, okunur adi ve alanlarinin ham degerleri.
 * Guncellemeden once ve sonra alinan iki goruntunun farki gecmise yazilir.
 *
 * <p>Alan adlari API'deki adlardir (arayuz etiketi bunlara gore basar). Degerler metindir: enum adi, ISO tarih,
 * sayi; proje sorumlusu adiyla. Uzun metinler {@value #MAX_VALUE_LENGTH} karakterde kesilir. Parola gibi hassas
 * alanlar bu turlerde yoktur, eklenirse buraya yazilmamalidir.
 */
public record AuditSubject(AuditEntityType type, Long id, Long projectId, String summary, Map<String, String> values) {

    static final int MAX_VALUE_LENGTH = 500;
    static final int MAX_SUMMARY_LENGTH = 255;

    public AuditSubject {
        values = Collections.unmodifiableMap(values);
        summary = truncate(summary, MAX_SUMMARY_LENGTH);
    }

    public static AuditSubject of(Project p) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("name", text(p.getName()));
        v.put("customer", text(p.getCustomer()));
        v.put("description", text(p.getDescription()));
        v.put("status", text(p.getStatus()));
        v.put("owner", p.getOwner() != null ? text(p.getOwner().getFullName()) : null);
        return new AuditSubject(AuditEntityType.PROJE, p.getId(), p.getId(), p.getName(), v);
    }

    public static AuditSubject of(WeeklyReport r) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("year", text(r.getYear()));
        v.put("weekNumber", text(r.getWeekNumber()));
        v.put("reportDate", text(r.getReportDate()));
        v.put("progressStage", text(r.getProgressStage()));
        v.put("overallStatus", text(r.getOverallStatus()));
        v.put("riskLevel", text(r.getRiskLevel()));
        v.put("done", text(r.getDone()));
        v.put("planned", text(r.getPlanned()));
        v.put("risks", text(r.getRisks()));
        v.put("note", text(r.getNote()));
        return new AuditSubject(AuditEntityType.RAPOR, r.getId(), r.getProject().getId(),
                r.getYear() + " / " + r.getWeekNumber() + ". hafta", v);
    }

    public static AuditSubject of(WorkItem w) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("title", text(w.getTitle()));
        v.put("description", text(w.getDescription()));
        v.put("assignee", text(w.getAssignee()));
        v.put("status", text(w.getStatus()));
        v.put("plannedDate", text(w.getPlannedDate()));
        v.put("completedDate", text(w.getCompletedDate()));
        return new AuditSubject(AuditEntityType.IS_KALEMI, w.getId(), w.getWeeklyReport().getProject().getId(),
                w.getTitle(), v);
    }

    public static AuditSubject of(TrackingItem t) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("type", text(t.getType()));
        v.put("title", text(t.getTitle()));
        v.put("description", text(t.getDescription()));
        v.put("riskLevel", text(t.getRiskLevel()));
        v.put("impact", text(t.getImpact()));
        v.put("actionPlan", text(t.getActionPlan()));
        v.put("assignee", text(t.getAssignee()));
        v.put("followUpDate", text(t.getFollowUpDate()));
        v.put("status", text(t.getStatus()));
        v.put("closedDate", text(t.getClosedDate()));
        return new AuditSubject(AuditEntityType.TAKIP_KAYDI, t.getId(), t.getProject().getId(), t.getTitle(), v);
    }

    /** Ham metin: enum adi, ISO tarih, sayi; bos metin null sayilir (formda silinen alan "bos"a doner). */
    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String s = value instanceof Enum<?> e ? e.name() : value.toString();
        return s.isBlank() ? null : truncate(s, MAX_VALUE_LENGTH);
    }

    static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }
}
