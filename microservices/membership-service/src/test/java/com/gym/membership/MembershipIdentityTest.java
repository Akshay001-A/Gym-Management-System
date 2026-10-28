package com.gym.membership;

import com.gym.membership.client.MemberClient;
import com.gym.membership.MembershipController;
import com.gym.membership.entity.Membership;
import com.gym.membership.entity.PaymentRecord;
import com.gym.membership.enums.MembershipPlan;
import com.gym.membership.dto.PaymentSimulationRequest;
import com.gym.membership.repository.PaymentRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import com.gym.membership.repository.MembershipRepository;
import com.gym.membership.service.MembershipService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembershipIdentityTest {
    @Test void memberMembershipQueryUsesMemberIdClaim() {
        MembershipRepository repository=mock(MembershipRepository.class);
        MemberClient memberClient=mock(MemberClient.class);
        MembershipService service=new MembershipService(repository,mock(PaymentRecordRepository.class),memberClient);
        when(repository.findByMemberId(eq(42L), any())).thenReturn(new PageImpl<>(List.of()));
        assertEquals(List.of(),service.getMembershipsByMemberId(42L, PageRequest.of(0, 100, Sort.by("startDate").descending())));
        verify(repository).findByMemberId(eq(42L), any());
    }

    @Test void memberMeEndpointReadsMemberIdClaim() {
        MembershipService service=mock(MembershipService.class);
        MembershipController controller=new MembershipController(service);
        Jwt jwt=Jwt.withTokenValue("token").header("alg","HS256").subject("member@example.com")
                .claim("accountId",18L).claim("memberId",42L).claim("role","MEMBER").build();
        when(service.getMembershipsByMemberId(eq(42L), any())).thenReturn(List.of());
        assertEquals(List.of(),controller.getMyMemberships(jwt));
        verify(service).getMembershipsByMemberId(eq(42L), any());
    }
    @Test void memberServiceUnavailableStopsMembershipCreation() {
        MembershipRepository repository=mock(MembershipRepository.class);
        MemberClient memberClient=mock(MemberClient.class);
        doThrow(new MemberClient.MemberServiceUnavailableException("Member Service unavailable"))
                .when(memberClient).memberExists(42L);
        MembershipService service=new MembershipService(repository,mock(PaymentRecordRepository.class),memberClient);
        Membership membership=new Membership();membership.setMemberId(42L);
        assertThrows(MemberClient.MemberServiceUnavailableException.class,()->service.createMembership(membership));
        verify(repository,never()).save(any(Membership.class));
    }

    @Test void planEnrollmentUsesCatalogPriceAndRejectsOverlaps() {
        MembershipRepository repository=mock(MembershipRepository.class);
        PaymentRecordRepository payments=mock(PaymentRecordRepository.class);
        MemberClient client=mock(MemberClient.class);
        MembershipService service=new MembershipService(repository,payments,client);
        when(repository.existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(42L),eq("ACTIVE"),any(LocalDate.class),any(LocalDate.class))).thenReturn(false);
        when(repository.save(any(Membership.class))).thenAnswer(invocation->invocation.getArgument(0));
        Membership created=service.createMembershipForPlan(42L,MembershipPlan.MONTHLY);
        assertEquals(MembershipPlan.MONTHLY.getPrice(),created.getPrice());
        assertEquals("ACTIVE",created.getStatus());
        verify(repository).save(any(Membership.class));

        when(repository.existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(42L),eq("ACTIVE"),any(LocalDate.class),any(LocalDate.class))).thenReturn(true);
        assertEquals(409,assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->service.createMembershipForPlan(42L,MembershipPlan.MONTHLY)).getStatusCode().value());
    }

    @Test void renewalStartsAfterTheCurrentMembershipAndCancellationIsOneWay() {
        MembershipRepository repository=mock(MembershipRepository.class);
        MembershipService service=new MembershipService(repository,mock(PaymentRecordRepository.class),mock(MemberClient.class));
        Membership current=new Membership();current.setMemberId(42L);
        current.setStatus("ACTIVE");current.setStartDate(LocalDate.now().minusMonths(1));
        current.setEndDate(LocalDate.now().plusDays(3));
        when(repository.findFirstByMemberIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(
                eq(42L),eq("ACTIVE"),any(LocalDate.class))).thenReturn(Optional.of(current));
        when(repository.existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(42L),eq("ACTIVE"),any(LocalDate.class),any(LocalDate.class))).thenReturn(false);
        when(repository.save(any(Membership.class))).thenAnswer(invocation->invocation.getArgument(0));
        Membership renewal=service.renewMembership(42L,MembershipPlan.QUARTERLY);
        assertEquals(current.getEndDate().plusDays(1),renewal.getStartDate());

        when(repository.findById(7L)).thenReturn(Optional.of(current));
        when(repository.save(current)).thenReturn(current);
        assertEquals("CANCELLED",service.cancelMembershipForMember(42L,7L).getStatus());
    }

    @Test void simulatedPaymentUsesMembershipPriceAndIdempotencyKey() {
        MembershipRepository memberships=mock(MembershipRepository.class);
        PaymentRecordRepository payments=mock(PaymentRecordRepository.class);
        MembershipService service=new MembershipService(memberships,payments,mock(MemberClient.class));
        Membership membership=new Membership();membership.setMemberId(42L);
        membership.setStatus("ACTIVE");membership.setStartDate(LocalDate.now());
        membership.setEndDate(LocalDate.now().plusMonths(1));membership.setPrice(new BigDecimal("1500.00"));
        when(memberships.findById(6L)).thenReturn(Optional.of(membership));
        when(payments.findByIdempotencyKey("request-12345")).thenReturn(Optional.empty());
        when(payments.save(any(PaymentRecord.class))).thenAnswer(invocation->invocation.getArgument(0));
        PaymentRecord first=service.startSimulatedPayment(42L,6L,"request-12345");
        assertEquals(new BigDecimal("1500.00"),first.getAmount());
        assertTrue(first.isSimulated());
        assertTrue(first.getTransactionReference().startsWith("SIMULATED-"));
        when(payments.findByIdempotencyKey("request-12345")).thenReturn(Optional.of(first));
        assertSame(first,service.startSimulatedPayment(42L,6L,"request-12345"));
        verify(payments,times(1)).save(any(PaymentRecord.class));
        when(payments.findByIdAndMemberId(first.getId(),42L)).thenReturn(Optional.of(first));
        PaymentRecord paid=service.simulatePaymentOutcome(42L,first.getId(),PaymentSimulationRequest.Outcome.SUCCESS);
        assertEquals("PAID",paid.getStatus());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->service.simulatePaymentOutcome(42L,first.getId(),PaymentSimulationRequest.Outcome.SUCCESS));
    }

    @Test void membershipWithPaymentHistoryCannotBeDeleted() {
        MembershipRepository memberships=mock(MembershipRepository.class);
        PaymentRecordRepository payments=mock(PaymentRecordRepository.class);
        MembershipService service=new MembershipService(memberships,payments,mock(MemberClient.class));
        Membership membership=new Membership();membership.setMemberId(42L);
        when(memberships.findById(11L)).thenReturn(Optional.of(membership));
        when(payments.existsByMembershipId(11L)).thenReturn(true);
        assertEquals(409,assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->service.deleteMembership(11L)).getStatusCode().value());
        verify(memberships,never()).delete(any(Membership.class));
    }
}
