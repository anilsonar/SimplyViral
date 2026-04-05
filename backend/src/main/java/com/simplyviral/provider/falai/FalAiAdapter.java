package com.simplyviral.provider.falai;

import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.provider.client.ProviderClient;
import com.simplyviral.provider.falai.dto.FalAiRequest;
import com.simplyviral.provider.falai.dto.FalAiResponse;
import com.simplyviral.shared.exception.SimplyViralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;

@Slf4j
@Service
public class FalAiAdapter implements ProviderClient<FalAiRequest, FalAiResponse> {

    private final WebClient webClient;
    private final String apiKey;

    public FalAiAdapter(WebClient.Builder webClientBuilder,
                        @Value("${simplyviral.provider.falai.api-key:mock_key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl("https://fal.run").build();
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderKey() {
        return "falai";
    }

    @Override
    public FalAiResponse execute(FalAiRequest request, StepRun stepContext) {
        log.info("Executing Fal.ai Image Generation for step run: {}", stepContext.getId());

        if ("mock_key".equals(apiKey) || !StringUtils.hasText(apiKey)) {
            log.warn("Fal.ai Mock Key detected. Returning stub image URL.");
            FalAiResponse mockResp = new FalAiResponse();
            FalAiResponse.ImageObj mockImg = new FalAiResponse.ImageObj();
            // Free random image placeholder simulating external response bounds
            mockImg.setUrl("https://picsum.photos/seed/" + stepContext.getId() + "/1080/1920"); 
            mockResp.setImages(Collections.singletonList(mockImg));
            return mockResp;
        }

        try {
            return webClient.post()
                    .uri("/fal-ai/flux/schnell")
                    .header(HttpHeaders.AUTHORIZATION, "Key " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FalAiResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Fal.ai HTTP exchange failed", e);
            throw new SimplyViralException("Fal.ai API execution failed", e);
        }
    }
}
