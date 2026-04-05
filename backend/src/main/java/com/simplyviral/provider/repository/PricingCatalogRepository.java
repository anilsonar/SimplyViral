package com.simplyviral.provider.repository;

import com.simplyviral.provider.entity.PricingCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PricingCatalogRepository extends JpaRepository<PricingCatalog, Long> {

    @Query("SELECT p FROM PricingCatalog p WHERE p.providerKey = :providerKey " +
           "AND p.modelConfigKey = :modelConfigKey " +
           "AND (p.effectiveTo IS NULL OR p.effectiveTo >= CURRENT_TIMESTAMP) " +
           "ORDER BY p.effectiveFrom DESC")
    Optional<PricingCatalog> findActiveRate(
            @Param("providerKey") String providerKey,
            @Param("modelConfigKey") String modelConfigKey);
}
