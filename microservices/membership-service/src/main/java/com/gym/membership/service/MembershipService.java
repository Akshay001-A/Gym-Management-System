package com.gym.membership.service;

import com.gym.membership.client.MemberClient;
import com.gym.membership.dto.MembershipPlanView;
import com.gym.membership.dto.PaymentSimulationRequest;
import com.gym.membership.entity.Membership;
import com.gym.membership.entity.PaymentRecord;
import com.gym.membership.enums.MembershipPlan;
import com.gym.membership.exception.MembershipNotFoundException;
import com.gym.membership.repository.MembershipRepository;
import com.gym.membership.repository.PaymentRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MembershipService {
    private static final String ACTIVE = "ACTIVE";
    private final MembershipRepository membershipRepository;
    private final PaymentRecordRepository paymentRepository;
    private final MemberClient memberClient;

    public MembershipService(MembershipRepository membershipRepository,
            PaymentRecordRepository paymentRepository, MemberClient memberClient) {
        this.membershipRepository = membershipRepository;
        this.paymentRepository = paymentRepository;
        this.memberClient = memberClient;
    }

    @Transactional(readOnly = true)
    public List<MembershipPlanView> listPlans() {
        return Arrays.stream(MembershipPlan.values())
                .map(plan -> new MembershipPlanView(plan.name(), plan.getDurationMonths(), plan.getPrice()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MembershipPlanView getPlan(String name) {
        MembershipPlan plan = parsePlan(name);
        return new MembershipPlanView(plan.name(), plan.getDurationMonths(), plan.getPrice());
    }

    @Transactional
    public Membership createMembership(Membership membership) {
        validateMember(membership.getMemberId());
        MembershipPlan plan = parsePlan(membership.getPlanName());
        validateDates(membership.getStartDate(), membership.getEndDate(), plan);
        ensureNoActiveOverlap(membership.getMemberId(), membership.getStartDate(), membership.getEndDate());
        membership.setPlanName(plan.name());
        membership.setPrice(plan.getPrice());
        membership.setStatus(ACTIVE);
        return membershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public List<Membership> getAllMemberships(Pageable pageable) {
        return membershipRepository.findAll(pageable).getContent();
    }

    @Transactional(readOnly = true)
    public Page<Membership> getMembershipPage(Long memberId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()
                && !List.of("ACTIVE", "EXPIRED", "CANCELLED", "PENDING_PAYMENT").contains(status.trim().toUpperCase())) {
            throw new IllegalArgumentException("Unsupported membership status");
        }
        if (memberId != null && status != null && !status.isBlank()) {
            return membershipRepository.findByMemberIdAndStatusIgnoreCase(memberId, status, pageable);
        }
        if (memberId != null) return membershipRepository.findByMemberId(memberId, pageable);
        if (status != null && !status.isBlank()) return membershipRepository.findByStatusIgnoreCase(status, pageable);
        return membershipRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Membership> getMembershipsByMemberId(Long memberId, Pageable pageable) {
        validateMemberId(memberId);
        return membershipRepository.findByMemberId(memberId, pageable).getContent();
    }

    @Transactional(readOnly = true)
    public Membership getMembershipById(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("A valid membership ID is required");
        return membershipRepository.findById(id).orElseThrow(() -> new MembershipNotFoundException(id));
    }

    @Transactional
    public Membership updateMembership(Long id, Membership request) {
        Membership existing = getMembershipById(id);
        if (!existing.getMemberId().equals(request.getMemberId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Membership ownership cannot be changed");
        }
        MembershipPlan plan = parsePlan(request.getPlanName());
        validateDates(request.getStartDate(), request.getEndDate(), plan);
        if (ACTIVE.equals(existing.getStatus())) {
            ensureNoActiveOverlap(existing.getMemberId(), request.getStartDate(), request.getEndDate(), id);
        }
        existing.setPlanName(plan.name());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setPrice(plan.getPrice());
        return membershipRepository.save(existing);
    }

    @Transactional
    public void deleteMembership(Long id) {
        Membership membership = getMembershipById(id);
        if (paymentRepository.existsByMembershipId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Memberships with payment history cannot be deleted");
        }
        membershipRepository.delete(membership);
    }

    @Transactional
    public Membership createMembershipForPlan(Long memberId, MembershipPlan plan) {
        validateMember(memberId);
        if (plan == null) throw new IllegalArgumentException("Membership plan is required");
        LocalDate start = LocalDate.now();
        return savePlanMembership(memberId, plan, start);
    }

    @Transactional
    public Membership assignPlanPendingPayment(Long memberId, MembershipPlan plan) {
        validateMember(memberId);
        if (plan == null) throw new IllegalArgumentException("Membership plan is required");
        Membership pending = membershipRepository
                .findFirstByMemberIdAndPlanNameAndStatusOrderByIdDesc(memberId, plan.name(), "PENDING_PAYMENT")
                .orElse(null);
        if (pending != null) return pending;
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(plan.getDurationMonths());
        ensureNoActiveOverlap(memberId, start, end);
        Membership membership = new Membership();
        membership.setMemberId(memberId);
        membership.setPlanName(plan.name());
        membership.setStartDate(start);
        membership.setEndDate(end);
        membership.setPrice(plan.getPrice());
        membership.setStatus("PENDING_PAYMENT");
        return membershipRepository.save(membership);
    }

    @Transactional
    public Membership recoverAssignedPlan(Long memberId) {
        validateMember(memberId);
        if (!membershipRepository.findByMemberId(memberId).isEmpty()) return null;
        BigDecimal amount = memberClient.memberAmount(memberId);
        if (amount == null) return null;
        MembershipPlan plan = Arrays.stream(MembershipPlan.values())
                .filter(candidate -> candidate.getPrice().compareTo(amount) == 0)
                .findFirst().orElse(null);
        return plan == null ? null : assignPlanPendingPayment(memberId, plan);
    }

    @Transactional
    public Membership activateMembershipForPayment(Long paymentId, Long memberId, MembershipPlan plan) {
        if (paymentId == null || paymentId <= 0) throw new IllegalArgumentException("A valid payment ID is required");
        validateMember(memberId);
        if (plan == null) throw new IllegalArgumentException("Membership plan is required");
        Membership existingPayment = membershipRepository.findByPaymentId(paymentId).orElse(null);
        if (existingPayment != null) {
            if (!existingPayment.getMemberId().equals(memberId) || !existingPayment.getPlanName().equals(plan.name())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is already linked to another membership");
            }
            return existingPayment;
        }
        Membership membership = membershipRepository
                .findFirstByMemberIdAndPlanNameAndStatusOrderByIdDesc(memberId, plan.name(), "PENDING_PAYMENT")
                .orElse(null);
        if (membership == null) {
            LocalDate start = LocalDate.now();
            ensureNoActiveOverlap(memberId, start, start.plusMonths(plan.getDurationMonths()));
            membership = new Membership();
            membership.setMemberId(memberId);
            membership.setPlanName(plan.name());
            membership.setStartDate(start);
            membership.setEndDate(start.plusMonths(plan.getDurationMonths()));
            membership.setPrice(plan.getPrice());
        } else {
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusMonths(plan.getDurationMonths());
            ensureNoActiveOverlap(memberId, start, end, membership.getId());
            membership.setStartDate(start);
            membership.setEndDate(end);
            membership.setPrice(plan.getPrice());
        }
        membership.setPaymentId(paymentId);
        membership.setStatus(ACTIVE);
        return membershipRepository.saveAndFlush(membership);
    }

    @Transactional
    public Membership renewMembership(Long memberId, MembershipPlan plan) {
        validateMember(memberId);
        if (plan == null) throw new IllegalArgumentException("Membership plan is required");
        LocalDate today = LocalDate.now();
        LocalDate start = membershipRepository
                .findFirstByMemberIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(memberId, ACTIVE, today)
                .map(membership -> membership.getEndDate().plusDays(1)).orElse(today);
        return savePlanMembership(memberId, plan, start);
    }

    @Transactional
    public Membership cancelMembershipForMember(Long memberId, Long membershipId) {
        Membership membership = getMembershipById(membershipId);
        if (!membership.getMemberId().equals(memberId)) {
            throw new MembershipNotFoundException(membershipId);
        }
        if (!ACTIVE.equals(membership.getStatus()) && !"PENDING_PAYMENT".equals(membership.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active or awaiting-payment memberships can be cancelled");
        }
        membership.setStatus("CANCELLED");
        return membershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public Page<PaymentRecord> getPaymentPage(Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<PaymentRecord> getPaymentsForMember(Long memberId, Pageable pageable) {
        validateMemberId(memberId);
        return paymentRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public PaymentRecord startSimulatedPayment(Long memberId, Long membershipId, String idempotencyKey) {
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9._:-]{8,100}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid Idempotency-Key header is required");
        }
        Membership membership = getMembershipById(membershipId);
        if (!membership.getMemberId().equals(memberId)) throw new MembershipNotFoundException(membershipId);
        PaymentRecord prior = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (prior != null) {
            if (prior.getMemberId().equals(memberId) && prior.getMembershipId().equals(membershipId)) return prior;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was already used");
        }
        LocalDate today = LocalDate.now();
        if (!ACTIVE.equals(membership.getStatus()) || today.isBefore(membership.getStartDate())
                || today.isAfter(membership.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment can only be started for a current active membership");
        }
        PaymentRecord record = new PaymentRecord();
        record.setMemberId(memberId);
        record.setMembershipId(membershipId);
        record.setAmount(membership.getPrice());
        record.setStatus("PENDING");
        record.setSimulated(true);
        record.setIdempotencyKey(idempotencyKey);
        record.setTransactionReference("SIMULATED-" + UUID.randomUUID());
        return paymentRepository.save(record);
    }

    @Transactional
    public PaymentRecord simulatePaymentOutcome(Long memberId, Long paymentId, PaymentSimulationRequest.Outcome outcome) {
        PaymentRecord payment = paymentRepository.findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment record not found"));
        if (!payment.isSimulated()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only simulated records can use this endpoint");
        if (!"PENDING".equals(payment.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is already final");
        if (outcome == null) throw new IllegalArgumentException("A simulated payment outcome is required");
        payment.setStatus(outcome == PaymentSimulationRequest.Outcome.SUCCESS ? "PAID" : "FAILED");
        return paymentRepository.save(payment);
    }

    private Membership savePlanMembership(Long memberId, MembershipPlan plan, LocalDate start) {
        LocalDate end = start.plusMonths(plan.getDurationMonths());
        ensureNoActiveOverlap(memberId, start, end);
        Membership membership = new Membership();
        membership.setMemberId(memberId);
        membership.setPlanName(plan.name());
        membership.setStartDate(start);
        membership.setEndDate(end);
        membership.setPrice(plan.getPrice());
        membership.setStatus(ACTIVE);
        return membershipRepository.save(membership);
    }

    private void ensureNoActiveOverlap(Long memberId, LocalDate start, LocalDate end) {
        ensureNoActiveOverlap(memberId, start, end, null);
    }

    private void ensureNoActiveOverlap(Long memberId, LocalDate start, LocalDate end, Long excludeId) {
        boolean overlaps = excludeId == null
                ? membershipRepository.existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        memberId, ACTIVE, end, start)
                : membershipRepository.existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                        memberId, ACTIVE, end, start, excludeId);
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member already has an overlapping active membership");
        }
    }

    private void validateMember(Long memberId) {
        validateMemberId(memberId);
        memberClient.memberExists(memberId);
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) throw new IllegalArgumentException("A valid member ID is required");
    }

    private MembershipPlan parsePlan(String name) {
        if (name == null) throw new IllegalArgumentException("A supported membership plan is required");
        try { return MembershipPlan.valueOf(name.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Unsupported membership plan"); }
    }

    private void validateDates(LocalDate start, LocalDate end, MembershipPlan plan) {
        if (start == null || end == null) throw new IllegalArgumentException("Membership start date and end date are required");
        if (start.isBefore(LocalDate.now())) throw new IllegalArgumentException("Membership start date cannot be in the past");
        if (!end.equals(start.plusMonths(plan.getDurationMonths()))) {
            throw new IllegalArgumentException("Membership dates must match the selected plan duration");
        }
    }

    @Transactional
    public int updateExpiredMemberships() {
        return membershipRepository.expireActiveMemberships(LocalDate.now());
    }
}
