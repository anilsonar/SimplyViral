package com.simplyviral.provider.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "model_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelConfig {

    @Id
    @Column(name = "model_config_key", length = 100)
    private String modelConfigKey;

    @Column(name = "provider_key", nullable = false, length = 50)
    private String providerKey;

    @Column(name = "external_model_name", nullable = false)
    private String externalModelName;

    @Column(name = "model_type", length = 50)
    private String modelType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params_json", columnDefinition = "jsonb")
    private String paramsJson;

    @Column(name = "active_flag", nullable = false)
    @Builder.Default
    private Boolean activeFlag = true;
}
