package com.simplyviral.provider.elevenlabs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElevenLabsRequest {
    private String voiceId;
    private Payload payload;

    @Data
    @Builder
    public static class Payload {
        private String text;
        private String model_id;
        private VoiceSettings voice_settings;
    }

    @Data
    @Builder
    public static class VoiceSettings {
        private double stability;
        private double similarity_boost;
    }
}
