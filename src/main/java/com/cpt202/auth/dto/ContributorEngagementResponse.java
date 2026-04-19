package com.cpt202.auth.dto;

import java.util.List;

public record ContributorEngagementResponse(
        int totalReceivedLikes,
        List<DailyMetricPoint> dailyReceivedLikes
) {
}
