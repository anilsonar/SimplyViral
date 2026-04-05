package com.simplyviral.provider.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pricing_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pricing_id")
    private Long id;

    @Column(name = "provider_key", nullable = false, length = 50)
    private String providerKey;

    @Column(name = "model_config_key", nullable = false, length = 100)
    private String modelConfigKey;

    @Column(name = "unit_type", length = 50)
    private String unitType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rates_json", columnDefinition = "jsonb")
    private String ratesJson;

    @Column(name = "effective_from")
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;
}
