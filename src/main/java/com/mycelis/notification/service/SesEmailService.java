package com.mycelis.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * AWS SES implementation — to be wired when prod deploy is ready.
 *
 * Will use AWS SDK v2 with `software.amazon.awssdk:ses` and read credentials
 * via the default credential provider chain (IAM role on EC2/ECS, env vars
 * locally, etc.).
 *
 * Lives here as a profile-gated placeholder so the prod path is wired
 * end-to-end and the prod context starts without missing-bean errors.
 */
@Slf4j
@Service
@Profile("prod")
public class SesEmailService implements EmailService {

    @Override
    public void send(String to, String subject, String htmlBody) {
        log.warn("SesEmailService.send not yet implemented — to={} subject={}", to, subject);
//        TODO: implement with SesV2Client when AWS SES is provisioned
    }
}