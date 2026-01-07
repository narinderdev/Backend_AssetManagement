package com.example.eam.Common;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final Environment environment;

    @Value("${app.mail.enforce-credentials:true}")
    private boolean enforceMailCredentials;

    private String fromEmail;

    /**
     * Runs once when the application starts
     * Verifies that mail username & password are loaded
     */
    @PostConstruct
    public void verifyMailConfiguration() {
        String username = environment.getProperty("spring.mail.username");
        String password = environment.getProperty("spring.mail.password");

        log.info("Mail username present: {}", username != null && !username.isBlank());
        log.info("Mail password present: {}", password != null && !password.isBlank());

        if ((username == null || username.isBlank()) || (password == null || password.isBlank())) {
            if (enforceMailCredentials) {
                throw new IllegalStateException("spring.mail.username/password are NOT configured");
            } else {
                log.warn("Mail credentials missing; email features (OTP/invite) will fail until configured.");
                return;
            }
        }

        // store for reuse
        this.fromEmail = username;

        log.info("Mail will be sent FROM: {}", this.fromEmail);
    }

    public void sendWithAttachment(String to, String subject, String html, byte[] pdf) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("Mail is not configured; set spring.mail.username/password or set app.mail.enforce-credentials=true with valid credentials.");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            if (pdf != null) {
                helper.addAttachment(
                        "invoice.pdf",
                        new ByteArrayResource(pdf)
                );
            }

            mailSender.send(message);

            log.info("Email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}

