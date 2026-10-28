package com.gym.authservice.service;

import com.gym.authservice.client.MemberServiceClient;
import com.gym.authservice.dto.AuthResponse;
import com.gym.authservice.dto.LoginRequest;
import com.gym.authservice.dto.MemberProfileRequest;
import com.gym.authservice.dto.RegisterRequest;
import com.gym.authservice.entity.Account;
import com.gym.authservice.repository.AccountRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MemberServiceClient memberServiceClient;

    public AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, MemberServiceClient memberServiceClient) {
        this.accountRepository=accountRepository; this.passwordEncoder=passwordEncoder;
        this.jwtService=jwtService; this.memberServiceClient=memberServiceClient;
    }

    public AuthResponse register(RegisterRequest request, MemberProfileRequest profileRequest) {
        String email=request.getEmail().trim().toLowerCase();
        if (profileRequest.getEmail()==null || !email.equalsIgnoreCase(profileRequest.getEmail().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Account email and member profile email must match");
        }
        Account account=accountRepository.findByEmailIgnoreCase(email).orElse(null);
        if (account!=null) {
            boolean resumable="PENDING".equals(account.getStatus()) && account.getRegistrationKey()!=null
                    && passwordEncoder.matches(request.getPassword(),account.getPassword());
            if (!resumable) throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists or needs administrator review");
        } else {
            account=new Account(request.getName().trim(),email,passwordEncoder.encode(request.getPassword()),"MEMBER");
            account.setStatus("PENDING");
            account.setRegistrationKey(UUID.randomUUID().toString());
            try { account=accountRepository.saveAndFlush(account); }
            catch (DataIntegrityViolationException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists");
            }
        }

        profileRequest.setName(account.getName());
        profileRequest.setEmail(account.getEmail());
        try {
            Long memberId=memberServiceClient.createMemberProfile(profileRequest,account.getRegistrationKey());
            account.setMemberId(memberId);
            account.setStatus("ACTIVE");
            account=accountRepository.saveAndFlush(account);
        } catch (RuntimeException ex) {
            int status=downstreamStatus(ex);
            log.warn("Member profile registration is incomplete for account ID {} (downstream status {})",account.getId(),status);
            if (status==400) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Member profile details were rejected. Correct the details and retry registration.");
            if (status==409) throw new ResponseStatusException(HttpStatus.CONFLICT,"The submitted member profile conflicts with an existing record.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Member profile setup is pending. Retry registration with the same email and password.");
        }
        return issueResponse(account);
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