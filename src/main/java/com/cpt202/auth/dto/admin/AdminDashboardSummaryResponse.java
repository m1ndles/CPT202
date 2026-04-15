package com.cpt202.auth.dto.admin;

import java.util.List;

public record AdminDashboardSummaryResponse(
        int attentionCount,
        List<AdminDashboardMetricCard> metricCards,
        AdminDashboardInsightSection contribution,
        AdminDashboardInsightSection resource,
        int pendingContributorApplications,
        int pendingResourceReviews,
        int activeCategories,
        int activeTags,
        String latestApplicant,
        String latestResource,
        String latestTaxonomy,
        int archivedResources,
        String latestArchive,
        List<AdminHistoryItemResponse> recentHistory
) {

    public record AdminDashboardMetricCard(
            String label,
            String value,
            String tone
    ) {
    }

    public record AdminDashboardInsightSection(
            String title,
            String subtitle,
            String headlineValue,
            String headlineLabel,
            String accentLabel,
            String accentValue,
            List<AdminDashboardTrendPoint> trend,
            List<AdminDashboardBreakdownItem> breakdown,
            List<String> todoItems
    ) {
    }

    public record AdminDashboardTrendPoint(
            String label,
            int value
    ) {
    }

    public record AdminDashboardBreakdownItem(
            String label,
            int value,
            String color
    ) {
    }
}
