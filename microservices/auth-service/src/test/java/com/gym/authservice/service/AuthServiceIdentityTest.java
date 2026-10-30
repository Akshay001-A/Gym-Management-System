package com.gym.authservice.service;

import com.gym.authservice.client.MemberServiceClient;
import com.gym.authservice.dto.AuthResponse;
import com.gym.authservice.dto.MemberRegistrationMatch;
import com.gym.authservice.entity.Account;
import com.gym.authservice.entity.MemberRegistrationChallenge;
import com.gym.authservice.repository.AccountRepository;
import com.gym.authservice.repository.MemberRegistrationChallengeRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceIdentityTest {
    @Test
    void verifiedRegistrationLinksTheExistingMemberProfile() {
        AccountRepository accounts = mock(AccountRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        JwtService jwt = mock(JwtService.class);
        MemberServiceClient members = mock(MemberServiceClient.class);
        MemberRegistrationChallengeRepository challenges = mock(MemberRegistrationChallengeRepository.class);
        ObjectProvider<JavaMailSender> mailSenders = mock(ObjectProvider.class);
        String email = "member@example.com";
        MemberRegistrationMatch member = new MemberRegistrationMatch(42L, "Member", email, "9876543210");
        MemberRegistrationChallenge challenge = new MemberRegistrationChallenge(
                email, member.id(), "hashed-code", Instant.now().plusSeconds(600));

        when(members.findMemberForRegistration(email)).thenReturn(member);
        when(challenges.findById(email)).thenReturn(Optional.of(challenge));
        when(passwords.matches("123456", "hashed-code")).thenReturn(true);
        when(passwords.encode("strong-password")).thenReturn("hashed-password");
        when(accounts.existsByEmailIgnoreCase(email)).thenReturn(false);
        when(accounts.saveAndFlush(any(Account.class))).thenAnswer(call -> {
            Account account = call.getArgument(0);
            account.setId(18L);
            return account;
        });
        when(jwt.generateToken(any(Account.class))).thenReturn("signed-token");

        AuthService service = new AuthService(accounts, passwords, jwt, members, challenges,
                mailSenders, "", "", 10);
        AuthResponse response = service.completeMemberRegistration(email, "123456", "strong-password");

        assertEquals(18L, response.getAccountId());
        assertEquals(42L, response.getMemberId());
        assertEquals("MEMBER", response.getRole());
        verify(challenges).delete(challenge);
    }
}
