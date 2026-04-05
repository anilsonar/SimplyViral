package com.simplyviral.provider.falai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FalAiRequest {
    private String prompt;
    private String image_size;
    private int num_inference_steps;
    private int num_images;
}
