package com.gym.authservice.service;

import com.gym.authservice.client.MemberServiceClient;
import com.gym.authservice.dto.AuthResponse;
import com.gym.authservice.dto.LoginRequest;
import com.gym.authservice.dto.CompletePasswordResetRequest;
import com.gym.authservice.dto.MemberRegistrationMatch;
import com.gym.authservice.dto.RegistrationCodeResponse;
import com.gym.authservice.entity.Account;
import com.gym.authservice.entity.MemberRegistrationChallenge;
import com.gym.authservice.entity.PasswordResetChallenge;
import com.gym.authservice.entity.EmailChangeChallenge;
import com.gym.authservice.repository.AccountRepository;
import com.gym.authservice.repository.EmailChangeChallengeRepository;
import com.gym.authservice.repository.MemberRegistrationChallengeRepository;
import com.gym.authservice.repository.PasswordResetChallengeRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MemberServiceClient memberServiceClient;
    private final MemberRegistrationChallengeRepository challengeRepository;
    private final PasswordResetChallengeRepository passwordResetChallengeRepository;
    private final EmailChangeChallengeRepository emailChangeChallengeRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailHost;
    private final String mailFrom;
    private final int codeExpirationMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, MemberServiceClient memberServiceClient,
            MemberRegistrationChallengeRepository challengeRepository,
            PasswordResetChallengeRepository passwordResetChallengeRepository,
            EmailChangeChallengeRepository emailChangeChallengeRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.registration.code-from:}") String mailFrom,
            @Value("${app.registration.code-expiration-minutes:10}") int codeExpirationMinutes) {
        this.accountRepository=accountRepository; this.passwordEncoder=passwordEncoder;
        this.jwtService=jwtService; this.memberServiceClient=memberServiceClient;
        this.challengeRepository=challengeRepository; this.mailSenderProvider=mailSenderProvider;
        this.passwordResetChallengeRepository=passwordResetChallengeRepository;
        this.emailChangeChallengeRepository=emailChangeChallengeRepository;
        this.mailHost=mailHost; this.mailFrom=mailFrom; this.codeExpirationMinutes=codeExpirationMinutes;
    }

    public AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, MemberServiceClient memberServiceClient,
            MemberRegistrationChallengeRepository challengeRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider, String mailHost, String mailFrom,
            int codeExpirationMinutes) {
        this(accountRepository, passwordEncoder, jwtService, memberServiceClient, challengeRepository,
                null, null, mailSenderProvider, mailHost, mailFrom, codeExpirationMinutes);
    }

    public RegistrationCodeResponse requestEmailChangeCode(Jwt jwt, String rawNewEmail) {
        Account account = requireMemberAccount(jwt);
        String newEmail = rawNewEmail.trim().toLowerCase(Locale.ROOT);
        if (newEmail.equalsIgnoreCase(account.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a different email address.");
        }
        if (accountRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already used by another account.");
        }
        try {
            MemberRegistrationMatch existingMember = memberServiceClient.findMemberForRegistration(newEmail);
            if (!account.getMemberId().equals(existingMember.id())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "That email is already linked to another gym member.");
            }
        } catch (IllegalStateException exception) {
            if (downstreamStatus(exception) != 404) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "We could not check whether that email is available. Please try again.");
            }
        }
        if (emailChangeChallengeRepository == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email change verification is unavailable.");
        }
        if (mailHost == null || mailHost.isBlank() || mailFrom == null || mailFrom.isBlank()
                || mailSenderProvider.getIfAvailable() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email verification is not configured yet. Contact the gym administrator.");
        }
        Instant now = Instant.now();
        EmailChangeChallenge previous = emailChangeChallengeRepository.findById(account.getId()).orElse(null);
        if (previous != null && previous.getCreatedAt().isAfter(now.minusSeconds(60))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait one minute before requesting another verification code.");
        }
        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(newEmail);
            message.setSubject("Form & Fit email change verification");
            message.setText("Your Form & Fit email change code is " + code
                    + ". It expires in " + codeExpirationMinutes
                    + " minutes. If you did not request this, ignore this email.");
            mailSenderProvider.getObject().send(message);
        } catch (RuntimeException exception) {
            Throwable rootCause = exception;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) rootCause = rootCause.getCause();
            log.warn("Could not send email change verification; mailError={}, rootCause={}",
                    exception.getClass().getSimpleName(), rootCause.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "We could not send the verification email. Please try again later.");
        }
        emailChangeChallengeRepository.save(new EmailChangeChallenge(account.getId(), newEmail,
                passwordEncoder.encode(code), now.plus(Duration.ofMinutes(codeExpirationMinutes))));
        return new RegistrationCodeResponse(maskEmail(newEmail), "A verification code was sent to your new email address.");
    }

    @Transactional
    public AuthResponse completeEmailChange(Jwt jwt, String rawNewEmail, String code) {
        Account account = requireMemberAccount(jwt);
        String newEmail = rawNewEmail.trim().toLowerCase(Locale.ROOT);
        if (newEmail.equalsIgnoreCase(account.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a different email address.");
        }
        EmailChangeChallenge challenge = emailChangeChallengeRepository.findById(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Request an email verification code before changing your email."));
        if (!challenge.getNewEmail().equalsIgnoreCase(newEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The code was sent for a different email. Request a new code.");
        }
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            emailChangeChallengeRepository.delete(challenge);
            throw new ResponseStatusException(HttpStatus.GONE, "The verification code expired. Request a new code.");
        }
        if (challenge.getAttempts() >= 5) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many incorrect codes. Request a new verification code.");
        }
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            emailChangeChallengeRepository.save(challenge);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "That verification code is incorrect. Check the email and try again.");
        }
        if (accountRepository.existsByEmailIgnoreCase(newEmail)) {
            emailChangeChallengeRepository.delete(challenge);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already used by another account.");
        }

        try {
            account.setEmail(newEmail);
            accountRepository.saveAndFlush(account);
            memberServiceClient.replaceMemberEmail(account.getMemberId(), newEmail);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already used by another account.");
        } catch (IllegalStateException exception) {
            if (exception.getCause() instanceof RestClientResponseException response
                    && response.getStatusCode().value() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "That email is already linked to another gym member.");
            }
            log.warn("Could not synchronize verified member email; errorType={}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "We could not update the gym profile email. Please try again later.");
        }
        emailChangeChallengeRepository.delete(challenge);
        return issueResponse(account);
    }

    private Account requireMemberAccount(Jwt jwt) {
        if (jwt == null || !"MEMBER".equalsIgnoreCase(jwt.getClaimAsString("role"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in as a member to change your email.");
        }
        Object accountId = jwt.getClaim("accountId");
        if (!(accountId instanceof Number id) || id.longValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your member session is invalid. Sign in again.");
        }
        Account account = accountRepository.findById(id.longValue())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your member session is no longer valid."));
        if (!"MEMBER".equals(account.getRole()) || account.getMemberId() == null
                || !account.getEmail().equalsIgnoreCase(jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your member session is no longer valid. Sign in again.");
        }
        return account;
    }

    public RegistrationCodeResponse requestMemberRegistrationCode(String identifier) {
        MemberRegistrationMatch member = findMember(identifier);
        if (member.email() == null || member.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Your gym profile does not have an email address. Contact the gym administrator to add one.");
        }
        String email = member.email().trim().toLowerCase(Locale.ROOT);
        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A member account already exists. Please sign in.");
        }
        if (mailHost == null || mailHost.isBlank() || mailFrom == null || mailFrom.isBlank()
                || mailSenderProvider.getIfAvailable() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email verification is not configured yet. Contact the gym administrator.");
        }

        Instant now = Instant.now();
        MemberRegistrationChallenge previous = challengeRepository.findById(email).orElse(null);
        if (previous != null && previous.getCreatedAt().isAfter(now.minusSeconds(60))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait one minute before requesting another verification code.");
        }
        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("Form & Fit member account verification");
            message.setText("Your Form & Fit verification code is " + code
                    + ". It expires in " + codeExpirationMinutes + " minutes. If you did not request this, ignore this email.");
            mailSender.send(message);
        } catch (RuntimeException exception) {
            Throwable rootCause = exception;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            // Log exception types only: SMTP exception messages may contain account details.
            log.warn("Could not send member registration verification email; mailError={}, rootCause={}",
                    exception.getClass().getSimpleName(), rootCause.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "We could not send the verification email. Please try again later.");
        }
        challengeRepository.save(new MemberRegistrationChallenge(email, member.id(),
                passwordEncoder.encode(code), now.plus(Duration.ofMinutes(codeExpirationMinutes))));
        return new RegistrationCodeResponse(maskEmail(email),
                "A verification code was sent to the email on your gym profile.");
    }

    public AuthResponse completeMemberRegistration(String identifier, String code, String password) {
        MemberRegistrationMatch member = findMember(identifier);
        String email = member.email() == null ? "" : member.email().trim().toLowerCase(Locale.ROOT);
        MemberRegistrationChallenge challenge = challengeRepository.findById(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Request an email verification code before creating your account."));
        if (!challenge.getMemberId().equals(member.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The verification code does not match this gym profile. Request a new code.");
        }
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            challengeRepository.delete(challenge);
            throw new ResponseStatusException(HttpStatus.GONE,
                    "The verification code expired. Request a new code.");
        }
        if (challenge.getAttempts() >= 5) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many incorrect codes. Request a new verification code.");
        }
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            challengeRepository.save(challenge);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "That verification code is incorrect. Check the email and try again.");
        }
        if (accountRepository.existsByEmailIgnoreCase(email)) {
            challengeRepository.delete(challenge);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A member account already exists. Please sign in.");
        }
        Account account = new Account(member.name(), email, passwordEncoder.encode(password), "MEMBER");
        account.setMemberId(member.id());
        account.setStatus("ACTIVE");
        try {
            account = accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A member account already exists. Please sign in.");
        }
        challengeRepository.delete(challenge);
        return issueResponse(account);
    }

    public RegistrationCodeResponse requestPasswordResetCode(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        Account account = accountRepository.findByEmailIgnoreCase(email).orElse(null);
        // Use the same response whether an account exists or not, to avoid exposing registered emails.
        if (account == null) {
            return new RegistrationCodeResponse("", "If an account exists for that email, a verification code has been sent.");
        }
        if (mailHost == null || mailHost.isBlank() || mailFrom == null || mailFrom.isBlank()
                || mailSenderProvider.getIfAvailable() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email verification is not configured yet. Contact the gym administrator.");
        }
        Instant now = Instant.now();
        PasswordResetChallenge previous = passwordResetChallengeRepository.findById(email).orElse(null);
        if (previous != null && previous.getCreatedAt().isAfter(now.minusSeconds(60))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait one minute before requesting another verification code.");
        }
        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("Form & Fit password reset code");
            message.setText("Your Form & Fit password reset code is " + code
                    + ". It expires in " + codeExpirationMinutes + " minutes. If you did not request this, ignore this email.");
            mailSenderProvider.getObject().send(message);
        } catch (RuntimeException exception) {
            Throwable rootCause = exception;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) rootCause = rootCause.getCause();
            log.warn("Could not send password reset email; mailError={}, rootCause={}",
                    exception.getClass().getSimpleName(), rootCause.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "We could not send the verification email. Please try again later.");
        }
        passwordResetChallengeRepository.save(new PasswordResetChallenge(email,
                passwordEncoder.encode(code), now.plus(Duration.ofMinutes(codeExpirationMinutes))));
        return new RegistrationCodeResponse(maskEmail(email),
                "If an account exists for that email, a verification code has been sent.");
    }

    public void completePasswordReset(CompletePasswordResetRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        PasswordResetChallenge challenge = passwordResetChallengeRepository.findById(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Request a password reset code before resetting your password."));
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            passwordResetChallengeRepository.delete(challenge);
            throw new ResponseStatusException(HttpStatus.GONE, "The verification code expired. Request a new code.");
        }
        if (challenge.getAttempts() >= 5) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many incorrect codes. Request a new verification code.");
        }
        if (!passwordEncoder.matches(request.code(), challenge.getCodeHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            passwordResetChallengeRepository.save(challenge);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "That verification code is incorrect. Check the email and try again.");
        }
        Account account = accountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Request a new password reset code and try again."));
        account.setPassword(passwordEncoder.encode(request.password()));
        accountRepository.saveAndFlush(account);
        passwordResetChallengeRepository.delete(challenge);
    }

    private MemberRegistrationMatch findMember(String identifier) {
        try {
            return memberServiceClient.findMemberForRegistration(identifier);
        } catch (RuntimeException exception) {
            if (downstreamStatus(exception) == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "You are not registered with this gym. Contact the gym administration to join.");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "We could not check your gym registration. Please try again later.");
        }
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + email.substring(at);
    }

    private int downstreamStatus(Throwable error) {
        for (Throwable cause=error; cause!=null; cause=cause.getCause()) {
            if (cause instanceof RestClientResponseException response) return response.getStatusCode().value();
        }
        return 0;
    }

    public AuthResponse login(LoginRequest request) {
        String email=request.getEmail().trim().toLowerCase();
        Account account=accountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(()->new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(),account.getPassword()))
            throw new BadCredentialsException("Invalid email or password");
        if ("PENDING".equals(account.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Member profile setup is pending. Retry registration to complete it.");
        if ("MEMBER".equals(account.getRole()) && account.getMemberId()==null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Member account needs administrator profile-link review");
        return issueResponse(account);
    }

    private AuthResponse issueResponse(Account account) {
        String token=jwtService.generateToken(account);
        return new AuthResponse(token,account.getId(),account.getMemberId(),account.getName(),account.getEmail(),account.getRole());
    }
}
