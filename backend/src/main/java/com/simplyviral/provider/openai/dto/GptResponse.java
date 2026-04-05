package com.simplyviral.provider.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GptResponse {
    private List<Choice> choices;
    private Usage usage;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Message message;
        private String finish_reason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
    }
}
