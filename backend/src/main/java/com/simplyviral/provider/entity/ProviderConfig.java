package com.simplyviral.provider.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "provider_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderConfig {

    @Id
    @Column(name = "provider_key", length = 50)
    private String providerKey;

    @Column(name = "endpoint_ref", nullable = false)
    private String endpointRef;

    @Column(name = "auth_ref")
    private String authRef;

    @Column(name = "timeout_ms")
    @Builder.Default
    private Long timeoutMs = 30000L;

    @Column(name = "enabled_flag", nullable = false)
    @Builder.Default
    private Boolean enabledFlag = true;
}
