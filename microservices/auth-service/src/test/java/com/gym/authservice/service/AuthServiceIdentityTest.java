package com.gym.authservice.service;

import com.gym.authservice.client.MemberServiceClient;
import com.gym.authservice.dto.AuthResponse;
import com.gym.authservice.dto.MemberProfileRequest;
import com.gym.authservice.dto.LoginRequest;
import com.gym.authservice.dto.RegisterRequest;
import com.gym.authservice.entity.Account;
import com.gym.authservice.repository.AccountRepository;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceIdentityTest {
    @Test void registrationKeepsAccountIdSeparateAndRetryUsesSameRegistrationKey() {
        AccountRepository accounts=mock(AccountRepository.class);
        PasswordEncoder passwords=mock(PasswordEncoder.class);
        JwtService jwt=mock(JwtService.class);
        MemberServiceClient members=mock(MemberServiceClient.class);
        AtomicReference<Account> stored=new AtomicReference<>();
        when(accounts.findByEmailIgnoreCase("member@example.com")).thenAnswer(call->Optional.ofNullable(stored.get()));
        when(accounts.saveAndFlush(any(Account.class))).thenAnswer(call->{Account account=call.getArgument(0);if(account.getId()==null)account.setId(18L);stored.set(account);return account;});
        when(passwords.encode("strong-password")).thenReturn("hashed-password");
        when(passwords.matches("strong-password","hashed-password")).thenReturn(true);
        when(jwt.generateToken(any(Account.class))).thenReturn("signed-token");
        when(members.createMemberProfile(any(MemberProfileRequest.class),anyString()))
                .thenThrow(new IllegalStateException("temporary downstream failure"))
                .thenReturn(42L);
        AuthService service=new AuthService(accounts,passwords,jwt,members);
        RegisterRequest registration=new RegisterRequest();registration.setName("Member");registration.setEmail("member@example.com");registration.setPassword("strong-password");
        MemberProfileRequest profile=new MemberProfileRequest();profile.setEmail("member@example.com");

        ResponseStatusException first=assertThrows(ResponseStatusException.class,()->service.register(registration,profile));
        assertEquals(503,first.getStatusCode().value());
        String retryKey=stored.get().getRegistrationKey();
        assertEquals("PENDING",stored.get().getStatus());

        AuthResponse response=service.register(registration,profile);
        assertEquals(18L,response.getAccountId());
        assertEquals(42L,response.getMemberId());
        assertEquals("ACTIVE",stored.get().getStatus());
        assertEquals(42L,stored.get().getMemberId());
        verify(members,times(2)).createMemberProfile(any(MemberProfileRequest.class),eq(retryKey));
        LoginRequest login=new LoginRequest();login.setEmail("member@example.com");login.setPassword("strong-password");
        AuthResponse loggedIn=service.login(login);
        assertEquals(18L,loggedIn.getAccountId());
        assertEquals(42L,loggedIn.getMemberId());
        ResponseStatusException duplicate=assertThrows(ResponseStatusException.class,()->service.register(registration,profile));
        assertEquals(409,duplicate.getStatusCode().value());        verify(accounts,never()).deleteById(anyLong());
    }
}