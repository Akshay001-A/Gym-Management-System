
package com.gym.membership;

import com.gym.membership.entity.Membership;
import com.gym.membership.enums.MembershipPlan;
import com.gym.membership.service.MembershipService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(
            MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/health")
    public String health() {
        return "Membership Service is running successfully!";
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Membership createMembership(
            @Valid @RequestBody Membership membership) {

        return membershipService.createMembership(membership);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Membership> getAllMemberships() {
        return membershipService.getAllMemberships();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Membership getMembershipById(
            @PathVariable Long id) {

        return membershipService.getMembershipById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Membership updateMembership(
            @PathVariable Long id,
            @Valid @RequestBody Membership membership) {

        return membershipService.updateMembership(id, membership);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMembership(@PathVariable Long id) {
        membershipService.deleteMembership(id);
    }

    @PostMapping("/plan")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Membership createMembershipForPlan(
            @RequestParam Long memberId,
            @RequestParam MembershipPlan plan) {

        return membershipService.createMembershipForPlan(
                memberId, plan
        );
    }
}
