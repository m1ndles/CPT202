package com.cpt202.auth.dto;

/**
 * Single point in a small daily metric series.
 *
 * @param date ISO date represented by the point
 * @param label display label used by the frontend chart
 * @param count metric count for the date
 */
public record DailyMetricPoint(
        String date,
        String label,
        int count
) {
}
