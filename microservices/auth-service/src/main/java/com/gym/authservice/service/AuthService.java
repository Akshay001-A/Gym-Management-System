
package com.gym.authservice.service;

import com.gym.authservice.client.MemberServiceClient;
import com.gym.authservice.dto.AuthResponse;
import com.gym.authservice.dto.LoginRequest;
import com.gym.authservice.dto.MemberProfileRequest;
import com.gym.authservice.dto.RegisterRequest;
import com.gym.authservice.entity.Account;
import com.gym.authservice.repository.AccountRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthService.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MemberServiceClient memberServiceClient;

    public AuthService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            MemberServiceClient memberServiceClient) {

        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.memberServiceClient = memberServiceClient;
    }

    public AuthResponse register(
            RegisterRequest request,
            MemberProfileRequest profileRequest) {

        String email = request.getEmail().trim().toLowerCase();

        // Check duplicate email in Auth Service.
        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists"
            );
        }

        // Account and profile emails must match.
        if (profileRequest.getEmail() == null
                || !email.equalsIgnoreCase(
                profileRequest.getEmail().trim())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account email and member profile email must match"
            );
        }

        Account account = new Account(
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                "MEMBER"
        );

        account = accountRepository.saveAndFlush(account);

        try {
            profileRequest.setName(request.getName().trim());
            profileRequest.setEmail(email);

            memberServiceClient.createMemberProfile(profileRequest);

        } catch (RuntimeException ex) {

            log.error(
                    "Member profile creation failed for account ID {}",
                    account.getId(),
                    ex
            );

            // Read the original response from Member Service.
            String downstreamMessage = "";
            Throwable cause = ex;

            while (cause != null) {
                if (cause instanceof RestClientResponseException responseEx) {
                    downstreamMessage =
                            responseEx.getResponseBodyAsString();
                    break;
                }
                cause = cause.getCause();
            }

            String error = downstreamMessage.toLowerCase();

            // Clean up the account if profile creation failed.
            try {
                accountRepository.deleteById(account.getId());
                accountRepository.flush();
            } catch (RuntimeException cleanupException) {
                log.error(
                        "Failed to clean up account ID {}",
                        account.getId(),
                        cleanupException
                );
            }

            // Handle duplicate mobile or phone number.
            if ((error.contains("mobile")
                    || error.contains("phone"))
                    && (error.contains("already registered")
                    || error.contains("already exists")
                    || error.contains("duplicate"))) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This mobile number is already registered"
                );
            }

            // Handle duplicate email reported by Member Service.
            if (error.contains("email")
                    && (error.contains("already registered")
                    || error.contains("already exists")
                    || error.contains("duplicate"))) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This email is already registered"
                );
            }

            // Other failures from Member Service.
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Member profile creation failed. Please check "
                            + "the submitted details and try again."
            );
        }

        try {
            String token = jwtService.generateToken(account);

            return new AuthResponse(
                    token,
                    account.getId(),
                    account.getName(),
                    account.getEmail(),
                    account.getRole()
            );

        } catch (RuntimeException ex) {
            log.error(
                    "JWT generation failed for account ID {}",
                    account.getId(),
                    ex
            );

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to complete registration"
            );
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        Account account = accountRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(
                request.getPassword(),
                account.getPassword())) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(account);

        return new AuthResponse(
                token,
                account.getId(),
                account.getName(),
                account.getEmail(),
                account.getRole()
        );
    }
}
