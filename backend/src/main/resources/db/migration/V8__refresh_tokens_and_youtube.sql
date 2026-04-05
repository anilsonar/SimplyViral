-- V8: Refresh tokens for JWT rotation and YouTube channel storage

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token) WHERE revoked = false;
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE youtube_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    channel_id VARCHAR(255) NOT NULL UNIQUE,
    channel_title VARCHAR(500),
    channel_url VARCHAR(500),
    subscriber_count BIGINT DEFAULT 0,
    video_count BIGINT DEFAULT 0,
    thumbnail_url VARCHAR(500),
    linked BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_youtube_channels_user ON youtube_channels (user_id) WHERE linked = true;
