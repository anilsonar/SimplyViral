package com.simplyviral.orchestration.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.entity.StepRunUsage;
import com.simplyviral.orchestration.service.JobArtifactService;
import com.simplyviral.provider.executor.MeteredProviderExecutor;
import com.simplyviral.provider.falai.FalAiAdapter;
import com.simplyviral.provider.falai.dto.FalAiRequest;
import com.simplyviral.provider.falai.dto.FalAiResponse;
import com.simplyviral.shared.constant.StepKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationHandler implements StepHandler {

    private final FalAiAdapter imageAdapter;
    private final MeteredProviderExecutor executor;
    private final JobArtifactService artifactService;
    private final ObjectMapper objectMapper;

    @Override
    public StepKey getSupportedStep() {
        return StepKey.IMAGE_GENERATION;
    }

    @Override
    public void handle(StepRun stepRunContext) {
        log.info("Handling Image Generation for StepRun {} (Job {})",
                stepRunContext.getId(), stepRunContext.getJob().getId());

        // 1. Read image prompts from prior step
        String promptsRaw = artifactService.readArtifact(
                stepRunContext.getJob().getId(), "image_prompts_json");

        try {
            // 2. Parse prompts — could be a JSON array or a single text
            List<String> prompts = parsePrompts(promptsRaw);
            List<String> imageUrls = new ArrayList<>();
            int totalImages = 0;

            // 3. Generate an image for each prompt
            for (int i = 0; i < prompts.size(); i++) {
                String prompt = prompts.get(i);
                log.info("Generating image {}/{} for Job {}", i + 1, prompts.size(),
                        stepRunContext.getJob().getId());

                FalAiRequest request = FalAiRequest.builder()
                        .prompt(prompt)
                        .image_size("landscape_16_9")
                        .num_inference_steps(4)
                        .num_images(1)
                        .build();

                FalAiResponse response = executor.executeMetered(imageAdapter, request, stepRunContext);

                if (response.getImages() != null && !response.getImages().isEmpty()) {
                    imageUrls.add(response.getImages().get(0).getUrl());
                    totalImages++;
                }
            }

            // 4. Populate usage for cost tracking
            StepRunUsage usage = StepRunUsage.builder()
                    .imageCount(totalImages)
                    .rawUsageJson("{\"image_count\": " + totalImages + "}")
                    .build();
            stepRunContext.setUsage(usage);

            // 5. Persist image URLs as artifact
            String urlsJson = objectMapper.writeValueAsString(imageUrls);
            artifactService.writeArtifact(
                    stepRunContext.getJob(),
                    StepKey.IMAGE_GENERATION,
                    "image_urls_json",
                    urlsJson
            );

            log.info("Generated {} images for Job {}", totalImages, stepRunContext.getJob().getId());

        } catch (Exception e) {
            log.error("Image generation failed", e);
            throw new RuntimeException("Image generation failed: " + e.getMessage(), e);
        }
    }

    private List<String> parsePrompts(String raw) {
        try {
            // Try parsing as JSON array first
            JsonNode node = objectMapper.readTree(raw);
            if (node.isArray()) {
                List<String> list = new ArrayList<>();
                node.forEach(n -> list.add(n.isTextual() ? n.asText() : n.toString()));
                return list;
            }
        } catch (Exception ignored) {
            // Not valid JSON, fall through
        }
        // Treat as single prompt text (split by newlines for multi-scene)
        return List.of(raw.split("\\n\\n"));
    }
}
