package com.simplyviral.provider.creatomate.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class CreatomateRequest {
    /** 
     * Template ID allows Creatomate to bind assets explicitly to a pre-defined animation wrapper on their cloud.
     * We can also pass raw JSON `source` but `template_id` is far safer for SaaS abstractions.
     */
    private String template_id;
    
    /** 
     * Dynamic modifications to apply to the template (e.g. injecting our AI audio MP3 and AI image URLs)
     */
    private Map<String, Object> modifications;

    /**
     * Webhook target URL - this allows Creatomate to POST back to our backend when the 2-minute render finishes.
     */
    private String webhook_url;
}
