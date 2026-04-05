package com.simplyviral.identity.controller;

import com.simplyviral.identity.entity.User;
import com.simplyviral.identity.entity.YouTubeChannelInfo;
import com.simplyviral.identity.service.AuthService;
import com.simplyviral.identity.service.YouTubeService;
import com.simplyviral.shared.dto.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final YouTubeService youTubeService;

    @GetMapping("/status")
    public ApiResult<String> getAuthStatus() {
        return ApiResult.success("Authentication system is up. Supports: Email+Password, OTP (Email/Mobile via Twilio), Google OAuth, YouTube channel linking.");
    }

    // ========================= Email + Password =========================

    @PostMapping("/register")
    public ApiResult<Map<String, String>> register(@RequestBody RegisterReq req) {
        Map<String, String> tokens = authService.register(req.getEmail(), req.getPassword(), req.getMobileNumber());
        return ApiResult.success(tokens);
    }

    @PostMapping("/login")
    public ApiResult<Map<String, String>> login(@RequestBody LoginReq req) {
        Map<String, String> tokens = authService.login(req.getEmail(), req.getPassword());
        return ApiResult.success(tokens);
    }

    // ========================= OTP (Email / Mobile) =========================

    @PostMapping("/otp/send")
    public ApiResult<String> sendOtp(@RequestBody OtpSendReq req) {
        authService.sendOtp(req.getIdentifier(), req.getType());
        return ApiResult.success("OTP sent successfully");
    }

    @PostMapping("/otp/verify")
    public ApiResult<Map<String, String>> verifyOtp(@RequestBody OtpVerifyReq req) {
        Map<String, String> tokens = authService.verifyOtp(req.getIdentifier(), req.getType(), req.getCode());
        return ApiResult.success(tokens);
    }

    // ========================= Refresh Token =========================

    @PostMapping("/refresh")
    public ApiResult<Map<String, String>> refreshToken(@RequestBody RefreshReq req) {
        Map<String, String> tokens = authService.refreshAccessToken(req.getRefreshToken());
        return ApiResult.success(tokens);
    }

    // ========================= User Profile =========================

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ApiResult.error("Not authenticated", "UNAUTHENTICATED");
        }

        String subject = authentication.getPrincipal().toString();
        User user;
        try {
            // Try parsing as UUID first (new token format)
            UUID userId = UUID.fromString(subject);
            user = authService.getUserById(userId);
        } catch (IllegalArgumentException e) {
            // Fall back to email (legacy token format)
            user = authService.getUserByEmail(subject);
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("user_id", user.getId().toString());
        profile.put("email", user.getEmail());
        profile.put("mobile_number", user.getMobileNumber());
        profile.put("is_email_verified", user.getIsEmailVerified());
        profile.put("is_mobile_verified", user.getIsMobileVerified());
        profile.put("created_at", user.getCreatedAt());

        // Include linked YouTube channels
        List<Map<String, Object>> channels = youTubeService.getLinkedChannels(user.getId()).stream()
                .map(ch -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("channel_id", ch.getChannelId());
                    m.put("channel_title", ch.getChannelTitle());
                    m.put("channel_url", ch.getChannelUrl());
                    m.put("subscriber_count", ch.getSubscriberCount());
                    m.put("video_count", ch.getVideoCount());
                    m.put("thumbnail_url", ch.getThumbnailUrl());
                    return m;
                })
                .collect(Collectors.toList());
        profile.put("youtube_channels", channels);

        return ApiResult.success(profile);
    }

    // ========================= YouTube Channel Management =========================

    @DeleteMapping("/youtube/{channelId}")
    public ApiResult<String> unlinkYouTubeChannel(
            @PathVariable String channelId,
            Authentication authentication) {
        if (authentication == null) {
            return ApiResult.error("Not authenticated", "UNAUTHENTICATED");
        }
        String subject = authentication.getPrincipal().toString();
        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            User user = authService.getUserByEmail(subject);
            userId = user.getId();
        }

        youTubeService.unlinkChannel(userId, channelId);
        return ApiResult.success("YouTube channel unlinked successfully");
    }

    // ========================= Request DTOs =========================

    @Data
    public static class RegisterReq {
        private String email;
        private String password;
        private String mobileNumber;
    }

    @Data
    public static class LoginReq {
        private String email;
        private String password;
    }

    @Data
    public static class OtpSendReq {
        private String identifier; // email or mobile number
        private String type; // EMAIL or MOBILE
    }

    @Data
    public static class OtpVerifyReq {
        private String identifier;
        private String type;
        private String code;
    }

    @Data
    public static class RefreshReq {
        private String refreshToken;
    }
}
