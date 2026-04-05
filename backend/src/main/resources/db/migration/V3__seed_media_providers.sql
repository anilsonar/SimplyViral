-- Seed Media Providers Into Registry
INSERT INTO provider_configs (provider_key, endpoint_ref, auth_ref, timeout_ms, enabled_flag) 
VALUES ('elevenlabs', 'https://api.elevenlabs.io/v1/text-to-speech', 'simplyviral.provider.elevenlabs.api-key', 120000, true);

INSERT INTO provider_configs (provider_key, endpoint_ref, auth_ref, timeout_ms, enabled_flag) 
VALUES ('falai', 'https://fal.run/fal-ai/flux/schnell', 'simplyviral.provider.falai.api-key', 120000, true);

-- Seed Explicit Media Limits inside Model Conf
INSERT INTO model_configs (model_config_key, provider_key, external_model_name, model_type, active_flag) 
VALUES ('eleven_multilingual_v2', 'elevenlabs', 'eleven_multilingual_v2', 'text-to-speech', true);

INSERT INTO model_configs (model_config_key, provider_key, external_model_name, model_type, active_flag) 
VALUES ('flux-schnell', 'falai', 'fal-ai/flux/schnell', 'text-to-image', true);
