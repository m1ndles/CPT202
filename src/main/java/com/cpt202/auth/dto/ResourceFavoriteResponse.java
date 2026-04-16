package com.cpt202.auth.dto;

/**
 * Favorite toggle result.
 */
public record ResourceFavoriteResponse(
        String message,
        boolean favoritedByMe,
        int favoriteCount
) {
}
