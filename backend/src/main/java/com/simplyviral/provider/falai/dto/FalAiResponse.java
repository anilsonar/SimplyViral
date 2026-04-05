package com.simplyviral.provider.falai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FalAiResponse {
    private List<ImageObj> images;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageObj {
        private String url;
        private int width;
        private int height;
        private String content_type;
    }
}
