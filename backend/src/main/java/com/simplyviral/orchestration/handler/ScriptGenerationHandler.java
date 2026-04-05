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

@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptGenerationHandler implements StepHandler {

    private final OpenAIGptAdapter gptAdapter;
    private final MeteredProviderExecutor executor;
    private final PromptTemplateRepository promptRepo;
    private final JobArtifactService artifactService;
    private final ProviderConfigService configService;

    @Override
    public StepKey getSupportedStep() {
        return StepKey.SCRIPT_GENERATION;
    }

    @Override
    public void handle(StepRun stepRunContext) {
        log.info("Handling Script Generation for StepRun {} (Job {})",
                stepRunContext.getId(), stepRunContext.getJob().getId());

        // 1. Read chosen topic from prior step
        String chosenTopic = artifactService.readArtifact(
                stepRunContext.getJob().getId(), "chosen_topic");

        // 2. Load prompt template
        PromptTemplate template = promptRepo.findById(StepKey.SCRIPT_GENERATION)
                .orElseThrow(() -> new RuntimeException("Missing prompt template for SCRIPT_GENERATION"));

        // 3. Resolve model name from DB config
        String modelName = configService.resolveModelName(stepRunContext.getModelConfigRef());

        // 4. Build the request
        String userPrompt = template.getUserPromptTemplate()
                .replace("{{topic}}", chosenTopic);

        GptRequest request = GptRequest.builder()
                .model(modelName)
                .messages(List.of(
                        GptRequest.Message.builder().role("system").content(template.getSystemPrompt()).build(),
                        GptRequest.Message.builder().role("user").content(userPrompt).build()
                ))
                .build();

        // 5. Metered execution
        GptResponse response = executor.executeMetered(gptAdapter, request, stepRunContext);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RuntimeException("Empty response from GPT for SCRIPT_GENERATION");
        }

        String rawScript = response.getChoices().get(0).getMessage().getContent();

        log.info("Script generated for Job {} ({} chars)", stepRunContext.getJob().getId(), rawScript.length());

        // 6. Persist raw script as artifact
        artifactService.writeArtifact(
                stepRunContext.getJob(),
                StepKey.SCRIPT_GENERATION,
                "raw_script",
                rawScript
        );
    }
}
