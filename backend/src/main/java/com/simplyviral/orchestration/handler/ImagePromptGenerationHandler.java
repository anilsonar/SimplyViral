package com.simplyviral.orchestration.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.service.JobArtifactService;
import com.simplyviral.provider.executor.MeteredProviderExecutor;
import com.simplyviral.provider.openai.OpenAIGptAdapter;
import com.simplyviral.provider.openai.dto.GptRequest;
import com.simplyviral.provider.openai.dto.GptResponse;
import com.simplyviral.provider.repository.PromptTemplateRepository;
import com.simplyviral.provider.service.ProviderConfigService;
import com.simplyviral.provider.entity.PromptTemplate;
import com.simplyviral.shared.constant.StepKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagePromptGenerationHandler implements StepHandler {

    private final OpenAIGptAdapter gptAdapter;
    private final MeteredProviderExecutor executor;
    private final PromptTemplateRepository promptRepo;
    private final JobArtifactService artifactService;
    private final ProviderConfigService configService;
    private final ObjectMapper objectMapper;

    @Override
    public StepKey getSupportedStep() {
        return StepKey.IMAGE_PROMPT_GENERATION;
    }

    @Override
    public void handle(StepRun stepRunContext) {
        log.info("Handling Image Prompt Generation for StepRun {} (Job {})",
                stepRunContext.getId(), stepRunContext.getJob().getId());

        // 1. Read scene breakdown from prior step
        String sceneJson = artifactService.readArtifact(
                stepRunContext.getJob().getId(), "scene_json");

        // 2. Load prompt template
        PromptTemplate template = promptRepo.findById(StepKey.IMAGE_PROMPT_GENERATION)
                .orElseThrow(() -> new RuntimeException("Missing prompt template for IMAGE_PROMPT_GENERATION"));

        // 3. Resolve model
        String modelName = configService.resolveModelName(stepRunContext.getModelConfigRef());

        // 4. Parse scenes and generate image prompts for each
        try {
            JsonNode scenes = objectMapper.readTree(sceneJson);
            JsonNode sceneArray = scenes.has("scenes") ? scenes.get("scenes") : scenes;

            // For efficiency, we send all scenes in one prompt and ask for a JSON array of prompts
            String allSceneDescriptions = sceneArray.toString();

            String userPrompt = template.getUserPromptTemplate()
                    .replace("{{scene_description}}", allSceneDescriptions);

            GptRequest request = GptRequest.builder()
                    .model(modelName)
                    .messages(List.of(
                            GptRequest.Message.builder().role("system").content(template.getSystemPrompt()).build(),
                            GptRequest.Message.builder().role("user").content(userPrompt).build()
                    ))
                    .build();

            // 5. Metered execution
            GptResponse response = executor.executeMetered(gptAdapter, request, stepRunContext);
            String promptsOutput = response.getChoices().get(0).getMessage().getContent();

            log.info("Generated image prompts for Job {} ({} chars)",
                    stepRunContext.getJob().getId(), promptsOutput.length());

            // 6. Save image prompts as artifact
            artifactService.writeArtifact(
                    stepRunContext.getJob(),
                    StepKey.IMAGE_PROMPT_GENERATION,
                    "image_prompts_json",
                    promptsOutput
            );

        } catch (Exception e) {
            log.error("Failed to parse scene JSON for image prompt generation", e);
            throw new RuntimeException("Image prompt generation failed: " + e.getMessage(), e);
        }
    }
}
