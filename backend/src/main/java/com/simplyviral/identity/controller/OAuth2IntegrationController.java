package com.simplyviral.identity.controller;

import com.simplyviral.identity.entity.AuthIdentity;
import com.simplyviral.identity.entity.User;
import com.simplyviral.identity.repository.AuthIdentityRepository;
import com.simplyviral.identity.repository.UserRepository;
import com.simplyviral.identity.configuration.JwtTokenProvider;
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
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
public class OAuth2IntegrationController {

    private final AuthIdentityRepository authIdentityRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/success")
    public ApiResult<String> handleOAuth2Success(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {

        String email = oauth2User.getAttribute("email");
        String providerId = oauth2User.getName();

        // Check if user exists
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder().email(email).isEmailVerified(true).build();
            return userRepository.save(newUser);
        });

        // Store tokens for background Youtube usage
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
            identity.setExpiresAt(OffsetDateTime.parse(client.getAccessToken().getExpiresAt().toString()));
        }

        authIdentityRepository.save(identity);

        // Generate our own JWT system token
        String token = jwtTokenProvider.generateToken(user.getEmail());
        log.info("OAuth2 login/link successful for email: {}", email);

        // In a real app this might redirect to frontend with token in URL / cookie
        return ApiResult.success("OAuth successful. Token: " + token);
    }
}
