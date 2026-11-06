package com.gym.memberservice.service;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
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
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(mailFrom, "Form & Fit Club");
        helper.setTo(email);
        helper.setSubject("A friendly check-in from Form & Fit");
        String memberName = safeName(name);
        String text = "FORM & FIT\n\n"
                + "A FRIENDLY CHECK-IN\n\n"
                + "We hope to see you tomorrow\n\n"
                + "Hi " + memberName + ", we didn’t see you at the gym today. If you couldn’t make it or needed a rest day, that’s okay. "
                + "We’ll be glad to welcome you back whenever you’re ready.\n\n"
                + "YOUR NEXT VISIT\n\nCome by tomorrow\n\n"
                + "Get back to your routine at your own pace. We look forward to seeing you.\n\n"
                + "Take care,\nThe Form & Fit Team";
        helper.setText(text, reminderHtml(memberName));
        helper.addInline("formFitGymLogo", new ClassPathResource("mail/form-fit-gym-logo.png"), "image/png");
        mailSender.send(message);
    }

    private String reminderHtml(String memberName) {
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:#f3f7f4;font-family:Arial,Helvetica,sans-serif;color:#173b2c;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:#f3f7f4;padding:32px 12px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:560px;background:#fff;border:1px solid #dce9df;border-radius:16px;overflow:hidden;\">"
                + "<tr><td align=\"center\" style=\"padding:16px 24px;background:#f4f8f5;border-bottom:1px solid #dce9df;\"><img src=\"cid:formFitGymLogo\" width=\"360\" alt=\"FORM &amp; FIT Fitness Club\" style=\"display:block;width:100%;max-width:360px;height:auto;border:0;\"></td></tr>"
                + "<tr><td style=\"padding:32px;\"><div style=\"font-size:12px;letter-spacing:1.5px;font-weight:700;color:#247951;\">A FRIENDLY CHECK-IN</div>"
                + "<h1 style=\"margin:12px 0 10px;font-size:25px;line-height:1.25;color:#173b2c;\">We hope to see you tomorrow</h1>"
                + "<p style=\"margin:0;font-size:15px;line-height:1.6;color:#607168;\">Hi " + escapeHtml(memberName)
                + ", we didn’t see you at the gym today. If you couldn’t make it or needed a rest day, that’s okay. We’ll be glad to welcome you back whenever you’re ready.</p>"
                + "<div style=\"margin:24px 0;padding:18px 20px;background:#eaf5ed;border:1px solid #d6e8da;border-radius:12px;\"><div style=\"font-size:11px;letter-spacing:1.4px;font-weight:700;color:#698174;\">YOUR NEXT VISIT</div>"
                + "<div style=\"margin-top:6px;font-size:23px;font-weight:700;color:#176342;\">Come by tomorrow</div>"
                + "<p style=\"margin:8px 0 0;font-size:14px;line-height:1.6;color:#607168;\">Get back to your routine at your own pace. We look forward to seeing you.</p></div>"
                + "<p style=\"margin:22px 0 0;font-size:13px;line-height:1.6;color:#607168;\">Take care,<br><strong>The Form &amp; Fit Team</strong></p></td></tr>"
                + "<tr><td style=\"padding:18px 32px;border-top:1px solid #edf1ee;font-size:12px;line-height:1.5;color:#839087;\">Form &amp; Fit Gym Management<br>A friendly attendance note from your club.</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String safeName(String name) {
        return name == null || name.isBlank() ? "Member" : name.trim();
    }
}
