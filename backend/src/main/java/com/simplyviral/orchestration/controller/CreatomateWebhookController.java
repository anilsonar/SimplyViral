package com.simplyviral.orchestration.controller;

import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.repository.StepRunRepository;
import com.simplyviral.orchestration.service.JobArtifactService;
import com.simplyviral.orchestration.service.Orchestrator;
import com.simplyviral.shared.constant.StepKey;
import com.simplyviral.shared.constant.StepStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/creatomate")
@RequiredArgsConstructor
public class CreatomateWebhookController {

    private final StepRunRepository stepRunRepository;
    private final JobArtifactService artifactService;
    private final Orchestrator orchestrator;

    @PostMapping("/{stepRunId}")
    public ResponseEntity<Void> receiveVideoStatus(
            @PathVariable UUID stepRunId,
            @RequestBody Map<String, Object> payload) {

        log.info("Received Creatomate Webhook for StepRun {}: {}", stepRunId, payload);

        String status = (String) payload.get("status");
        StepRun stepRun = stepRunRepository.findById(stepRunId).orElse(null);

        if (stepRun == null) {
            log.warn("StepRun {} not found for Creatomate webhook", stepRunId);
            return ResponseEntity.ok().build();
        }

        if ("succeeded".equalsIgnoreCase(status)) {
            String downloadUrl = (String) payload.get("url");
            log.info("Video render SUCCEEDED for StepRun {}. URL: {}", stepRunId, downloadUrl);

            // Store the final video URL
            artifactService.writeArtifact(
                    stepRun.getJob(),
                    StepKey.VIDEO_COMPOSITION,
                    "video_url",
                    downloadUrl
            );

            // Update StepRun if it was still in RUNNING/COMPLETED state
            stepRun.setFinishedAt(OffsetDateTime.now());
            stepRunRepository.save(stepRun);

            // Trigger orchestrator to chain next step (FINAL_EDIT_RENDER)
            orchestrator.handleStepCompletion(stepRun.getJob(), StepKey.VIDEO_COMPOSITION);

        } else if ("failed".equalsIgnoreCase(status)) {
            String errorMsg = (String) payload.get("error_message");
            log.error("Video render FAILED for StepRun {}: {}", stepRunId, errorMsg);

            stepRun.setStatus(StepStatus.FAILED);
            stepRun.setErrorCode("CREATOMATE_RENDER_FAILED");
            stepRun.setErrorMessage(errorMsg);
            stepRun.setFinishedAt(OffsetDateTime.now());
            stepRunRepository.save(stepRun);

            orchestrator.handleStepFailure(stepRun.getJob(), StepKey.VIDEO_COMPOSITION,
                    "CREATOMATE_RENDER_FAILED", errorMsg);
        }

        return ResponseEntity.ok().build();
    }
}
