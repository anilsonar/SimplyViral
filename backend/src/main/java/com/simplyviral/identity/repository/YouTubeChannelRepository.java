package com.simplyviral.identity.repository;

import com.simplyviral.identity.entity.YouTubeChannelInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface YouTubeChannelRepository extends JpaRepository<YouTubeChannelInfo, UUID> {
    List<YouTubeChannelInfo> findByUserIdAndLinkedTrue(UUID userId);
    Optional<YouTubeChannelInfo> findByChannelId(String channelId);
}
