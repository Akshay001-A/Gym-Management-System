package com.gym.membership;

import com.gym.membership.dto.MembershipPlanView;
import com.gym.membership.dto.PaymentSimulationRequest;
import com.gym.membership.dto.PlanEnrollmentRequest;
import com.gym.membership.entity.Membership;
import com.gym.membership.entity.PaymentRecord;
import com.gym.membership.enums.MembershipPlan;
import com.gym.membership.service.MembershipService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {
    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/health")
    public String health() { return "Membership Service is running successfully!"; }

    @GetMapping("/plans")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public List<MembershipPlanView> listPlans() { return membershipService.listPlans(); }

    @GetMapping("/plans/{planName}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    public MembershipPlanView getPlan(@PathVariable String planName) { return membershipService.getPlan(planName); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Membership createMembership(@Valid @RequestBody Membership membership) {
        return membershipService.createMembership(membership);
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEMBER')")
    public Membership enroll(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlanEnrollmentRequest request) {
        return membershipService.createMembershipForPlan(memberId(jwt), request.plan());
    }

    @PostMapping("/me/renewals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEMBER')")
    public Membership renew(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlanEnrollmentRequest request) {
        return membershipService.renewMembership(memberId(jwt), request.plan());
    }

    @PostMapping("/me/{membershipId}/cancel")
    @PreAuthorize("hasRole('MEMBER')")
    public Membership cancelOwnMembership(@AuthenticationPrincipal Jwt jwt, @PathVariable Long membershipId) {
        return membershipService.cancelMembershipForMember(memberId(jwt), membershipId);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public List<Membership> getMyMemberships(@AuthenticationPrincipal Jwt jwt) {
        return membershipService.getMembershipsByMemberId(memberId(jwt), PageRequest.of(0, 100, Sort.by("startDate").descending()));
    }

    @GetMapping("/me/page")
    @PreAuthorize("hasRole('MEMBER')")
    public Page<Membership> getMyMembershipPage(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Pageable pageable = pageRequest(page, size, "startDate", "desc");
        return membershipService.getMembershipPage(memberId(jwt), status, pageable);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Membership> getAllMemberships() {
        return membershipService.getAllMemberships(PageRequest.of(0, 100, Sort.by("id").descending()));
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<Membership> getMembershipPage(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return membershipService.getMembershipPage(memberId, status, pageRequest(page, size, sortBy, direction));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Membership getMembershipById(@PathVariable Long id) { return membershipService.getMembershipById(id); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Membership updateMembership(@PathVariable Long id, @Valid @RequestBody Membership membership) {
        return membershipService.updateMembership(id, membership);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMembership(@PathVariable Long id) { membershipService.deleteMembership(id); }

    @PostMapping("/plan")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Membership createMembershipForPlan(@RequestParam Long memberId, @RequestParam MembershipPlan plan) {
        return membershipService.createMembershipForPlan(memberId, plan);
    }

    @PostMapping("/me/{membershipId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEMBER')")
    public PaymentRecord startSimulatedPayment(@AuthenticationPrincipal Jwt jwt, @PathVariable Long membershipId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return membershipService.startSimulatedPayment(memberId(jwt), membershipId, idempotencyKey);
    }

    @GetMapping("/me/payments")
    @PreAuthorize("hasRole('MEMBER')")
    public Page<PaymentRecord> getMyPayments(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return membershipService.getPaymentsForMember(memberId(jwt), pageRequest(page, size, "createdAt", "desc"));
    }

    @PostMapping("/me/payments/{paymentId}/simulate")
    @PreAuthorize("hasRole('MEMBER')")
    public PaymentRecord simulatePayment(@AuthenticationPrincipal Jwt jwt, @PathVariable Long paymentId,
            @Valid @RequestBody PaymentSimulationRequest request) {
        return membershipService.simulatePaymentOutcome(memberId(jwt), paymentId, request.outcome());
    }

    @GetMapping("/payments/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<PaymentRecord> getPaymentPage(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return membershipService.getPaymentPage(pageRequest(page, size, "createdAt", "desc"));
    }

    private Long memberId(Jwt jwt) {
        Object claim = jwt.getClaim("memberId");
        if (!(claim instanceof Number value) || value.longValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Member account is not linked to a profile");
        }
        return value.longValue();
    }

    private Pageable pageRequest(int page, int size, String sortBy, String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        }
        if (!List.of("id", "memberId", "startDate", "endDate", "status", "price", "createdAt").contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        try { return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy)); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort direction"); }
    }
}
