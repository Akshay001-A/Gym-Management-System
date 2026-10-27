
package com.gym.authservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminSetupConfig {

    @Value("${app.admin-setup.enabled:false}")
    private boolean enabled;

    @Value("${app.admin-setup.secret:}")
    private String secret;

    public boolean isEnabled() {
        return enabled;
    }

    public String getSecret() {
        return secret;
    }
}
