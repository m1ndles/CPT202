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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int STANDARD_SESSION_SECONDS = 60 * 30;
    private static final int REMEMBER_ME_SESSION_SECONDS = 60 * 60 * 24 * 30;
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

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

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.email(), request.username(), request.password());
        return new LoginResponse("Account created successfully. Please log in.", "/login.html?registered=1", null, UserRole.USER.name());
    }

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

    @GetMapping("/me")
    public SessionUserResponse currentUser(HttpSession session) {
        Object roleValue = session.getAttribute("role");
        if (roleValue == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }

        Object userIdValue = session.getAttribute("userId");
        Long userId = userIdValue instanceof Long ? (Long) userIdValue : null;
        UserRole sessionRole = UserRole.fromDatabaseValue(String.valueOf(roleValue));
        return authService.getSessionUser(userId, sessionRole);
    }

    @PostMapping("/logout")
    public LoginResponse logout(HttpSession session, HttpServletResponse response) {
        session.invalidate();
        clearSessionCookie(response);
        return new LoginResponse("Logged out successfully.", "/login.html", null, null);
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }
    }

    private void writeSessionCookie(HttpSession session, boolean rememberMe, HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, session.getId());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(rememberMe ? REMEMBER_ME_SESSION_SECONDS : -1);
        response.addCookie(cookie);
    }

    private void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
