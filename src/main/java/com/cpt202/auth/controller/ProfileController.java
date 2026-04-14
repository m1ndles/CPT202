package com.cpt202.auth.controller;

import com.cpt202.auth.dto.SessionUserResponse;
import com.cpt202.auth.dto.UpdateEmailRequest;
import com.cpt202.auth.dto.UpdatePasswordRequest;
import com.cpt202.auth.dto.UpdateProfileRequest;
import com.cpt202.auth.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public SessionUserResponse getProfile(HttpSession session) {
        return profileService.getProfile(currentUserId(session));
    }

    @PutMapping
    public SessionUserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request, HttpSession session) {
        return profileService.updateProfile(currentUserId(session), request);
    }

    @PostMapping("/avatar")
    public SessionUserResponse uploadAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
        return profileService.uploadAvatar(currentUserId(session), file);
    }

    @PutMapping("/password")
    public Map<String, String> updatePassword(@Valid @RequestBody UpdatePasswordRequest request, HttpSession session) {
        profileService.updatePassword(currentUserId(session), request);
        return Map.of("message", "Password updated successfully.");
    }

    @PutMapping("/email")
    public SessionUserResponse updateEmail(@Valid @RequestBody UpdateEmailRequest request, HttpSession session) {
        return profileService.updateEmail(currentUserId(session), request);
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        return userId instanceof Long ? (Long) userId : null;
    }
}
