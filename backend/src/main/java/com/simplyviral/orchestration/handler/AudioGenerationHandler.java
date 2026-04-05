package com.simplyviral.orchestration.handler;

import com.simplyviral.asset.service.AssetStorageService;
import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.entity.StepRunUsage;
import com.simplyviral.orchestration.service.JobArtifactService;
import com.simplyviral.provider.elevenlabs.ElevenLabsAdapter;
import com.simplyviral.provider.elevenlabs.dto.ElevenLabsRequest;
import com.simplyviral.provider.executor.MeteredProviderExecutor;
import com.simplyviral.provider.service.ProviderConfigService;
import com.simplyviral.shared.constant.StepKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioGenerationHandler implements StepHandler {

    private final ElevenLabsAdapter audioAdapter;
    private final MeteredProviderExecutor executor;
    private final AssetStorageService storageService;
    private final JobArtifactService artifactService;
    private final ProviderConfigService configService;

    @Override
    public StepKey getSupportedStep() {
        return StepKey.AUDIO_GENERATION;
    }

    @Override
    public void handle(StepRun stepRunContext) {
        log.info("Handling Audio Generation for StepRun {} (Job {})",
                stepRunContext.getId(), stepRunContext.getJob().getId());

        // 1. Read the raw script from prior step
        String rawScript = artifactService.readArtifact(
                stepRunContext.getJob().getId(), "raw_script");

        // 2. Resolve model from DB config
        String modelId = configService.resolveModelName(stepRunContext.getModelConfigRef());

        // 3. Build ElevenLabs request
        ElevenLabsRequest request = ElevenLabsRequest.builder()
                .voiceId("21m00Tcm4TlvDq8ikWAM") // Rachel default — can be moved to model params
                .payload(ElevenLabsRequest.Payload.builder()
                        .text(rawScript)
                        .model_id(modelId != null ? modelId : "eleven_multilingual_v2")
                        .voice_settings(ElevenLabsRequest.VoiceSettings.builder()
                                .stability(0.5)
                                .similarity_boost(0.75)
                                .build())
                        .build())
                .build();

        // 4. Metered execution
        byte[] audioBytes = executor.executeMetered(audioAdapter, request, stepRunContext);

        // 5. Populate usage — character count for cost tracking, estimated audio duration
        int charCount = rawScript.length();
        // Rough estimate: ~15 characters per second of speech
        BigDecimal estimatedSeconds = BigDecimal.valueOf(charCount).divide(BigDecimal.valueOf(15), 2, java.math.RoundingMode.HALF_UP);

        StepRunUsage usage = StepRunUsage.builder()
                .audioSeconds(estimatedSeconds)
                .rawUsageJson("{\"character_count\": " + charCount + ", \"audio_seconds\": " + estimatedSeconds + "}")
                .build();
        stepRunContext.setUsage(usage);

        // 6. Store audio in configured storage backend
        String assetUri = storageService.storeAsset(
                "voiceover_" + stepRunContext.getId() + ".mp3",
                "audio/mpeg",
                audioBytes
        );

        log.info("Audio generated for Job {}. Asset: {}. Estimated duration: {}s",
                stepRunContext.getJob().getId(), assetUri, estimatedSeconds);

        // 7. Persist audio URL as artifact
        artifactService.writeArtifact(
                stepRunContext.getJob(),
                StepKey.AUDIO_GENERATION,
                "audio_url",
                assetUri
        );
    }
}
