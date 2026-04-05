package com.simplyviral.orchestration.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.entity.StepRunUsage;
import com.simplyviral.orchestration.service.JobArtifactService;
import com.simplyviral.provider.creatomate.CreatomateAdapter;
import com.simplyviral.provider.creatomate.dto.CreatomateRequest;
import com.simplyviral.provider.creatomate.dto.CreatomateResponse;
import com.simplyviral.provider.executor.MeteredProviderExecutor;
import com.simplyviral.shared.constant.StepKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCompositionHandler implements StepHandler {

    private final CreatomateAdapter videoAdapter;
    private final MeteredProviderExecutor executor;
    private final JobArtifactService artifactService;
    private final ObjectMapper objectMapper;

    @Override
    public StepKey getSupportedStep() {
        return StepKey.VIDEO_COMPOSITION;
    }

    @Override
    public void handle(StepRun stepRunContext) {
        log.info("Handling Video Composition for StepRun {} (Job {})",
                stepRunContext.getId(), stepRunContext.getJob().getId());

        // 1. Read image URLs and audio URL from prior steps
        String imageUrlsJson = artifactService.readArtifact(
                stepRunContext.getJob().getId(), "image_urls_json");
        String audioUrl = artifactService.readArtifact(
                stepRunContext.getJob().getId(), "audio_url");

        try {
            List<String> imageUrls = objectMapper.readValue(imageUrlsJson, new TypeReference<List<String>>() {});

            // 2. Build Creatomate modifications map
            Map<String, Object> dynamicBinds = new HashMap<>();
            dynamicBinds.put("Voiceover-Track", audioUrl);
            for (int i = 0; i < imageUrls.size(); i++) {
                dynamicBinds.put("Image-Plate-" + (i + 1), imageUrls.get(i));
            }

            // 3. Build request
            CreatomateRequest request = CreatomateRequest.builder()
                    .template_id("viral-shorts-template-v1") // From DB model_config params in future
                    .modifications(dynamicBinds)
                    .webhook_url("https://api.yourdomain.com/api/v1/webhooks/creatomate/" + stepRunContext.getId())
                    .build();

            // 4. Metered execution (async — returns immediately with render ID)
            CreatomateResponse response = executor.executeMetered(videoAdapter, request, stepRunContext);

            // 5. Track usage
            StepRunUsage usage = StepRunUsage.builder()
                    .videoSeconds(BigDecimal.ZERO) // Unknown until render completes
                    .rawUsageJson("{\"render_id\": \"" + response.getId() + "\", \"image_count\": " + imageUrls.size() + "}")
                    .build();
            stepRunContext.setUsage(usage);

            log.info("Video composition initiated for Job {}. Render ID: {}",
                    stepRunContext.getJob().getId(), response.getId());

            // 6. Persist render ID as artifact
            artifactService.writeArtifact(
                    stepRunContext.getJob(),
                    StepKey.VIDEO_COMPOSITION,
                    "video_render_id",
                    response.getId()
            );

            // If the mock returns a direct URL, also store it
            if (response.getUrl() != null) {
                artifactService.writeArtifact(
                        stepRunContext.getJob(),
                        StepKey.VIDEO_COMPOSITION,
                        "video_url",
                        response.getUrl()
                );
            }

        } catch (Exception e) {
            log.error("Video composition failed", e);
            throw new RuntimeException("Video composition failed: " + e.getMessage(), e);
        }
    }
}
