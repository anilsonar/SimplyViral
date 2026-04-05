package com.simplyviral.provider.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.entity.StepRunUsage;
import com.simplyviral.provider.client.ProviderClient;
import com.simplyviral.provider.openai.dto.GptRequest;
import com.simplyviral.provider.openai.dto.GptResponse;
import com.simplyviral.shared.exception.SimplyViralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class OpenAIGptAdapter implements ProviderClient<GptRequest, GptResponse> {

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public OpenAIGptAdapter(WebClient.Builder webClientBuilder,
                            @Value("${simplyviral.provider.openai.api-key}") String apiKey,
                            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com/v1").build();
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderKey() {
        return "openai";
    }

    @Override
    public GptResponse execute(GptRequest request, StepRun stepContext) {
        log.info("Executing OpenAI chat completion for step_run={} model={}",
                stepContext != null ? stepContext.getId() : "N/A", request.getModel());

        // Mock response for development without real API key
        if ("mock_key".equals(apiKey)) {
            log.warn("OpenAI Mock Key detected. Returning stub response.");
            GptResponse mockResponse = createMockResponse(request);
            populateUsageOnContext(mockResponse, stepContext);
            return mockResponse;
        }

        try {
            GptResponse response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GptResponse.class)
                    .block();

            if (response != null) {
                populateUsageOnContext(response, stepContext);
            }

            return response;

        } catch (Exception e) {
            log.error("Failed to execute OpenAI request: {}", e.getMessage(), e);
            throw new SimplyViralException("OpenAI API execution failed", e);
        }
    }

    /**
     * Populates the transient StepRunUsage on the StepRun context
     * so that MeteredProviderExecutor can persist it.
     */
    private void populateUsageOnContext(GptResponse response, StepRun stepContext) {
        if (response == null || response.getUsage() == null || stepContext == null) return;

        try {
            StepRunUsage usage = StepRunUsage.builder()
                    .promptTokens(response.getUsage().getPrompt_tokens())
                    .completionTokens(response.getUsage().getCompletion_tokens())
                    .totalTokens(response.getUsage().getTotal_tokens())
                    .rawUsageJson(objectMapper.writeValueAsString(response.getUsage()))
                    .build();

            stepContext.setUsage(usage);

            log.debug("Usage populated: prompt_tokens={} completion_tokens={} total_tokens={}",
                    usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        } catch (Exception e) {
            log.warn("Failed to populate usage on step context", e);
        }
    }

    /**
     * Creates a deterministic mock response for local dev without paid API key.
     */
    private GptResponse createMockResponse(GptRequest request) {
        GptResponse response = new GptResponse();

        GptResponse.Message msg = new GptResponse.Message();
        msg.setRole("assistant");

        // Produce contextual mock responses based on the system prompt
        String systemPrompt = request.getMessages().stream()
                .filter(m -> "system".equals(m.getRole()))
                .map(GptRequest.Message::getContent)
                .findFirst().orElse("");

        if (systemPrompt.contains("selector") || systemPrompt.contains("topic")) {
            msg.setContent("AI Replacing Developers in 2026");
        } else if (systemPrompt.contains("scriptwriter")) {
            msg.setContent("[INTRO] Welcome to this shocking revelation. In 2026, AI agents are writing better code " +
                    "than most developers. [BODY] Companies are replacing entire teams with AI coding assistants. " +
                    "The speed is unprecedented. The quality is often better. But at what cost? [OUTRO] " +
                    "Subscribe and let us know — is your job safe? Drop a comment below.");
        } else if (systemPrompt.contains("director") || systemPrompt.contains("scenes")) {
            msg.setContent("{\"scenes\": [" +
                    "{\"scene_number\": 1, \"description\": \"Futuristic office with AI robots at desks\", \"duration_seconds\": 5}," +
                    "{\"scene_number\": 2, \"description\": \"Split screen: human coder vs AI agent coding\", \"duration_seconds\": 10}," +
                    "{\"scene_number\": 3, \"description\": \"Dramatic graph showing AI productivity surge\", \"duration_seconds\": 8}," +
                    "{\"scene_number\": 4, \"description\": \"Person looking worried at computer screen\", \"duration_seconds\": 7}" +
                    "]}");
        } else if (systemPrompt.contains("image prompt")) {
            msg.setContent("[\"Hyper-realistic futuristic office, AI robots sitting at minimalist desks, neon blue lighting, " +
                    "cinematic wide angle, 8k resolution\"," +
                    "\"Split screen digital art, human programmer left side vs AI coding agent right side, " +
                    "dramatic lighting, cyberpunk aesthetic\"," +
                    "\"3D rendered graph with glowing bars shooting upward, holographic display, dark background, " +
                    "data visualization art\"," +
                    "\"Worried professional staring at laptop screen in dark room, screen glow on face, " +
                    "photorealistic, moody atmosphere\"]");
        } else {
            msg.setContent("This is a mock AI response for development purposes.");
        }

        GptResponse.Choice choice = new GptResponse.Choice();
        choice.setMessage(msg);
        choice.setFinish_reason("stop");
        response.setChoices(java.util.List.of(choice));

        GptResponse.Usage usage = new GptResponse.Usage();
        usage.setPrompt_tokens(150);
        usage.setCompletion_tokens(200);
        usage.setTotal_tokens(350);
        response.setUsage(usage);

        // Usage is populated on the stepContext by the caller (execute method)
        return response;
    }
}
