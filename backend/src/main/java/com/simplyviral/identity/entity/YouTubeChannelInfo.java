package com.simplyviral.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Stores linked YouTube channel information for a user.
 * Populated after the user links their Google account with YouTube scopes.
 */
@Entity
@Table(name = "youtube_channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouTubeChannelInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "channel_id", nullable = false, unique = true)
    private String channelId;

    @Column(name = "channel_title")
    private String channelTitle;

    @Column(name = "channel_url")
    private String channelUrl;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "video_count")
    private Long videoCount;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "linked")
    @Builder.Default
    private Boolean linked = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
