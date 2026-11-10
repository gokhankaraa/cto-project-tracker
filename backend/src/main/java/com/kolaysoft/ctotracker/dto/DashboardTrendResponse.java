package com.kolaysoft.ctotracker.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** GET /api/dashboard/trend yaniti: eskiden yeniye haftalar. */
@Schema(description = "Portföyün haftalık trendi")
public record DashboardTrendResponse(

        @Schema(description = "Haftalar, eskiden yeniye; son eleman referans hafta")
        List<TrendWeek> weeks) {
}
