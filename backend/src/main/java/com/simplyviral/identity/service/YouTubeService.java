package com.simplyviral.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplyviral.identity.entity.AuthIdentity;
import com.simplyviral.identity.entity.User;
import com.simplyviral.identity.entity.YouTubeChannelInfo;
import com.simplyviral.identity.repository.AuthIdentityRepository;
import com.simplyviral.identity.repository.YouTubeChannelRepository;
import com.simplyviral.shared.exception.SimplyViralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

/**
 * Handles YouTube channel linking/unlinking via Google OAuth tokens.
 * Uses YouTube Data API v3 to fetch channel info (channel ID, title, subscriber count).
 * The Google OAuth token stored in AuthIdentity provides access.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeService {

    private final YouTubeChannelRepository channelRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private static final String YOUTUBE_API_BASE = "https://www.googleapis.com/youtube/v3";

    /**
     * After Google OAuth completes, fetch the user's YouTube channel info
     * and persist it. Uses the access token stored in the AuthIdentity.
     */
    @Transactional
    public YouTubeChannelInfo linkChannelFromOAuth(User user, String accessToken) {
        log.info("Attempting to link YouTube channel for user {}", user.getId());

        try {
            // Call YouTube Data API to get "mine" channel
            String responseBody = webClientBuilder.build()
                    .get()
                    .uri(YOUTUBE_API_BASE + "/channels?part=snippet,statistics&mine=true")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody == null) {
                log.warn("Empty response from YouTube Data API for user {}", user.getId());
                return null;
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = root.path("items");

            if (!items.isArray() || items.isEmpty()) {
                log.warn("No YouTube channels found for user {}", user.getId());
                return null;
            }

            JsonNode channel = items.get(0);
            String channelId = channel.path("id").asText();
            JsonNode snippet = channel.path("snippet");
            JsonNode statistics = channel.path("statistics");

            // Check if channel already linked
            YouTubeChannelInfo existing = channelRepository.findByChannelId(channelId).orElse(null);
            if (existing != null) {
                // Update existing record
                existing.setUser(user);
                existing.setChannelTitle(snippet.path("title").asText(null));
                existing.setChannelUrl("https://www.youtube.com/channel/" + channelId);
                existing.setSubscriberCount(statistics.path("subscriberCount").asLong(0));
                existing.setVideoCount(statistics.path("videoCount").asLong(0));
                existing.setThumbnailUrl(snippet.path("thumbnails").path("default").path("url").asText(null));
                existing.setLinked(true);
                channelRepository.save(existing);
                log.info("Updated existing YouTube channel link: channelId={}, title={}",
                        channelId, existing.getChannelTitle());
                return existing;
            }

            // Create new channel link
            YouTubeChannelInfo channelInfo = YouTubeChannelInfo.builder()
                    .user(user)
                    .channelId(channelId)
                    .channelTitle(snippet.path("title").asText(null))
                    .channelUrl("https://www.youtube.com/channel/" + channelId)
                    .subscriberCount(statistics.path("subscriberCount").asLong(0))
                    .videoCount(statistics.path("videoCount").asLong(0))
                    .thumbnailUrl(snippet.path("thumbnails").path("default").path("url").asText(null))
                    .build();

            channelRepository.save(channelInfo);
            log.info("YouTube channel linked: channelId={}, title={} for user={}",
                    channelId, channelInfo.getChannelTitle(), user.getId());

            return channelInfo;

        } catch (Exception e) {
            log.error("Failed to fetch YouTube channel info for user {}", user.getId(), e);
            throw new SimplyViralException("Failed to link YouTube channel: " + e.getMessage(), e);
        }
    }

    /**
     * Unlinks a YouTube channel from a user.
     */
    @Transactional
    public void unlinkChannel(UUID userId, String channelId) {
        channelRepository.findByChannelId(channelId).ifPresent(channel -> {
            if (channel.getUser().getId().equals(userId)) {
                channel.setLinked(false);
                channelRepository.save(channel);
                log.info("YouTube channel unlinked: channelId={} for user={}", channelId, userId);
            }
        });
    }

    /**
     * Returns all linked YouTube channels for a user.
     */
    @Transactional(readOnly = true)
    public List<YouTubeChannelInfo> getLinkedChannels(UUID userId) {
        return channelRepository.findByUserIdAndLinkedTrue(userId);
    }
}
