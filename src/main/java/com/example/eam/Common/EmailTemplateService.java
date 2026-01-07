package com.example.eam.Common;

import java.time.Year;

import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final ResourceLoader resourceLoader;

    public String buildInviteEmail(String name, String companyName, String inviteLink) {
        try {
            Resource resource =
                    resourceLoader.getResource("classpath:templates/invite-user.html");

            String html = new String(resource.getInputStream().readAllBytes());

            return html
                    .replace("{{name}}", name)
                    .replace("{{companyName}}", companyName)
                    .replace("{{inviteLink}}", inviteLink)
                    .replace("{{year}}", String.valueOf(Year.now().getValue()));

        } catch (Exception e) {
            throw new RuntimeException("Failed to load invite email template", e);
        }
    }
}
