package com.simplyviral.provider.creatomate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatomateResponse {
    private String id;
    private String status; // "planned", "rendering", "succeeded", "failed"
    private String url; // Filled only when succeeded (sync fallback)
}
