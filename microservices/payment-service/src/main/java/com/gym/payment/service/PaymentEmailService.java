package com.gym.payment.service;

import com.gym.payment.client.MemberDirectoryClient;
import com.gym.payment.entity.Payment;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class PaymentEmailService {
    private static final Logger log = LoggerFactory.getLogger(PaymentEmailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private final PaymentOrderPersistenceService persistenceService;
    private final MemberDirectoryClient memberDirectoryClient;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailHost;
    private final String mailFrom;

    public PaymentEmailService(
            PaymentOrderPersistenceService persistenceService,
            MemberDirectoryClient memberDirectoryClient,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.mail.from:}") String mailFrom) {
        this.persistenceService = persistenceService;
        this.memberDirectoryClient = memberDirectoryClient;
        this.mailSenderProvider = mailSenderProvider;
        this.mailHost = mailHost;
        this.mailFrom = mailFrom;
    }

    public void sendReceiptIfNeeded(Long paymentId) {
        if (paymentId == null || mailHost == null || mailHost.isBlank()
                || mailFrom == null || mailFrom.isBlank()
                || mailSenderProvider.getIfAvailable() == null) {
            log.warn("Payment receipt email skipped because SMTP settings are not configured; paymentId={}", paymentId);
            return;
        }

        try {
            Payment payment = persistenceService.claimReceiptEmail(paymentId);
            if (payment == null) return;
            Map<Long, MemberDirectoryClient.MemberProfile> profiles = memberDirectoryClient.getProfiles(java.util.List.of(payment.getMemberId()));
            MemberDirectoryClient.MemberProfile member = profiles.get(payment.getMemberId());
            if (member == null || member.email() == null || member.email().isBlank()) {
                log.warn("Payment receipt email skipped because the member profile has no email; paymentId={}, memberId={}",
                        paymentId, payment.getMemberId());
                return;
            }

            sendReceipt(payment, member);
            persistenceService.markReceiptEmailSent(paymentId);
        } catch (Exception exception) {
            Throwable rootCause = exception;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) rootCause = rootCause.getCause();
            log.warn("Could not send payment receipt email; paymentId={}, mailError={}, rootCause={}",
                    paymentId, exception.getClass().getSimpleName(), rootCause.getClass().getSimpleName());
        }
    }

    private void sendReceipt(Payment payment, MemberDirectoryClient.MemberProfile member) throws Exception {
        JavaMailSender mailSender = mailSenderProvider.getObject();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(mailFrom, "Form & Fit");
        helper.setTo(member.email());
        helper.setSubject("Payment confirmed | Form & Fit");

        String memberName = safeName(member.name());
        String amount = BigDecimal.valueOf(payment.getAmountPaise(), 2).setScale(2).toPlainString();
        String planName = displayPlan(payment.getPlanName());
        String coverage = formatDate(payment.getMembershipStartDate()) + " – " + formatDate(payment.getMembershipEndDate());
        String text = "FORM & FIT\n\n"
                + "Your membership payment is confirmed, " + memberName + ".\n\n"
                + "Plan: " + planName + "\nAmount paid: ₹" + amount + " " + payment.getCurrency() + "\n"
                + "Membership valid: " + coverage + "\nPayment reference: " + payment.getRazorpayPaymentId() + "\n\n"
                + "Your payment was verified by Form & Fit. Keep this email as your receipt.\n\n"
                + "Form & Fit Gym Management — Automated receipt, please do not reply.";

        helper.setText(text, receiptHtml(memberName, planName, amount, payment.getCurrency(), coverage,
                payment.getRazorpayPaymentId(), payment.getId()));
        helper.addInline("formFitGymLogo", new ClassPathResource("mail/form-fit-gym-logo.png"), "image/png");
        mailSender.send(message);
    }

    private String receiptHtml(String memberName, String planName, String amount, String currency,
            String coverage, String paymentReference, Long receiptNumber) {
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:#f3f7f4;font-family:Arial,Helvetica,sans-serif;color:#173b2c;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:#f3f7f4;padding:32px 12px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:560px;background:#fff;border:1px solid #dce9df;border-radius:16px;overflow:hidden;\">"
                + "<tr><td align=\"center\" style=\"padding:16px 24px;background:#f4f8f5;border-bottom:1px solid #dce9df;\"><img src=\"cid:formFitGymLogo\" width=\"360\" alt=\"FORM &amp; FIT Fitness Club\" style=\"display:block;width:100%;max-width:360px;height:auto;border:0;\"></td></tr>"
                + "<tr><td style=\"padding:32px;\"><div style=\"font-size:12px;letter-spacing:1.5px;font-weight:700;color:#247951;\">PAYMENT RECEIPT</div>"
                + "<h1 style=\"margin:12px 0 10px;font-size:25px;line-height:1.25;color:#173b2c;\">Payment confirmed</h1>"
                + "<p style=\"margin:0;font-size:15px;line-height:1.6;color:#607168;\">Thank you, " + escapeHtml(memberName)
                + ". Your membership payment has been verified and your plan is active.</p>"
                + "<div style=\"margin:24px 0;padding:18px 20px;background:#eaf5ed;border:1px solid #d6e8da;border-radius:12px;\"><div style=\"font-size:11px;letter-spacing:1.4px;font-weight:700;color:#698174;\">AMOUNT PAID</div>"
                + "<div style=\"margin-top:6px;font-size:30px;font-weight:700;color:#176342;\">₹" + amount + " " + escapeHtml(currency) + "</div></div>"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"font-size:14px;line-height:1.5;\">"
                + detailRow("Membership plan", planName) + detailRow("Active period", coverage)
                + detailRow("Receipt number", "FF-" + receiptNumber) + detailRow("Razorpay payment ID", paymentReference)
                + "</table><p style=\"margin:22px 0 0;font-size:13px;line-height:1.6;color:#607168;\">Please keep this email for your records. Your membership dates are shown above.</p></td></tr>"
                + "<tr><td style=\"padding:18px 32px;border-top:1px solid #edf1ee;font-size:12px;line-height:1.5;color:#839087;\">Form &amp; Fit Gym Management<br>Automated receipt — please do not reply.</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String detailRow(String label, String value) {
        return "<tr><td style=\"padding:10px 0;border-bottom:1px solid #edf1ee;color:#718278;\">" + escapeHtml(label)
                + "</td><td align=\"right\" style=\"padding:10px 0;border-bottom:1px solid #edf1ee;color:#173b2c;font-weight:700;word-break:break-word;\">"
                + escapeHtml(value) + "</td></tr>";
    }

    private String formatDate(java.time.LocalDate value) {
        return value == null ? "your active membership period" : DATE_FORMAT.format(value);
    }

    private String displayPlan(String plan) {
        if (plan == null || plan.isBlank()) return "Membership plan";
        String lower = plan.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String safeName(String name) {
        return name == null || name.isBlank() ? "Member" : name.trim();
    }

    private String escapeHtml(String value) {
        return value == null ? "—" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
