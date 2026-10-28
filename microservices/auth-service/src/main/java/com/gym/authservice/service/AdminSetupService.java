package com.gym.authservice.service;

import com.gym.authservice.config.AdminSetupConfig;
import com.gym.authservice.entity.Account;
import com.gym.authservice.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminSetupService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSetupConfig adminSetupConfig;

    public AdminSetupService(AccountRepository accountRepository, PasswordEncoder passwordEncoder,
            AdminSetupConfig adminSetupConfig) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminSetupConfig = adminSetupConfig;
    }

    @Transactional
    public Account createInitialAdmin(String secret, String name, String email, String password) {
        if (!adminSetupConfig.isEnabled() || adminSetupConfig.getSecret() == null
                || adminSetupConfig.getSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Initial admin setup is disabled");
        }
        if (secret == null || !java.security.MessageDigest.isEqual(
                adminSetupConfig.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid admin setup secret");
        }
        if (accountRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        if (accountRepository.existsByRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An admin account already exists");
        }
        Account admin = new Account(name.trim(), email.trim().toLowerCase(),
                passwordEncoder.encode(password), "ADMIN");
        return accountRepository.save(admin);
    }
}
