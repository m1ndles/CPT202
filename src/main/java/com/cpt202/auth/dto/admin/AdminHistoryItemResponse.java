package com.cpt202.auth.dto.admin;

/**
 * Admin history item payload.
 *
 * @param actionType action type
 * @param targetType target type
 * @param targetName target name
 * @param operator operator name
 * @param createdAt action time
 * @param details action details
 */
public record AdminHistoryItemResponse(
        String actionType,
        String targetType,
        String targetName,
        String operator,
        String createdAt,
        String details
) {
}
