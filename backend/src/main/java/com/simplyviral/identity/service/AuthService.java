package com.simplyviral.identity.service;

import com.simplyviral.identity.entity.Otp;
import com.simplyviral.identity.entity.RefreshToken;
import com.simplyviral.identity.entity.User;
import com.simplyviral.identity.repository.OtpRepository;
import com.simplyviral.identity.repository.RefreshTokenRepository;
import com.simplyviral.identity.repository.UserRepository;
import com.simplyviral.shared.exception.SimplyViralException;
import com.simplyviral.identity.configuration.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles all authentication flows:
 * - Email + Password (register, login)
 * - OTP via Email or Mobile (passwordless signup/login)
 * - Refresh token rotation
 * - User profile retrieval
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final JwtTokenProvider jwtTokenProvider;

    // SecureRandom is cryptographically strong — never use java.util.Random for OTPs
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int REFRESH_TOKEN_EXPIRY_DAYS = 30;
    private static final int MAX_OTP_ATTEMPTS = 5;

    // ========================= Registration =========================

    @Transactional
    public Map<String, String> register(String email, String rawPassword, String mobileNumber) {
        if (email == null || email.isBlank()) {
            throw new SimplyViralException("Email is required for registration");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new SimplyViralException("Password is required for email registration");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new SimplyViralException("Email already registered");
        }
        if (mobileNumber != null && !mobileNumber.isBlank()
                && userRepository.findByMobileNumber(mobileNumber).isPresent()) {
            throw new SimplyViralException("Mobile number already registered");
        }

        User user = User.builder()
                .email(email)
                .mobileNumber(mobileNumber != null && !mobileNumber.isBlank() ? mobileNumber : null)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();
        userRepository.save(user);

        log.info("User registered: userId={}, email={}", user.getId(), email);
        return generateTokenPair(user);
    }

    // ========================= Login by Email + Password =========================

    @Transactional
    public Map<String, String> login(String email, String rawPassword) {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new SimplyViralException("Invalid credentials");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SimplyViralException("Invalid credentials"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new SimplyViralException("Invalid credentials");
        }

        log.info("User logged in via email+password: userId={}", user.getId());
        return generateTokenPair(user);
    }

    // ========================= OTP Send =========================

    @Transactional
    public void sendOtp(String identifier, String type) {
        if (identifier == null || identifier.isBlank()) {
            throw new SimplyViralException("Identifier (email or mobile) is required");
        }
        if (type == null || (!type.equalsIgnoreCase("EMAIL") && !type.equalsIgnoreCase("MOBILE"))) {
            throw new SimplyViralException("Invalid OTP type. Use EMAIL or MOBILE.");
        }

        // Rate limit: count recent OTPs (last 15 min) for this identifier
        long recentCount = otpRepository.countRecentOtps(identifier, type,
                OffsetDateTime.now().minusMinutes(15));
        if (recentCount >= MAX_OTP_ATTEMPTS) {
            throw new SimplyViralException("Too many OTP requests. Please wait before requesting again.");
        }

        String code = String.format("%06d", secureRandom.nextInt(999999));
        Otp otp = Otp.builder()
                .otpCode(code)
                .type(type.toUpperCase())
                .expiresAt(OffsetDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();

        if ("EMAIL".equalsIgnoreCase(type)) {
            otp.setEmail(identifier);
            otpRepository.save(otp);
            notificationService.sendEmailOtp(identifier, code);
        } else {
            otp.setMobileNumber(identifier);
            otpRepository.save(otp);
            notificationService.sendSmsOtp(identifier, code);
        }

        log.info("OTP sent: type={}, identifier={}", type, identifier);
    }

    // ========================= OTP Verify (Login / Signup) =========================

    @Transactional
    public Map<String, String> verifyOtp(String identifier, String type, String code) {
        if (identifier == null || type == null || code == null) {
            throw new SimplyViralException("Identifier, type, and code are required");
        }

        Optional<Otp> otpOpt;
        if ("EMAIL".equalsIgnoreCase(type)) {
            otpOpt = otpRepository.findTopByEmailAndTypeOrderByCreatedAtDesc(identifier, type.toUpperCase());
        } else {
            otpOpt = otpRepository.findTopByMobileNumberAndTypeOrderByCreatedAtDesc(identifier, type.toUpperCase());
        }

        Otp otp = otpOpt.orElseThrow(() -> new SimplyViralException("No OTP requested for this identifier"));

        if (Boolean.TRUE.equals(otp.getUsed())) {
            throw new SimplyViralException("OTP already used");
        }

        if (OffsetDateTime.now().isAfter(otp.getExpiresAt())) {
            throw new SimplyViralException("OTP expired");
        }

        if (!otp.getOtpCode().equals(code)) {
            throw new SimplyViralException("Invalid OTP");
        }

        otp.setUsed(true);
        otpRepository.save(otp);

        // Fetch or create user (OTP acts as both signup and login)
        User user;
        if ("EMAIL".equalsIgnoreCase(type)) {
            user = userRepository.findByEmail(identifier).orElseGet(() -> {
                User newUser = User.builder().email(identifier).isEmailVerified(true).build();
                return userRepository.save(newUser);
            });
            user.setIsEmailVerified(true);
        } else {
            user = userRepository.findByMobileNumber(identifier).orElseGet(() -> {
                // Mobile-only users get a system-generated placeholder email
                String placeholderEmail = "mobile_" + UUID.randomUUID().toString().substring(0, 8) + "@simplyviral.app";
                User newUser = User.builder()
                        .email(placeholderEmail)
                        .mobileNumber(identifier)
                        .isMobileVerified(true)
                        .build();
                return userRepository.save(newUser);
            });
            user.setIsMobileVerified(true);
        }

        userRepository.save(user);

        log.info("OTP verified for user: userId={}, type={}", user.getId(), type);
        return generateTokenPair(user);
    }

    // ========================= Refresh Token =========================

    @Transactional
    public Map<String, String> refreshAccessToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new SimplyViralException("Refresh token is required");
        }

        RefreshToken existing = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new SimplyViralException("Invalid or expired refresh token"));

        if (OffsetDateTime.now().isAfter(existing.getExpiresAt())) {
            // Auto-revoke expired token
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw new SimplyViralException("Refresh token expired. Please log in again.");
        }

        User user = existing.getUser();

        // Rotate: revoke old, issue new
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        log.info("Refresh token rotated for user: userId={}", user.getId());
        return generateTokenPair(user);
    }

    // ========================= User Profile =========================

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new SimplyViralException("User not found"));
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new SimplyViralException("User not found"));
    }

    // ========================= Token Pair Generation =========================

    /**
     * Generates both an access token (short-lived JWT) and a refresh token
     * (long-lived, stored in DB for rotation).
     */
    private Map<String, String> generateTokenPair(User user) {
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        // Create and persist a new refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(OffsetDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);

        return Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken.getToken(),
                "user_id", user.getId().toString()
        );
    }
}
