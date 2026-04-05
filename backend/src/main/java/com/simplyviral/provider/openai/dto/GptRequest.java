package com.simplyviral.provider.openai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GptRequest {
    private String model;
    private List<Message> messages;
    
    @JsonProperty("response_format")
    private Map<String, String> responseFormat;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}
