package com.cpt202.auth.dto;

import java.util.List;

/**
 * Contributor engagement summary shown on the profile page.
 *
 * @param totalReceivedLikes total favorites received by the contributor's resources
 * @param dailyReceivedLikes daily favorite counts used by the profile trend chart
 */
public record ContributorEngagementResponse(
        int totalReceivedLikes,
        List<DailyMetricPoint> dailyReceivedLikes
) {
}
