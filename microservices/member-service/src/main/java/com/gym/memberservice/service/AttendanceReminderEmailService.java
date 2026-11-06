package com.gym.memberservice.service;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class AttendanceReminderEmailService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailHost;
    private final String mailFrom;

    public AttendanceReminderEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.mail.from:}") String mailFrom) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailHost = mailHost;
        this.mailFrom = mailFrom;
    }

    public boolean isConfigured() {
        return mailHost != null && !mailHost.isBlank()
                && mailFrom != null && !mailFrom.isBlank()
                && mailSenderProvider.getIfAvailable() != null;
    }

    public void sendReminder(String email, String name) throws Exception {
        if (!isConfigured()) throw new IllegalStateException("Attendance reminder email is not configured");

        JavaMailSender mailSender = mailSenderProvider.getObject();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(mailFrom, "Form & Fit Club");
        helper.setTo(email);
        helper.setSubject("We'd love to see you at Form & Fit");
        helper.setText("Hi " + safeName(name) + ",\n\n"
                + "We missed seeing you at Form & Fit yesterday. If you needed a rest day or couldn't make it, that's okay. "
                + "When it works for you, we'd be happy to welcome you back. Small, steady steps can help build a routine.\n\n"
                + "Take care,\nForm & Fit Club\n\n"
                + "This is an automated attendance reminder from your gym.", false);
        mailSender.send(message);
    }

    private String safeName(String name) {
        return name == null || name.isBlank() ? "there" : name.trim();
    }
}
