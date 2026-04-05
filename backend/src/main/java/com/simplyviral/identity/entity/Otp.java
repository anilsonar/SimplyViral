package com.simplyviral.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email")
    private String email;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // 'EMAIL' or 'MOBILE'

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used")
    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
