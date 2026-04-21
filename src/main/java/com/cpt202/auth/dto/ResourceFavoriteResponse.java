package com.cpt202.auth.dto;

/**
 * Favorite toggle result.
 *
 * @param message result message
 * @param favoritedByMe whether the viewer now favorites the resource
 * @param favoriteCount updated favorite count
 */
public record ResourceFavoriteResponse(
        String message,
        boolean favoritedByMe,
        int favoriteCount
) {
}
