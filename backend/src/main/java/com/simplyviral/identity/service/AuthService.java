package com.simplyviral.identity.service;

import com.simplyviral.identity.entity.Otp;
import com.simplyviral.identity.entity.User;
import com.simplyviral.identity.repository.OtpRepository;
import com.simplyviral.identity.repository.UserRepository;
import com.simplyviral.shared.exception.SimplyViralException;
import com.simplyviral.identity.configuration.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final Random random = new Random();

    @Transactional
    public String register(String email, String rawPassword, String mobileNumber) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new SimplyViralException("Email already registered");
        }
        if (mobileNumber != null && userRepository.findByMobileNumber(mobileNumber).isPresent()) {
            throw new SimplyViralException("Mobile number already registered");
        }

        User user = User.builder()
                .email(email)
                .mobileNumber(mobileNumber)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();
        userRepository.save(user);

        return jwtTokenProvider.generateToken(email);
    }

    @Transactional
    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SimplyViralException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new SimplyViralException("Invalid credentials");
        }

        return jwtTokenProvider.generateToken(email);
    }

    @Transactional
    public void sendOtp(String identifier, String type) {
        String code = String.format("%06d", random.nextInt(999999));
        Otp otp = Otp.builder()
                .otpCode(code)
                .type(type)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();

        if ("EMAIL".equalsIgnoreCase(type)) {
            otp.setEmail(identifier);
            notificationService.sendEmailOtp(identifier, code);
        } else if ("MOBILE".equalsIgnoreCase(type)) {
            otp.setMobileNumber(identifier);
            notificationService.sendSmsOtp(identifier, code);
        } else {
            throw new SimplyViralException("Invalid OTP type. Use EMAIL or MOBILE.");
        }

        otpRepository.save(otp);
    }

    @Transactional
    public String verifyOtp(String identifier, String type, String code) {
        Optional<Otp> otpOpt;
        if ("EMAIL".equalsIgnoreCase(type)) {
            otpOpt = otpRepository.findTopByEmailAndTypeOrderByCreatedAtDesc(identifier, type);
        } else {
            otpOpt = otpRepository.findTopByMobileNumberAndTypeOrderByCreatedAtDesc(identifier, type);
        }

        Otp otp = otpOpt.orElseThrow(() -> new SimplyViralException("No OTP requested"));

        if (otp.getUsed()) {
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

        // Fetch or create user
        User user;
        if ("EMAIL".equalsIgnoreCase(type)) {
            user = userRepository.findByEmail(identifier).orElseGet(() -> {
                User newUser = User.builder().email(identifier).isEmailVerified(true).build();
                return userRepository.save(newUser);
            });
            user.setIsEmailVerified(true);
        } else {
            user = userRepository.findByMobileNumber(identifier).orElseGet(() -> {
                // If mobile login and no email, generate placeholder email
                String placeholderEmail = identifier + "@mobile.verified";
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

        return jwtTokenProvider.generateToken(user.getEmail());
    }
}
