CREATE TABLE prompt_templates (
    step_key VARCHAR(100) PRIMARY KEY,
    system_prompt TEXT NOT NULL,
    user_prompt_template TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed Providers
INSERT INTO provider_configs (provider_key, endpoint_ref, auth_ref, timeout_ms, enabled_flag) 
VALUES ('openai', 'https://api.openai.com/v1/chat/completions', 'simplyviral.provider.openai.api-key', 60000, true);

-- Seed Models
INSERT INTO model_configs (model_config_key, provider_key, external_model_name, model_type, active_flag) 
VALUES ('gpt-4o-baseline', 'openai', 'gpt-4o', 'chat', true);

INSERT INTO model_configs (model_config_key, provider_key, external_model_name, model_type, active_flag) 
VALUES ('gpt-4-turbo-baseline', 'openai', 'gpt-4-turbo', 'chat', true);

-- Seed Prompts (Baseline Defaults)
INSERT INTO prompt_templates (step_key, system_prompt, user_prompt_template) 
VALUES (
  'TOPIC_SELECTION', 
  'You are a trending viral YouTube shorts selector. Return only the best topic title.', 
  'From the following candidates, pick the most engaging topic: {{candidates}}'
);

INSERT INTO prompt_templates (step_key, system_prompt, user_prompt_template) 
VALUES (
  'SCRIPT_GENERATION', 
  'You are a viral YouTube short scriptwriter. Keep the script under 60 seconds of speaking time.', 
  'Write a script for the topic: {{topic}}'
);

INSERT INTO prompt_templates (step_key, system_prompt, user_prompt_template) 
VALUES (
  'SCRIPT_BREAKDOWN', 
  'You are an AI director. Extract the scenes from the script and return structured JSON.', 
  'Break down the following script into JSON scenes: {{script}}'
);

INSERT INTO prompt_templates (step_key, system_prompt, user_prompt_template) 
VALUES (
  'IMAGE_PROMPT_GENERATION', 
  'You are an AI image prompt engineer. Convert scenes into Midjourney style visual prompts.', 
  'Generate an image prompt for the scene: {{scene_description}}'
);
