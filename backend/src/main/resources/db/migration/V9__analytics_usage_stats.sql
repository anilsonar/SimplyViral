-- V9: Analytics and usage tracking

CREATE TABLE user_usage_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    period VARCHAR(20) NOT NULL, -- e.g., '2026-04'
    total_jobs INT DEFAULT 0,
    total_cost DECIMAL(12, 4) DEFAULT 0,
    total_tokens_used BIGINT DEFAULT 0,
    total_images_generated INT DEFAULT 0,
    total_audio_seconds DECIMAL(10, 2) DEFAULT 0,
    total_videos_rendered INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, period)
);

CREATE INDEX idx_user_usage_stats_user_period ON user_usage_stats (user_id, period);
