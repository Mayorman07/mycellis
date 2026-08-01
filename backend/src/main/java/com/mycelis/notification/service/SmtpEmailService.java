package com.mycelis.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import jakarta.mail.MessagingException;

@Slf4j
@Service
@Profile({"dev", "local"})
@ConditionalOnProperty(name = "mycelis.email.provider", havingValue = "smtp", matchIfMissing = true)
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${mycelis.mail.from}")
    private String fromAddress;

    @Value("${mycelis.mail.from-name}")
    private String fromName;

    @Override
    @Async("emailTaskExecutor")
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email sent to {} | subject='{}'", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email to {} | subject='{}'", to, subject, e);
            // Swallow for dev: we don't want email failures to crash callers.
            // In production, push to a dead-letter queue for retry.
        }
    }
}