package com.simplyviral.provider.creatomate;

import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.provider.client.ProviderClient;
import com.simplyviral.provider.creatomate.dto.CreatomateRequest;
import com.simplyviral.provider.creatomate.dto.CreatomateResponse;
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
public class CreatomateAdapter implements ProviderClient<CreatomateRequest, CreatomateResponse> {

    private final WebClient webClient;
    private final String apiKey;

    public CreatomateAdapter(WebClient.Builder webClientBuilder,
                        @Value("${simplyviral.provider.creatomate.api-key:mock_key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl("https://api.creatomate.com/v1").build();
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderKey() {
        return "creatomate";
    }

    @Override
    public CreatomateResponse execute(CreatomateRequest request, StepRun stepContext) {
        log.info("Executing Creatomate Video Render for step run: {}", stepContext.getId());

        if ("mock_key".equals(apiKey) || !StringUtils.hasText(apiKey)) {
            log.warn("Creatomate Mock Key detected. Returning stub ASYNC response ID.");
            CreatomateResponse mockResp = new CreatomateResponse();
            mockResp.setId("mock-render-" + stepContext.getId());
            mockResp.setStatus("planned");
            return mockResp;
        }

        try {
            return webClient.post()
                    .uri("/renders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CreatomateResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Creatomate HTTP exchange failed", e);
            throw new SimplyViralException("Creatomate API execution failed", e);
        }
    }
}
