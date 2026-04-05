-- Seed Video Composition Provider Into Registry
INSERT INTO provider_configs (provider_key, endpoint_ref, auth_ref, timeout_ms, enabled_flag) 
VALUES ('creatomate', 'https://api.creatomate.com/v1/renders', 'simplyviral.provider.creatomate.api-key', 300000, true);

-- Seed Explicit Video Model Limits
INSERT INTO model_configs (model_config_key, provider_key, external_model_name, model_type, active_flag) 
VALUES ('creatomate_api_v1', 'creatomate', 'v1', 'video-composition', true);
