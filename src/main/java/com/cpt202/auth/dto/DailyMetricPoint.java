package com.cpt202.auth.dto;

public record DailyMetricPoint(
        String date,
        String label,
        int count
) {
}
