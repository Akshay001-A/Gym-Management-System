
package com.gym.membership.service;

import com.gym.membership.client.MemberClient;
import com.gym.membership.entity.Membership;
import com.gym.membership.enums.MembershipPlan;
import com.gym.membership.exception.MembershipNotFoundException;
import com.gym.membership.repository.MembershipRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Membership createMembership(Membership membership) {
        validateMember(membership.getMemberId());
        validateDates(
                membership.getStartDate(),
                membership.getEndDate()
        );

        membership.setStatus("ACTIVE");

        return membershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Membership> getMembershipsByMemberId(
            Long memberId) {

        validateMemberId(memberId);

        return membershipRepository.findByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public Membership getMembershipById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "A valid membership ID is required"
            );
        }

        return membershipRepository.findById(id)
                .orElseThrow(
                        () -> new MembershipNotFoundException(id)
                );
    }

    @Transactional
    public Membership updateMembership(
            Long id,
            Membership updatedMembership) {

        Membership existing = getMembershipById(id);

        validateMember(updatedMembership.getMemberId());
        validateDates(
                updatedMembership.getStartDate(),
                updatedMembership.getEndDate()
        );

        existing.setMemberId(updatedMembership.getMemberId());
        existing.setPlanName(updatedMembership.getPlanName());
        existing.setStartDate(updatedMembership.getStartDate());
        existing.setEndDate(updatedMembership.getEndDate());
        existing.setPrice(updatedMembership.getPrice());

        return membershipRepository.save(existing);
    }

    @Transactional
    public void deleteMembership(Long id) {
        Membership existing = getMembershipById(id);
        membershipRepository.delete(existing);
    }

    @Transactional
    public Membership createMembershipForPlan(
            Long memberId,
            MembershipPlan plan) {

        validateMember(memberId);

        if (plan == null) {
            throw new IllegalArgumentException(
                    "Membership plan is required"
            );
        }

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
        validateMemberId(memberId);
        memberClient.memberExists(memberId);
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException(
                    "A valid member ID is required"
            );
        }
    }

    private void validateDates(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Membership start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }
    }

    @Transactional
    public void updateExpiredMemberships() {
        List<Membership> memberships =
                membershipRepository.findAll();

        LocalDate today = LocalDate.now();

        for (Membership membership : memberships) {
            if ("ACTIVE".equals(membership.getStatus())
                    && membership.getEndDate() != null
                    && membership.getEndDate().isBefore(today)) {

                membership.setStatus("EXPIRED");
                membershipRepository.save(membership);
            }
        }
    }
}
