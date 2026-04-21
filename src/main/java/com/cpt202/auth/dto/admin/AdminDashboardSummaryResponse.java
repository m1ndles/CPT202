package com.cpt202.auth.dto.admin;

import java.util.List;

/**
 * Administrator dashboard summary payload.
 *
 * @param attentionCount total attention items shown on the dashboard
 * @param metricCards dashboard metric cards
 * @param contribution contribution insight section
 * @param resource resource insight section
 * @param pendingContributorApplications pending contributor application count
 * @param pendingResourceReviews pending resource review count
 * @param activeCategories active category count
 * @param activeTags active tag count
 * @param latestApplicant latest applicant highlight
 * @param latestResource latest pending resource highlight
 * @param latestTaxonomy latest taxonomy highlight
 * @param archivedResources archived resource count
 * @param latestArchive latest archive highlight
 * @param recentHistory recent admin history items
 */
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

    /**
     * Dashboard metric card payload.
     *
     * @param label metric label
     * @param value metric value
     * @param tone visual tone key
     */
    public record AdminDashboardMetricCard(
            String label,
            String value,
            String tone
    ) {
    }

    /**
     * Dashboard insight section payload.
     *
     * @param title section title
     * @param subtitle section subtitle
     * @param headlineValue headline value
     * @param headlineLabel headline label
     * @param accentLabel accent label
     * @param accentValue accent value
     * @param trend trend chart points
     * @param breakdown category breakdown items
     * @param todoItems follow-up items
     */
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

    /**
     * Dashboard trend point payload.
     *
     * @param label point label
     * @param value point value
     */
    public record AdminDashboardTrendPoint(
            String label,
            int value
    ) {
    }

    /**
     * Dashboard breakdown item payload.
     *
     * @param label item label
     * @param value item value
     * @param color item color
     */
    public record AdminDashboardBreakdownItem(
            String label,
            int value,
            String color
    ) {
    }
}
