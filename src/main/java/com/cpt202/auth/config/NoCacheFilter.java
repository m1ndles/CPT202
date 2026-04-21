package com.cpt202.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Disables browser caching for dynamic pages and APIs.
 */
@Component
public class NoCacheFilter extends OncePerRequestFilter {

    /**
     * Applies no-cache headers to matching requests before continuing the chain.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (shouldDisableCaching(request.getRequestURI())) {
            response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determines whether the request URI should bypass browser caching.
     */
    private boolean shouldDisableCaching(String uri) {
        return uri.startsWith("/api/")
                || uri.endsWith("/home.html")
                || uri.startsWith("/admin/")
                || uri.endsWith("/index.html")
                || uri.endsWith("/detail.html")
                || uri.endsWith("/login.html")
                || uri.endsWith("/register.html")
                || uri.endsWith("/profile.html")
                || uri.endsWith("/my-favorites.html")
                || uri.endsWith("/my-resources.html")
                || uri.endsWith("/my-resource-detail.html")
                || uri.endsWith("/applicant.html");
    }
}
