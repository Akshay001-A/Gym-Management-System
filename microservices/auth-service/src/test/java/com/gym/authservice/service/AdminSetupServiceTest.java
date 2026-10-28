package com.gym.authservice.service;

import com.gym.authservice.config.AdminSetupConfig;
import com.gym.authservice.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminSetupServiceTest {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final AdminSetupConfig config = new AdminSetupConfig();
    private final AdminSetupService service = new AdminSetupService(accounts, passwords, config);

    @Test void disabledSetupReturnsForbidden() {
        ReflectionTestUtils.setField(config, "enabled", false);
        ReflectionTestUtils.setField(config, "secret", "");
        assertStatus(403, () -> service.createInitialAdmin("secret", "Admin", "admin@example.com", "long-password"));
    }
    @Test void wrongSetupSecretReturnsForbidden() {
        enableSetup();
        assertStatus(403, () -> service.createInitialAdmin("wrong", "Admin", "admin@example.com", "long-password"));
    }
    @Test void duplicateEmailReturnsConflict() {
        enableSetup();
        when(accounts.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);
        assertStatus(409, () -> service.createInitialAdmin("correct", "Admin", "admin@example.com", "long-password"));
    }
    private void enableSetup() {
        ReflectionTestUtils.setField(config, "enabled", true);
        ReflectionTestUtils.setField(config, "secret", "correct");
    }
    private void assertStatus(int expected, Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(expected, exception.getStatusCode().value());
    }
}
