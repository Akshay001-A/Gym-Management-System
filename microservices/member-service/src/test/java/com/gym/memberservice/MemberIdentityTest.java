package com.gym.memberservice;

import com.gym.memberservice.controller.MemberController;
import com.gym.memberservice.config.InternalServiceKeyFilter;
import jakarta.servlet.FilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import java.util.concurrent.atomic.AtomicBoolean;
import com.gym.memberservice.entity.Member;
import com.gym.memberservice.dto.MemberProfileUpdateRequest;
import java.math.BigDecimal;
import com.gym.memberservice.repository.MemberRepository;
import com.gym.memberservice.service.MemberService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemberIdentityTest {
    @Test void memberMeUsesMemberIdClaimInsteadOfAccountIdOrEmailLookup() {
        MemberService service=mock(MemberService.class);
        MemberController controller=new MemberController(service,"internal-key");
        Jwt jwt=Jwt.withTokenValue("token").header("alg","HS256").subject("member@example.com")
                .claim("accountId",18L).claim("memberId",42L).claim("role","MEMBER").build();
        Member profile=new Member();profile.setId(42L);
        when(service.getMemberById(42L)).thenReturn(profile);
        assertEquals(42L,controller.getMyProfile(jwt).id());
        verify(service).getMemberById(42L);
        verify(service,never()).getMemberByEmail(anyString());
    }

    @Test void wrongInternalKeyIsRejected() {
        MemberController controller=new MemberController(mock(MemberService.class),"expected-key");
        ResponseStatusException error=assertThrows(ResponseStatusException.class,
                ()->controller.memberExistsInternal("wrong-key",42L));
        assertEquals(401,error.getStatusCode().value());
    }

    @Test void internalFilterRejectsWrongServiceKeyBeforeEndpointRuns() throws Exception {
        InternalServiceKeyFilter filter=new InternalServiceKeyFilter("expected-key",JsonMapper.builder().build());
        MockHttpServletRequest request=new MockHttpServletRequest();request.setRequestURI("/api/members/internal/42/exists");request.addHeader("X-Internal-Service-Key","wrong-key");
        MockHttpServletResponse response=new MockHttpServletResponse();AtomicBoolean proceeded=new AtomicBoolean(false);
        FilterChain chain=(req,res)->proceeded.set(true);
        filter.doFilter(request,response,chain);
        assertEquals(401,response.getStatus());
        assertFalse(proceeded.get());
    }
    @Test void registrationRetryReturnsProfileAlreadyCreatedForThatKey() {
        MemberRepository repository=mock(MemberRepository.class);
        MemberService service=new MemberService(repository);
        Member existing=new Member();existing.setId(42L);existing.setEmail("member@example.com");existing.setRegistrationKey("registration-uuid");
        when(repository.findByRegistrationKey("registration-uuid")).thenReturn(Optional.of(existing));
        Member retry=new Member();retry.setEmail("MEMBER@example.com");
        assertSame(existing,service.createMemberForRegistration(retry,"registration-uuid"));
        verify(repository,never()).save(any(Member.class));
    }

    @Test void memberSelfUpdateUsesMappedMemberIdAndDoesNotExposeBillingFields() {
        MemberService service=mock(MemberService.class);
        MemberController controller=new MemberController(service,"internal-key");
        Jwt jwt=Jwt.withTokenValue("token").header("alg","HS256").subject("account@example.com")
                .claim("accountId",18L).claim("memberId",42L).claim("role","MEMBER").build();
        MemberProfileUpdateRequest request=new MemberProfileUpdateRequest(
                "Updated Name",28,"+919876543210","Evening","new@example.com","Other","Parent");
        Member profile=new Member();profile.setId(42L);profile.setAmount(new BigDecimal("1200.00"));
        when(service.updateMemberProfile(42L,request)).thenReturn(profile);
        assertEquals(42L,controller.updateMyProfile(jwt,request).id());
        verify(service).updateMemberProfile(42L,request);
        assertEquals(new BigDecimal("1200.00"),profile.getAmount());
    }

    @Test void selfProfilePatchChangesOnlySubmittedFieldsAndKeepsBillingProtected() {
        MemberRepository repository=mock(MemberRepository.class);
        MemberService service=new MemberService(repository);
        Member existing=new Member();existing.setId(42L);existing.setName("Old Name");
        existing.setEmail("member@example.com");existing.setMobile("9000000001");
        existing.setAmount(new BigDecimal("1200.00"));existing.setRegistrationKey("internal-only");
        when(repository.findById(42L)).thenReturn(Optional.of(existing));
        when(repository.findByMobile("9000000002")).thenReturn(Optional.empty());
        when(repository.save(existing)).thenReturn(existing);
        MemberProfileUpdateRequest request=new MemberProfileUpdateRequest(null,null,"9000000002",null,null,null,null);
        Member updated=service.updateMemberProfile(42L,request);
        assertEquals("Old Name",updated.getName());
        assertEquals("9000000002",updated.getMobile());
        assertEquals(new BigDecimal("1200.00"),updated.getAmount());
        assertEquals("internal-only",updated.getRegistrationKey());
    }

    @Test void correlationFilterPropagatesSafeIdsAndReplacesUnsafeValues() throws Exception {
        com.gym.memberservice.config.CorrelationIdFilter filter=new com.gym.memberservice.config.CorrelationIdFilter();
        MockHttpServletRequest request=new MockHttpServletRequest();request.setRequestURI("/api/members/me");
        request.addHeader("X-Correlation-ID","client-request-123");
        MockHttpServletResponse response=new MockHttpServletResponse();
        filter.doFilter(request,response,(req,res)->{});
        assertEquals("client-request-123",response.getHeader("X-Correlation-ID"));

        MockHttpServletRequest unsafe=new MockHttpServletRequest();unsafe.setRequestURI("/api/members/me");
        unsafe.addHeader("X-Correlation-ID","bad\\r\\nvalue");
        MockHttpServletResponse safeResponse=new MockHttpServletResponse();
        filter.doFilter(unsafe,safeResponse,(req,res)->{});
        assertNotEquals("bad\\r\\nvalue",safeResponse.getHeader("X-Correlation-ID"));
        assertTrue(safeResponse.getHeader("X-Correlation-ID").matches("[A-Za-z0-9._:-]{1,64}"));
    }
}