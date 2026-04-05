package com.simplyviral.identity.controller;

import com.simplyviral.shared.dto.ApiResult;
import org.springframework.web.bind.annotation.*;
import com.simplyviral.identity.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.Data;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/status")
    public ApiResult<String> getAuthStatus() {
        return ApiResult.success("Authentication system is up, OTP and OAuth integrations enabled.");
    }
    
    @PostMapping("/register")
    public ApiResult<String> register(@RequestBody RegisterReq req) {
        return ApiResult.success(authService.register(req.getEmail(), req.getPassword(), req.getMobileNumber()));
    }

    @PostMapping("/login")
    public ApiResult<String> login(@RequestBody LoginReq req) {
        return ApiResult.success(authService.login(req.getEmail(), req.getPassword()));
    }

    @PostMapping("/otp/send")
    public ApiResult<String> sendOtp(@RequestBody OtpSendReq req) {
        authService.sendOtp(req.getIdentifier(), req.getType());
        return ApiResult.success("OTP sent successfully");
    }

    @PostMapping("/otp/verify")
    public ApiResult<String> verifyOtp(@RequestBody OtpVerifyReq req) {
        return ApiResult.success(authService.verifyOtp(req.getIdentifier(), req.getType(), req.getCode()));
    }

    @Data
    public static class RegisterReq {
        private String email;
        private String password;
        private String mobileNumber;
    }

    @Data
    public static class LoginReq {
        private String email;
        private String password;
    }

    @Data
    public static class OtpSendReq {
        private String identifier; // email or mobile
        private String type; // EMAIL or MOBILE
    }

    @Data
    public static class OtpVerifyReq {
        private String identifier;
        private String type;
        private String code;
    }
}
