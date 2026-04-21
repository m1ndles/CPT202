package com.cpt202.auth.controller;

import com.cpt202.auth.dto.LoginRequest;
import com.cpt202.auth.dto.LoginResponse;
import com.cpt202.auth.dto.RegisterRequest;
import com.cpt202.auth.dto.SessionUserResponse;
import com.cpt202.auth.exception.AuthException;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication and session management APIs.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * Standard session timeout in seconds.
     */
    private static final int STANDARD_SESSION_SECONDS = 60 * 30;

    /**
     * Extended session timeout for remember-me sign-ins.
     */
    private static final int REMEMBER_ME_SESSION_SECONDS = 60 * 60 * 24 * 30;

    /**
     * Name of the session cookie.
     */
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";

    /**
     * Authentication business service.
     */
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Signs a user in and creates a new session.
     */
    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthService.AuthenticatedUser user = authService.login(request.email(), request.password());

        invalidateSession(httpRequest);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("userId", user.id());
        session.setAttribute("username", user.username());
        session.setAttribute("email", user.email());
        session.setAttribute("role", user.role().name());
        session.setMaxInactiveInterval(request.rememberMe() ? REMEMBER_ME_SESSION_SECONDS : STANDARD_SESSION_SECONDS);
        writeSessionCookie(session, request.rememberMe(), httpResponse);

        return new LoginResponse("Login successful.", user.role().dashboardPath(), user.username(), user.role().name());
    }

    /**
     * Registers a new account and signs the user in immediately.
     */
    @PostMapping("/register")
    public LoginResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthService.AuthenticatedUser user = authService.register(request.email(), request.username(), request.password());

        invalidateSession(httpRequest);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("userId", user.id());
        session.setAttribute("username", user.username());
        session.setAttribute("email", user.email());
        session.setAttribute("role", user.role().name());
        session.setMaxInactiveInterval(STANDARD_SESSION_SECONDS);
        writeSessionCookie(session, false, httpResponse);

        return new LoginResponse("Account created successfully.", user.role().dashboardPath(), user.username(), user.role().name());
    }

    /**
     * Creates a temporary guest session.
     */
    @PostMapping("/guest")
    public LoginResponse continueAsGuest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        invalidateSession(httpRequest);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("role", UserRole.GUEST.name());
        session.setAttribute("username", "Guest Visitor");
        session.setAttribute("email", "Guest session");
        session.setMaxInactiveInterval(STANDARD_SESSION_SECONDS);
        writeSessionCookie(session, false, httpResponse);

        return new LoginResponse("Guest access enabled.", UserRole.GUEST.dashboardPath(), "Guest Visitor", UserRole.GUEST.name());
    }

    /**
     * Returns the current session user profile.
     */
    @GetMapping("/me")
    public SessionUserResponse currentUser(HttpSession session) {
        Object roleValue = session.getAttribute("role");
        if (roleValue == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }

        Object userIdValue = session.getAttribute("userId");
        Long userId = userIdValue instanceof Long ? (Long) userIdValue : null;
        UserRole sessionRole = UserRole.fromDatabaseValue(String.valueOf(roleValue));
        SessionUserResponse user = authService.getSessionUser(userId, sessionRole);
        session.setAttribute("username", user.username());
        session.setAttribute("email", user.email());
        session.setAttribute("role", user.role());
        return user;
    }

    /**
     * Logs the current session out and clears the cookie.
     */
    @PostMapping("/logout")
    public LoginResponse logout(HttpSession session, HttpServletResponse response) {
        session.invalidate();
        clearSessionCookie(response);
        return new LoginResponse("Logged out successfully.", "/login.html", null, null);
    }

    /**
     * Invalidates any existing session before creating a new one.
     */
    private void invalidateSession(HttpServletRequest request) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }
    }

    /**
     * Writes the session cookie using the requested lifetime.
     */
    private void writeSessionCookie(HttpSession session, boolean rememberMe, HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, session.getId());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(rememberMe ? REMEMBER_ME_SESSION_SECONDS : -1);
        response.addCookie(cookie);
    }

    /**
     * Removes the session cookie from the browser.
     */
    private void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
