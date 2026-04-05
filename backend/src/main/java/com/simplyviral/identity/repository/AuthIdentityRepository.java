package com.simplyviral.identity.repository;

import com.simplyviral.identity.entity.AuthIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, UUID> {
    Optional<AuthIdentity> findByProviderAndProviderId(String provider, String providerId);
}
