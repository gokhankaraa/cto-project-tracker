package com.kolaysoft.ctotracker.dto.filter;

import java.util.List;

import com.kolaysoft.ctotracker.entity.ProjectStatus;

/**
 * Haftalik trend filtresi (filtre sozlesmesi, bolum 2a). Proje filtreleri dashboard'dakilerle aynidir; rapora
 * dayali filtreler (genel durum, risk, gecikme) yoktur, cunku trend bunlarin haftadan haftaya dagilimini gosterir.
 *
 * <p>Pencere {@code weeks} haftadir ve referans haftada biter: {@code year} + {@code weekNumber} verilirse o hafta,
 * yalnizca {@code year} verilirse o yilin son haftasi, hicbiri yoksa bu hafta.
 */
public record TrendFilter(
        Long projectId,
        List<ProjectStatus> projectStatuses,
        Long ownerId,
        Integer year,
        Integer weekNumber,
        int weeks) {

    public TrendFilter {
        FilterValues.requireYear(year, weekNumber, "weekNumber");
        projectStatuses = FilterValues.values(projectStatuses);
    }
}
