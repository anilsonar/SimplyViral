package com.simplyviral.identity.repository;

import com.simplyviral.identity.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {
    Optional<Otp> findTopByEmailAndTypeOrderByCreatedAtDesc(String email, String type);
    Optional<Otp> findTopByMobileNumberAndTypeOrderByCreatedAtDesc(String mobileNumber, String type);

    /**
     * Rate-limit OTP requests: count how many OTPs were sent to this identifier
     * of this type since the given cutoff time.
     */
    @Query("SELECT COUNT(o) FROM Otp o WHERE " +
           "((o.email = :identifier AND o.type = :type) OR " +
           " (o.mobileNumber = :identifier AND o.type = :type)) " +
           "AND o.createdAt >= :since")
    long countRecentOtps(@Param("identifier") String identifier,
                         @Param("type") String type,
                         @Param("since") OffsetDateTime since);
}
