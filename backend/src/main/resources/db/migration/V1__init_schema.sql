CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auth_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider, provider_id)
);

CREATE TABLE workflow_definitions (
    workflow_id VARCHAR(100) NOT NULL,
    version INT NOT NULL,
    plan_type VARCHAR(50) NOT NULL,
    active_flag BOOLEAN DEFAULT true,
    priority INT DEFAULT 0,
    input_schema_ref JSONB,
    PRIMARY KEY (workflow_id, version)
);

CREATE TABLE step_definitions (
    step_key VARCHAR(100) NOT NULL,
    workflow_id VARCHAR(100) NOT NULL,
    workflow_version INT NOT NULL,
    dependency_rule JSONB,
    provider_policy_ref VARCHAR(100),
    retry_policy_ref VARCHAR(100),
    fallback_policy_ref VARCHAR(100),
    storage_policy_ref VARCHAR(100),
    PRIMARY KEY (step_key, workflow_id, workflow_version),
    FOREIGN KEY (workflow_id, workflow_version) REFERENCES workflow_definitions(workflow_id, version) ON DELETE CASCADE
);

CREATE TABLE jobs (
    job_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    workflow_id VARCHAR(100) NOT NULL,
    workflow_version INT NOT NULL,
    plan_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    priority INT DEFAULT 0,
    FOREIGN KEY (workflow_id, workflow_version) REFERENCES workflow_definitions(workflow_id, version)
);

CREATE TABLE step_runs (
    step_run_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES jobs(job_id) ON DELETE CASCADE,
    step_key VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempt_no INT DEFAULT 1,
    provider_ref VARCHAR(100),
    model_config_ref VARCHAR(100),
    planned_cost DECIMAL(10, 4),
    actual_cost DECIMAL(10, 4),
    latency_ms BIGINT,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE step_run_usage (
    step_run_id UUID PRIMARY KEY REFERENCES step_runs(step_run_id) ON DELETE CASCADE,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    image_count INT,
    audio_seconds DECIMAL(10, 2),
    video_seconds DECIMAL(10, 2),
    raw_usage_json JSONB
);

CREATE TABLE step_queue (
    queue_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES jobs(job_id) ON DELETE CASCADE,
    step_key VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    claimed_by VARCHAR(255),
    claimed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE provider_configs (
    provider_key VARCHAR(50) PRIMARY KEY,
    endpoint_ref VARCHAR(500) NOT NULL,
    auth_ref VARCHAR(255),
    timeout_ms BIGINT DEFAULT 30000,
    enabled_flag BOOLEAN DEFAULT true
);

CREATE TABLE model_configs (
    model_config_key VARCHAR(100) PRIMARY KEY,
    provider_key VARCHAR(50) NOT NULL REFERENCES provider_configs(provider_key),
    external_model_name VARCHAR(255) NOT NULL,
    model_type VARCHAR(50),
    params_json JSONB,
    active_flag BOOLEAN DEFAULT true
);

CREATE TABLE pricing_catalog (
    pricing_id SERIAL PRIMARY KEY,
    provider_key VARCHAR(50) NOT NULL,
    model_config_key VARCHAR(100) NOT NULL,
    unit_type VARCHAR(50),
    rates_json JSONB,
    effective_from TIMESTAMP WITH TIME ZONE,
    effective_to TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_step_queue_ready ON step_queue (status, next_attempt_at) WHERE status = 'READY';
