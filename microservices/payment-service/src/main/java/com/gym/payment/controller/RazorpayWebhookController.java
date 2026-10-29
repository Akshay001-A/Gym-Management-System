package com.gym.payment.controller;

import com.gym.payment.config.RazorpayConfig.RazorpayCredentials;
import com.gym.payment.dto.PaymentWebhookRequest;
import com.gym.payment.service.RazorpayWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/payments/webhooks")
public class RazorpayWebhookController {

    private final RazorpayCredentials credentials;
    private final RazorpayWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public RazorpayWebhookController(
            RazorpayCredentials credentials,
            RazorpayWebhookService webhookService,
            ObjectMapper objectMapper
    ) {
        this.credentials = credentials;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody byte[] rawBody,
            @RequestHeader(
                    value = "X-Razorpay-Signature",
                    required = false
            ) String signature,
            @RequestHeader(
                    value = "X-Razorpay-Event-Id",
                    required = false
            ) String eventId
    ) {
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing webhook signature");
        }

        if (!isValidSignature(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid webhook signature");
        }

        final PaymentWebhookRequest webhookRequest;

        try {
            webhookRequest = objectMapper.readValue(
                    rawBody,
                    PaymentWebhookRequest.class
            );
        } catch (JacksonException exception) {
            return ResponseEntity.badRequest()
                    .body("Invalid webhook payload");
        }

        try {
            webhookService.processWebhook(webhookRequest, eventId);
            return ResponseEntity.ok("Webhook received");
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body("Invalid webhook event");
        }
    }

    private boolean isValidSignature(
            byte[] rawBody,
            String suppliedSignature
    ) {
        String webhookSecret = credentials.webhookSecret();

        if (webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(secretKey);

            byte[] expectedSignature = mac.doFinal(rawBody);
            byte[] actualSignature =
                    HexFormat.of().parseHex(suppliedSignature);

            return MessageDigest.isEqual(
                    expectedSignature,
                    actualSignature
            );
        } catch (Exception exception) {
            return false;
        }
    }
}
