package com.simplyviral.orchestration.handler;

import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.service.JobArtifactService;
import com.simplyviral.shared.constant.StepKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Loads topic candidates from the configured source (classpath JSON file).
 * Future: replace with Google Sheets API integration.
 * This is an internal step — no external API call, no metering.
 */
@Slf4j
@Service
public class TopicFetchHandler implements StepHandler {

    private final JobArtifactService artifactService;
    private final ResourceLoader resourceLoader;
    private final String topicsSource;

    public TopicFetchHandler(JobArtifactService artifactService,
                             ResourceLoader resourceLoader,
                             @Value("${simplyviral.topics.source:classpath:topics/topics.json}") String topicsSource) {
        this.artifactService = artifactService;
        this.resourceLoader = resourceLoader;
        this.topicsSource = topicsSource;
    }

    @Override
    public StepKey getSupportedStep() {
        return StepKey.TOPIC_FETCH;
    }

    @Override
    public void handle(StepRun stepRunContext) {
        log.info("Handling Topic Fetch for StepRun {} (Job {})",
                stepRunContext.getId(), stepRunContext.getJob().getId());

        try {
            Resource resource = resourceLoader.getResource(topicsSource);
            String topicsJson = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            log.info("Loaded {} bytes of topic data from {}", topicsJson.length(), topicsSource);

            artifactService.writeArtifact(
                    stepRunContext.getJob(),
                    StepKey.TOPIC_FETCH,
                    "topic_candidates",
                    topicsJson
            );

            log.info("Topic candidates persisted as artifact for Job {}", stepRunContext.getJob().getId());

        } catch (Exception e) {
            log.error("Failed to load topics from {}", topicsSource, e);
            throw new RuntimeException("Topic fetch failed: " + e.getMessage(), e);
        }
    }
}
