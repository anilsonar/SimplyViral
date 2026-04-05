package com.simplyviral.identity.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    private final JavaMailSender mailSender;
    private final String twilioFromNumber;
    private final boolean isTwilioConfigured;

    public NotificationService(
            JavaMailSender mailSender,
            @Value("${simplyviral.twilio.account-sid:}") String twilioSid,
            @Value("${simplyviral.twilio.auth-token:}") String twilioToken,
            @Value("${simplyviral.twilio.from-number:}") String twilioFromNumber) {
        this.mailSender = mailSender;
        this.twilioFromNumber = twilioFromNumber;

        if (twilioSid != null && !twilioSid.isEmpty() && !twilioSid.equals("mock_sid")) {
            Twilio.init(twilioSid, twilioToken);
            this.isTwilioConfigured = true;
            log.info("Twilio initialized for SMS.");
        } else {
            this.isTwilioConfigured = false;
            log.info("Twilio not configured. SMS will be mocked.");
        }
    }

    public void sendEmailOtp(String toAddress, String code) {
        log.info("Sending Email OTP to {}: {}", toAddress, code);
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toAddress);
            msg.setSubject("Your Simply Viral Login Code");
            msg.setText("Your OTP code is: " + code + "\n\nIt expires in 10 minutes.");
            mailSender.send(msg);
            log.info("Email OTP sent successfully.");
        } catch (Exception e) {
            log.error("Failed to send Email OTP. Ensure SMTP server is running. Mocking success for demo.", e);
        }
    }

    public void sendSmsOtp(String toNumber, String code) {
        log.info("Sending SMS OTP to {}: {}", toNumber, code);
        if (isTwilioConfigured) {
            try {
                Message message = Message.creator(
                        new PhoneNumber(toNumber),
                        new PhoneNumber(twilioFromNumber),
                        "Your Simply Viral OTP code is: " + code
                ).create();
                log.info("SMS OTP sent successfully via Twilio (SID: {})", message.getSid());
            } catch (Exception e) {
                log.error("Failed to send SMS via Twilio", e);
            }
        } else {
            log.warn("Twilio disabled. MOCK SMS Sent to {}: {}", toNumber, code);
        }
    }
}
