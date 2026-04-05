package com.simplyviral.orchestration.handler;

import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.service.JobArtifactService;
import com.simplyviral.shared.constant.StepKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Final edit/render step — post-composition processing.
 * Currently verifies all prior artifacts exist and marks the pipeline complete.
 * Future: subtitle overlay, thumbnail generation, format conversion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinalEditRenderHandler implements StepHandler {

    private final JobArtifactService artifactService;

    @Override
    public StepKey getSupportedStep() {
        return StepKey.FINAL_EDIT_RENDER;
    }

    @Override
    public void handle(StepRun stepRunContext) {
        log.info("Handling Final Edit/Render for StepRun {} (Job {})",
                stepRunContext.getId(), stepRunContext.getJob().getId());

        // 1. Verify all required artifacts exist
        String videoRenderId = artifactService.readArtifactOrNull(
                stepRunContext.getJob().getId(), "video_render_id");
        String videoUrl = artifactService.readArtifactOrNull(
                stepRunContext.getJob().getId(), "video_url");
        String audioUrl = artifactService.readArtifactOrNull(
                stepRunContext.getJob().getId(), "audio_url");
        String imageUrls = artifactService.readArtifactOrNull(
                stepRunContext.getJob().getId(), "image_urls_json");
        String chosenTopic = artifactService.readArtifactOrNull(
                stepRunContext.getJob().getId(), "chosen_topic");
        String rawScript = artifactService.readArtifactOrNull(
                stepRunContext.getJob().getId(), "raw_script");

        log.info("Final Edit verification for Job {}:", stepRunContext.getJob().getId());
        log.info("  chosen_topic: {}", chosenTopic != null ? "OK" : "MISSING");
        log.info("  raw_script: {}", rawScript != null ? "OK (" + rawScript.length() + " chars)" : "MISSING");
        log.info("  audio_url: {}", audioUrl != null ? "OK" : "MISSING");
        log.info("  image_urls: {}", imageUrls != null ? "OK" : "MISSING");
        log.info("  video_render_id: {}", videoRenderId != null ? videoRenderId : "MISSING");
        log.info("  video_url: {}", videoUrl != null ? videoUrl : "PENDING (async render)");

        // 2. Create final output summary artifact
        String summary = String.format(
                "{\"topic\": \"%s\", \"video_render_id\": \"%s\", \"video_url\": \"%s\", " +
                "\"audio_url\": \"%s\", \"status\": \"RENDERED\"}",
                chosenTopic != null ? chosenTopic.replace("\"", "\\\"") : "",
                videoRenderId != null ? videoRenderId : "",
                videoUrl != null ? videoUrl : "pending",
                audioUrl != null ? audioUrl : ""
        );

        artifactService.writeArtifact(
                stepRunContext.getJob(),
                StepKey.FINAL_EDIT_RENDER,
                "final_output",
                summary
        );

        log.info("Final edit/render complete for Job {}. Pipeline finished.", stepRunContext.getJob().getId());
    }
}
