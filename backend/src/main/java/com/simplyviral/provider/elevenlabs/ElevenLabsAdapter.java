package com.simplyviral.provider.elevenlabs;

import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.provider.client.ProviderClient;
import com.simplyviral.provider.elevenlabs.dto.ElevenLabsRequest;
import com.simplyviral.shared.exception.SimplyViralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class ElevenLabsAdapter implements ProviderClient<ElevenLabsRequest, byte[]> {

    private final WebClient webClient;
    private final String apiKey;

    public ElevenLabsAdapter(WebClient.Builder webClientBuilder,
                             @Value("${simplyviral.provider.elevenlabs.api-key:mock_key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl("https://api.elevenlabs.io/v1").build();
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderKey() {
        return "elevenlabs";
    }

    @Override
    public byte[] execute(ElevenLabsRequest request, StepRun stepContext) {
        log.info("Executing ElevenLabs API call for step run: {}", stepContext.getId());

        // Stub/Mock return logic for local demo environments running without an explicit paid API key
        if ("mock_key".equals(apiKey) || !StringUtils.hasText(apiKey)) {
            log.warn("ElevenLabs Mock Key detected. Returning stub byte array.");
            return "MOCK_AUDIO_BYTES_FOR_DEV_ENVIRONMENT".getBytes();
        }

        try {
            return webClient.post()
                    .uri("/text-to-speech/{voiceId}", request.getVoiceId())
                    .header("xi-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.valueOf("audio/mpeg"))
                    .bodyValue(request.getPayload())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception e) {
            log.error("ElevenLabs HTTP exchange failed", e);
            throw new SimplyViralException("ElevenLabs API execution failed", e);
        }
    }
}
