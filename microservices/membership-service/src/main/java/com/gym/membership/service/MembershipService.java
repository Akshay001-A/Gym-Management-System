
package com.gym.membership.service;

import com.gym.membership.client.MemberClient;
import com.gym.membership.entity.Membership;
import com.gym.membership.enums.MembershipPlan;
import com.gym.membership.exception.MembershipNotFoundException;
import com.gym.membership.repository.MembershipRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final MemberClient memberClient;

    public MembershipService(
            MembershipRepository membershipRepository,
            MemberClient memberClient) {
        this.membershipRepository = membershipRepository;
        this.memberClient = memberClient;
    }

    public Membership createMembership(Membership membership) {
        validateMember(membership.getMemberId());

        if (membership.getEndDate()
                .isBefore(membership.getStartDate())) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        membership.setStatus("ACTIVE");
        return membershipRepository.save(membership);
    }

    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }

    public Membership getMembershipById(Long id) {
        return membershipRepository.findById(id)
                .orElseThrow(
                        () -> new MembershipNotFoundException(id)
                );
    }

    public Membership updateMembership(
            Long id, Membership updatedMembership) {

        Membership existing = getMembershipById(id);

        validateMember(updatedMembership.getMemberId());

        if (updatedMembership.getEndDate()
                .isBefore(updatedMembership.getStartDate())) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        existing.setMemberId(updatedMembership.getMemberId());
        existing.setPlanName(updatedMembership.getPlanName());
        existing.setStartDate(updatedMembership.getStartDate());
        existing.setEndDate(updatedMembership.getEndDate());
        existing.setPrice(updatedMembership.getPrice());

        return membershipRepository.save(existing);
    }

    public void deleteMembership(Long id) {
        Membership existing = getMembershipById(id);
        membershipRepository.delete(existing);
    }

    public Membership createMembershipForPlan(
            Long memberId, MembershipPlan plan) {

        validateMember(memberId);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate =
                startDate.plusMonths(plan.getDurationMonths());

        Membership membership = new Membership();
        membership.setMemberId(memberId);
        membership.setPlanName(plan.name());
        membership.setStartDate(startDate);
        membership.setEndDate(endDate);
        membership.setPrice(plan.getPrice());
        membership.setStatus("ACTIVE");

        return membershipRepository.save(membership);
    }

    private void validateMember(Long memberId) {
        memberClient.memberExists(memberId);
    }

    public void updateExpiredMemberships() {
        List<Membership> memberships =
                membershipRepository.findAll();

        LocalDate today = LocalDate.now();

        for (Membership membership : memberships) {
            if ("ACTIVE".equals(membership.getStatus())
                    && membership.getEndDate().isBefore(today)) {
                membership.setStatus("EXPIRED");
                membershipRepository.save(membership);
            }
        }
    }
}
