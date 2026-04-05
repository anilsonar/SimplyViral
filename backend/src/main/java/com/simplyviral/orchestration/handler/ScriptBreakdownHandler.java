package com.simplyviral.orchestration.handler;

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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptBreakdownHandler implements StepHandler {

    private final OpenAIGptAdapter gptAdapter;
    private final MeteredProviderExecutor executor;
    private final PromptTemplateRepository promptRepo;
    private final JobArtifactService artifactService;
    private final ProviderConfigService configService;

    @Override
    public StepKey getSupportedStep() {
        return StepKey.SCRIPT_BREAKDOWN;
    }

    @Override
    public void handle(StepRun stepRunContext) {
        log.info("Handling Script Breakdown for StepRun {} (Job {})",
                stepRunContext.getId(), stepRunContext.getJob().getId());

        // 1. Read raw script from prior step
        String rawScript = artifactService.readArtifact(
                stepRunContext.getJob().getId(), "raw_script");

        // 2. Load prompt template
        PromptTemplate template = promptRepo.findById(StepKey.SCRIPT_BREAKDOWN)
                .orElseThrow(() -> new RuntimeException("Missing prompt template for SCRIPT_BREAKDOWN"));

        // 3. Resolve model from DB
        String modelName = configService.resolveModelName(stepRunContext.getModelConfigRef());

        // 4. Build request with JSON output format for structured scenes
        String userPrompt = template.getUserPromptTemplate()
                .replace("{{script}}", rawScript);

        GptRequest request = GptRequest.builder()
                .model(modelName)
                .messages(List.of(
                        GptRequest.Message.builder().role("system").content(template.getSystemPrompt()).build(),
                        GptRequest.Message.builder().role("user").content(userPrompt).build()
                ))
                .responseFormat(Map.of("type", "json_object"))
                .build();

        // 5. Metered execution
        GptResponse response = executor.executeMetered(gptAdapter, request, stepRunContext);
        String sceneJson = response.getChoices().get(0).getMessage().getContent();

        log.info("Script broken down into scenes for Job {} ({} chars JSON)",
                stepRunContext.getJob().getId(), sceneJson.length());

        // 6. Persist structured scene JSON as artifact
        artifactService.writeArtifact(
                stepRunContext.getJob(),
                StepKey.SCRIPT_BREAKDOWN,
                "scene_json",
                sceneJson
        );
    }
}
