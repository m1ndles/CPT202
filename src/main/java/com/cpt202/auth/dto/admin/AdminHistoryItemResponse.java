package com.cpt202.auth.dto.admin;

public record AdminHistoryItemResponse(
        String actionType,
        String targetType,
        String targetName,
        String operator,
        String createdAt,
        String details
) {
}
