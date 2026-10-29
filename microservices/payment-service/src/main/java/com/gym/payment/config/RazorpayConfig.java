
package com.gym.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @Bean
    public RazorpayCredentials razorpayCredentials() {
        return new RazorpayCredentials(
                keyId,
                keySecret,
                webhookSecret
        );
    }

    public record RazorpayCredentials(
            String keyId,
            String keySecret,
            String webhookSecret
    ) {
    }
}
