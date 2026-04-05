# Simply Viral Engineering Principles

## Purpose
This file is the coding contract for backend and React Native implementation. It is written for human developers and code-generation agents such as Gemini or Anti Gravity.

## Locked project principles
1. **Modular monolith only**. Spring Boot is the single runtime owner.
2. **Configuration-driven workflow**. Workflow behavior, step behavior, provider selection, model selection, fallback, retry, storage, and priority come from configuration and DB-backed definitions.
3. **No hardcoded external API paths in business code**.
4. **No hardcoded model names in business code**.
5. **No hardcoded dependency on any workflow automation tool**.
6. **No hardcoded dependency on any message broker or cache provider**.
7. **Every step is first-class and observable**, whether the step is internal or external.
8. **DB-backed fallback is mandatory** for queueing and orchestration if optional infrastructure is absent.

## Core business flow
1. Topic fetch from DB.
2. Topic selection using LLM.
3. Script generation.
4. Script breakdown into structured metadata.
5. Image prompt generation.
6. Image generation.
7. Audio generation.
8. Video composition.
9. Final edit/render.

Represent these in code as system step keys:
- `TOPIC_FETCH`
- `TOPIC_SELECTION`
- `SCRIPT_GENERATION`
- `SCRIPT_BREAKDOWN`
- `IMAGE_PROMPT_GENERATION`
- `IMAGE_GENERATION`
- `AUDIO_GENERATION`
- `VIDEO_COMPOSITION`
- `FINAL_EDIT_RENDER`

## Required coding standards
- Follow **SOLID** principles.
- Prefer **interface-first design** for provider integrations and optional infrastructure.
- Prefer **clear package boundaries** over deeply coupled service classes.
- Use **readable code**, not clever code.
- Keep controllers thin.
- Keep orchestration logic out of controllers.
- Keep provider-specific logic out of business services.
- Use constructor injection.
- Favor immutable request/response DTOs where practical.
- Use explicit names such as `WorkflowDefinitionService`, `DbQueueProvider`, `PricingService`, `MeteredProviderExecutor`.

## Naming conventions
- Classes: `PascalCase`
- Methods and fields: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Database tables: `snake_case`
- Step keys and status enums: `UPPER_SNAKE_CASE`
- Config keys: lowercase dotted notation under `simplyviral.*`

## Package guidance for backend
- `identity`
- `workflow`
- `orchestration`
- `queue`
- `provider`
- `asset`
- `analytics`
- `admin`
- `shared`

Do not dump unrelated classes into one generic `service` package.

## Mandatory abstractions
### Queue
Use a `QueueProvider` abstraction.
Implementations may include:
- `DbQueueProvider`
- `BrokerQueueProvider`

### Cache
Use a `CacheProvider` abstraction.
Implementations may include:
- `NoOpCacheProvider`
- `RedisCacheProvider`

### Locks
Use a `LockProvider` abstraction.
Implementations may include:
- `DbLockProvider`
- `RedisLockProvider`

### Providers
Use typed provider interfaces.
Examples:
- `TopicSelectionClient`
- `ScriptGenerationClient`
- `ImageGenerationClient`
- `AudioGenerationClient`
- `VideoCompositionClient`
- `FinalRenderClient`

Wrap external calls with `MeteredProviderExecutor` or equivalent.

## Observability requirements
Every step execution must record:
- `job_id`
- `step_run_id`
- `user_id`
- `workflow_id`
- `workflow_version`
- `plan_type`
- `step_key`
- `provider_ref`
- `model_config_ref`
- `attempt_no`
- `status`
- `started_at`
- `finished_at`
- `latency_ms`
- `planned_cost`
- `actual_cost`
- usage metrics such as tokens, image count, audio seconds, video seconds
- retry/fallback details
- error code and message when relevant

## Logging rules
- Use structured logs.
- Include correlation identifiers.
- Log state transitions, retries, fallback, and provider failures.
- Do not log secrets.
- Do not log raw OAuth tokens, refresh tokens, OTP values, or provider credentials.
- Do not log sensitive payloads unless redacted and explicitly needed for support tooling.

## Retry and fallback rules
Each step definition must include:
- retry policy
- timeout policy
- fallback policy
- idempotency behavior
- storage policy

No step should rely on hand-written ad hoc retry loops in random service classes.

## Configuration rules
### Allowed in source config (ENV vars)
- provider API keys via `${ENV_VAR:fallback}` pattern in `application.yml`
- provider endpoint templates
- queue mode
- cache mode
- lock mode
- feature flags
- storage defaults

### Allowed in DB
- workflow definitions
- step definitions (with dependency DAG, retry/fallback/storage policy as JSONB)
- provider registry (`provider_configs` table: endpoint URLs, auth property reference, timeout)
- model catalog (`model_configs` table: provider key, external model name, params)
- pricing catalog (`pricing_catalog` table: rates per unit type)
- prompt templates (`prompt_templates` table: system prompt, user prompt template per step)
- plan overrides
- fallback policies (inline in step definition JSONB)

### API key storage strategy
- API keys are stored in **environment variables** (`OPENAI_API_KEY`, `FALAI_API_KEY`, etc.)
- The DB `provider_configs.auth_ref` stores the **property name** (e.g., `simplyviral.provider.openai.api-key`)
- Adapters resolve keys from Spring Environment using `@Value` injection
- Keys must **never** be stored in the database, source code, or logs

### Inter-step data passing
- Steps produce and consume data via the `job_artifacts` table
- Each artifact has a `job_id`, `step_key` (producer), `artifact_key` (name), and `artifact_value` (TEXT)
- Standard artifact keys: `topic_candidates`, `chosen_topic`, `raw_script`, `scene_json`, `image_prompts_json`, `image_urls_json`, `audio_url`, `video_render_id`, `video_url`, `final_output`

### Topics source
- Topics will be sourced from **Google Sheets** in production
- For development, topics are loaded from `classpath:topics/topics.json`
- The source is configurable via `simplyviral.topics.source`

### Forbidden
- hardcoding model names in business logic
- hardcoding external API paths in business logic
- hardcoding Rabbit or Redis assumptions in orchestration logic
- hardcoding one workflow shape into controllers or services
- storing API keys in the database or source code
- passing data between steps via in-memory state (use `job_artifacts` table)

## Storage rules
- Final videos persist by default.
- Intermediate images must be config-driven.
- If `image-store = false`, do not retain intermediate images beyond temporary processing need.
- If storage is enabled for premium/debug/audit, retention must be explicit and traceable.

## Security rules
- Support Google and Apple through OAuth2/OIDC where applicable.
- Support mobile and email through OTP/passwordless flows.
- Use Spring Security.
- Use short-lived JWT access tokens and rotating refresh tokens.
- Secrets belong in secure config or secret stores, never in source code.

## React Native guidance
- Keep API consumption typed and centralized.
- Maintain a clean separation between auth state, generation state, analytics state, and UI rendering.
- Reflect job status as a timeline users can understand.
- Do not embed provider-specific assumptions in the mobile app.

## Code comments
- Comment *why*, not the obvious *what*.
- Add comments to explain non-trivial orchestration rules, fallback behavior, quota handling, and idempotency decisions.
- Avoid noisy comments that restate method names.

## Definition of done for backend code
A feature is not done unless:
1. it follows the workflow/config principles,
2. it emits required metrics and logs,
3. it has retry/fallback behavior or an explicit non-retryable reason,
4. it avoids hardcoded provider/model/path assumptions,
5. it is readable enough that a new engineer can identify responsibility by package and class names.
