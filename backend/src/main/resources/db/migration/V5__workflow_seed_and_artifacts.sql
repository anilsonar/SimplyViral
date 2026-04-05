-- ============================================================
-- V5: Job Artifacts table, StepRun error fields, policy columns,
--     and seeding workflow + step definitions
-- ============================================================

-- 1. Job artifacts — inter-step data passing
CREATE TABLE job_artifacts (
    artifact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jobs(job_id) ON DELETE CASCADE,
    step_key VARCHAR(100) NOT NULL,
    artifact_key VARCHAR(100) NOT NULL,
    artifact_value TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(job_id, artifact_key)
);
CREATE INDEX idx_job_artifacts_job ON job_artifacts (job_id);

-- 2. StepRun error tracking
ALTER TABLE step_runs ADD COLUMN IF NOT EXISTS error_code VARCHAR(100);
ALTER TABLE step_runs ADD COLUMN IF NOT EXISTS error_message TEXT;

-- 3. Convert step_definitions policy columns from VARCHAR to JSONB
ALTER TABLE step_definitions ALTER COLUMN provider_policy_ref TYPE JSONB USING provider_policy_ref::jsonb;
ALTER TABLE step_definitions ALTER COLUMN retry_policy_ref TYPE JSONB USING retry_policy_ref::jsonb;
ALTER TABLE step_definitions ALTER COLUMN fallback_policy_ref TYPE JSONB USING fallback_policy_ref::jsonb;
ALTER TABLE step_definitions ALTER COLUMN storage_policy_ref TYPE JSONB USING storage_policy_ref::jsonb;

-- 4. Seed the baseline workflow definition for FREE plan
INSERT INTO workflow_definitions (workflow_id, version, plan_type, active_flag, priority, input_schema_ref)
VALUES ('viral-shorts-v1', 1, 'FREE', true, 0, '{"input": "topics_json"}')
ON CONFLICT DO NOTHING;

-- 5. Seed all 9 step definitions with dependency rules, provider/model config, retry, fallback
INSERT INTO step_definitions (step_key, workflow_id, workflow_version, dependency_rule, provider_policy_ref, retry_policy_ref, fallback_policy_ref, storage_policy_ref) VALUES
(
    'TOPIC_FETCH', 'viral-shorts-v1', 1,
    '{"depends_on": []}',
    '{"provider_key": "internal", "model_config_key": null}',
    '{"max_attempts": 2, "initial_delay_ms": 1000, "backoff_multiplier": 1.0, "max_delay_ms": 5000}',
    NULL,
    '{"persist": false}'
),
(
    'TOPIC_SELECTION', 'viral-shorts-v1', 1,
    '{"depends_on": ["TOPIC_FETCH"]}',
    '{"provider_key": "openai", "model_config_key": "gpt-4o-baseline"}',
    '{"max_attempts": 3, "initial_delay_ms": 2000, "backoff_multiplier": 2.0, "max_delay_ms": 30000}',
    '{"fallback_model_config_key": "gpt-4-turbo-baseline"}',
    '{"persist": true}'
),
(
    'SCRIPT_GENERATION', 'viral-shorts-v1', 1,
    '{"depends_on": ["TOPIC_SELECTION"]}',
    '{"provider_key": "openai", "model_config_key": "gpt-4o-baseline"}',
    '{"max_attempts": 3, "initial_delay_ms": 2000, "backoff_multiplier": 2.0, "max_delay_ms": 30000}',
    '{"fallback_model_config_key": "gpt-4-turbo-baseline"}',
    '{"persist": true}'
),
(
    'SCRIPT_BREAKDOWN', 'viral-shorts-v1', 1,
    '{"depends_on": ["SCRIPT_GENERATION"]}',
    '{"provider_key": "openai", "model_config_key": "gpt-4o-baseline"}',
    '{"max_attempts": 3, "initial_delay_ms": 2000, "backoff_multiplier": 2.0, "max_delay_ms": 30000}',
    '{"fallback_model_config_key": "gpt-4-turbo-baseline"}',
    '{"persist": true}'
),
(
    'IMAGE_PROMPT_GENERATION', 'viral-shorts-v1', 1,
    '{"depends_on": ["SCRIPT_BREAKDOWN"]}',
    '{"provider_key": "openai", "model_config_key": "gpt-4o-baseline"}',
    '{"max_attempts": 3, "initial_delay_ms": 2000, "backoff_multiplier": 2.0, "max_delay_ms": 30000}',
    '{"fallback_model_config_key": "gpt-4-turbo-baseline"}',
    '{"persist": true}'
),
(
    'IMAGE_GENERATION', 'viral-shorts-v1', 1,
    '{"depends_on": ["IMAGE_PROMPT_GENERATION"]}',
    '{"provider_key": "falai", "model_config_key": "flux-schnell"}',
    '{"max_attempts": 3, "initial_delay_ms": 3000, "backoff_multiplier": 2.0, "max_delay_ms": 60000}',
    NULL,
    '{"persist": true}'
),
(
    'AUDIO_GENERATION', 'viral-shorts-v1', 1,
    '{"depends_on": ["SCRIPT_GENERATION"]}',
    '{"provider_key": "elevenlabs", "model_config_key": "eleven_multilingual_v2"}',
    '{"max_attempts": 3, "initial_delay_ms": 3000, "backoff_multiplier": 2.0, "max_delay_ms": 60000}',
    NULL,
    '{"persist": true}'
),
(
    'VIDEO_COMPOSITION', 'viral-shorts-v1', 1,
    '{"depends_on": ["IMAGE_GENERATION", "AUDIO_GENERATION"]}',
    '{"provider_key": "creatomate", "model_config_key": "creatomate_api_v1"}',
    '{"max_attempts": 2, "initial_delay_ms": 5000, "backoff_multiplier": 2.0, "max_delay_ms": 60000}',
    NULL,
    '{"persist": true}'
),
(
    'FINAL_EDIT_RENDER', 'viral-shorts-v1', 1,
    '{"depends_on": ["VIDEO_COMPOSITION"]}',
    '{"provider_key": "internal", "model_config_key": null}',
    '{"max_attempts": 2, "initial_delay_ms": 1000, "backoff_multiplier": 1.0, "max_delay_ms": 5000}',
    NULL,
    '{"persist": true}'
)
ON CONFLICT DO NOTHING;

-- 6. Seed pricing catalog for cost computation
INSERT INTO pricing_catalog (provider_key, model_config_key, unit_type, rates_json, effective_from) VALUES
('openai', 'gpt-4o-baseline', 'tokens', '{"input_per_1k": 0.0025, "output_per_1k": 0.01}', NOW()),
('openai', 'gpt-4-turbo-baseline', 'tokens', '{"input_per_1k": 0.01, "output_per_1k": 0.03}', NOW()),
('elevenlabs', 'eleven_multilingual_v2', 'characters', '{"per_1k_chars": 0.30}', NOW()),
('falai', 'flux-schnell', 'images', '{"per_image": 0.003}', NOW()),
('creatomate', 'creatomate_api_v1', 'renders', '{"per_render": 0.50}', NOW())
ON CONFLICT DO NOTHING;
