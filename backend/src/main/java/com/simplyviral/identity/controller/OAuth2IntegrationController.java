package com.simplyviral.identity.controller;

import com.simplyviral.identity.entity.AuthIdentity;
import com.simplyviral.identity.entity.User;
import com.simplyviral.identity.entity.YouTubeChannelInfo;
import com.simplyviral.identity.repository.AuthIdentityRepository;
import com.simplyviral.identity.repository.UserRepository;
import com.simplyviral.identity.configuration.JwtTokenProvider;
import com.simplyviral.identity.service.YouTubeService;
import com.simplyviral.shared.dto.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles OAuth2 login success callbacks from Google.
 * Performs two functions:
 * 1. User authentication (creates/finds user, issues JWT)
 * 2. YouTube channel linking (fetches channel info via YouTube Data API)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
public class OAuth2IntegrationController {

    private final AuthIdentityRepository authIdentityRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final YouTubeService youTubeService;

    /**
     * Called after successful Google OAuth2 login.
     * Creates or updates the user, stores OAuth tokens for YouTube API access,
     * and attempts to link the user's YouTube channel.
     */
    @GetMapping("/success")
    public ApiResult<Map<String, Object>> handleOAuth2Success(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {

        String email = oauth2User.getAttribute("email");
        String providerId = oauth2User.getName();

        if (email == null || email.isBlank()) {
            log.error("OAuth2 login failed: no email attribute returned by Google");
            return ApiResult.error("OAuth2 login failed: email not provided", "OAUTH_NO_EMAIL");
        }

        // 1. Find or create user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder().email(email).isEmailVerified(true).build();
            return userRepository.save(newUser);
        });
        user.setIsEmailVerified(true);
        userRepository.save(user);

        // 2. Store/update OAuth tokens for background YouTube API usage
        Optional<AuthIdentity> existingIdentity = authIdentityRepository.findByProviderAndProviderId("google", providerId);

        AuthIdentity identity = existingIdentity.orElseGet(() -> AuthIdentity.builder()
                .user(user)
                .provider("google")
                .providerId(providerId)
                .build());

        identity.setAccessToken(client.getAccessToken().getTokenValue());
        if (client.getRefreshToken() != null) {
            identity.setRefreshToken(client.getRefreshToken().getTokenValue());
        }
        if (client.getAccessToken().getExpiresAt() != null) {
            identity.setExpiresAt(client.getAccessToken().getExpiresAt().atOffset(ZoneOffset.UTC));
        }
        authIdentityRepository.save(identity);

        // 3. Generate our JWT system token
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        // 4. Attempt YouTube channel linking (non-blocking — failures don't break login)
        YouTubeChannelInfo channelInfo = null;
        try {
            channelInfo = youTubeService.linkChannelFromOAuth(user, client.getAccessToken().getTokenValue());
        } catch (Exception e) {
            log.warn("YouTube channel linking failed (non-fatal): {}", e.getMessage());
        }

        // 5. Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", accessToken);
        response.put("user_id", user.getId().toString());
        response.put("email", user.getEmail());
        response.put("oauth_provider", "google");

        if (channelInfo != null) {
            Map<String, Object> ytInfo = new LinkedHashMap<>();
            ytInfo.put("channel_id", channelInfo.getChannelId());
            ytInfo.put("channel_title", channelInfo.getChannelTitle());
            ytInfo.put("subscriber_count", channelInfo.getSubscriberCount());
            response.put("youtube_channel", ytInfo);
        }

        log.info("OAuth2 login successful for email: {}. YouTube linked: {}", email, channelInfo != null);
        return ApiResult.success(response);
    }
}
