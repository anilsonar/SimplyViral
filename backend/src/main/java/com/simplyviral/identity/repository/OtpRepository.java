package com.simplyviral.identity.repository;

import com.simplyviral.identity.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {
    Optional<Otp> findTopByEmailAndTypeOrderByCreatedAtDesc(String email, String type);
    Optional<Otp> findTopByMobileNumberAndTypeOrderByCreatedAtDesc(String mobileNumber, String type);
}
