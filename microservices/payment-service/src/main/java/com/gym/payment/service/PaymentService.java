
package com.gym.payment.service;

import com.gym.payment.dto.CreatePaymentResponse;
import com.gym.payment.dto.PaymentResponse;
import com.gym.payment.dto.MonthlyRevenueResponse;
import com.gym.payment.dto.RevenuePaymentItem;
import com.gym.payment.dto.RevenueReportResponse;
import com.gym.payment.client.MemberDirectoryClient;
import com.gym.payment.dto.VerifyPaymentRequest;
import com.gym.payment.entity.Payment;
import com.gym.payment.enums.PaymentStatus;
import com.gym.payment.exception.PaymentNotFoundException;
import com.gym.payment.exception.PaymentProcessingException;
import com.gym.payment.repository.PaymentRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final ZoneId REVENUE_ZONE = ZoneId.of("Asia/Kolkata");

    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final MembershipIntegrationService membershipIntegrationService;
    private final PaymentOrderPersistenceService orderPersistenceService;
    private final MemberDirectoryClient memberDirectoryClient;
    private final PaymentEmailService paymentEmailService;

    @Autowired
    public PaymentService(
            PaymentRepository paymentRepository,
            RazorpayService razorpayService,
            MembershipIntegrationService membershipIntegrationService,
            PaymentOrderPersistenceService orderPersistenceService,
            MemberDirectoryClient memberDirectoryClient,
            PaymentEmailService paymentEmailService
    ) {
        this.paymentRepository = paymentRepository;
        this.razorpayService = razorpayService;
        this.membershipIntegrationService = membershipIntegrationService;
        this.orderPersistenceService = orderPersistenceService;
        this.memberDirectoryClient = memberDirectoryClient;
        this.paymentEmailService = paymentEmailService;
    }

    public PaymentService(PaymentRepository paymentRepository, RazorpayService razorpayService,
            MembershipIntegrationService membershipIntegrationService,
            PaymentOrderPersistenceService orderPersistenceService,
            MemberDirectoryClient memberDirectoryClient) {
        this(paymentRepository, razorpayService, membershipIntegrationService,
                orderPersistenceService, memberDirectoryClient, null);
    }

    // IMPORTANT: Do not add @Transactional to this method.
    public CreatePaymentResponse createPaymentOrder(
            Long memberId,
            String planName,
            String idempotencyKey
    ) {
        return createPaymentOrder(memberId, planName, idempotencyKey, null);
    }

    public CreatePaymentResponse createPaymentOrder(
            Long memberId,
            String planName,
            String idempotencyKey,
            String memberAccessToken
    ) {
        // 1. Validate request.
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("Invalid member ID");
        }

        if (planName == null || planName.isBlank()) {
            throw new IllegalArgumentException("Plan name is required");
        }

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "A valid idempotency key is required"
            );
        }

        // 2. Check whether this request was already processed.
        Payment existing = orderPersistenceService
                .findByIdempotencyKey(idempotencyKey);

        if (existing != null) {
            validateIdempotencyRequest(existing, memberId, planName);

            if (existing.getRazorpayOrderId() != null) {
                return toCreatePaymentResponse(existing);
            }

            throw new IllegalStateException(
                    "A payment request with this key is already being processed or needs recovery"
            );
        }

        membershipIntegrationService.ensureMemberCanChangePlan(memberAccessToken);

        // 3. Get the actual plan price from Membership Service.
        long amountPaise = memberAccessToken == null
                ? membershipIntegrationService.getPlanAmountPaise(planName)
                : membershipIntegrationService.getPlanAmountPaise(
                        planName,
                        memberAccessToken
                );

        if (amountPaise < 100) {
            throw new IllegalArgumentException(
                    "The membership plan has an invalid payment amount"
            );
        }

        // 4. Persist the initial payment in its own transaction.
        Payment payment;

        try {
            payment = orderPersistenceService.createPayment(
                    memberId,
                    planName,
                    amountPaise,
                    idempotencyKey
            );
        } catch (DataIntegrityViolationException exception) {

            // Another concurrent request may have inserted this key.
            Payment concurrentPayment = orderPersistenceService
                    .findByIdempotencyKey(idempotencyKey);

            if (concurrentPayment == null) {
                throw exception;
            }

            validateIdempotencyRequest(
                    concurrentPayment,
                    memberId,
                    planName
            );

            if (concurrentPayment.getRazorpayOrderId() != null) {
                return toCreatePaymentResponse(concurrentPayment);
            }

            throw new IllegalStateException(
                    "A payment request with this key is already being processed"
            );
        }

        // 5. Call Razorpay OUTSIDE a database transaction.
        RazorpayService.RazorpayOrder order;

        try {
            order = razorpayService.createOrder(
                    amountPaise,
                    "gym-payment-" + payment.getId(),
                    payment.getId()
            );

            if (order == null
                    || order.orderId() == null
                    || order.orderId().isBlank()) {
                throw new IllegalStateException(
                        "Razorpay returned an invalid order"
                );
            }

        } catch (RuntimeException exception) {

            // A timeout or server-side transport failure may mean Razorpay
            // created the order but the response was lost. Preserve CREATED
            // for reconciliation instead of falsely recording a failure.
            try {
                if (exception instanceof PaymentProcessingException processing
                        && processing.isOutcomeUnknown()) {
                    orderPersistenceService.markOrderOutcomeUnknown(
                            payment.getId(),
                            "Razorpay order outcome is unknown; reconciliation is required"
                    );
                } else {
                    orderPersistenceService.markOrderFailed(
                            payment.getId(),
                            exception.getMessage()
                    );
                }
            } catch (RuntimeException persistenceException) {
                exception.addSuppressed(persistenceException);
            }

            throw exception;
        }

        // 7. Persist the successful Razorpay order independently.
        // If this database update fails, leave the record CREATED for
        // reconciliation rather than incorrectly marking it FAILED.
        Payment updatedPayment;
        try {
            updatedPayment = orderPersistenceService.markOrderCreated(
                    payment.getId(),
                    order.orderId()
            );
        } catch (RuntimeException exception) {
            try {
                orderPersistenceService.markOrderOutcomeUnknown(
                        payment.getId(),
                        "Razorpay order was created but local persistence needs reconciliation"
                );
            } catch (RuntimeException persistenceException) {
                exception.addSuppressed(persistenceException);
            }
            throw exception;
        }

        return toCreatePaymentResponse(updatedPayment);
    }

    private void validateIdempotencyRequest(
            Payment payment,
            Long memberId,
            String planName
    ) {
        if (!payment.getMemberId().equals(memberId)
                || !payment.getPlanName().equals(planName)) {
            throw new IllegalArgumentException(
                    "Idempotency key was already used for another request"
            );
        }
    }

    public PaymentResponse verifyPayment(
            Long memberId,
            VerifyPaymentRequest request
    ) {
        if (memberId == null || memberId <= 0 || request == null) {
            throw new IllegalArgumentException(
                    "Invalid payment verification request"
            );
        }

        // Read first, then perform network calls without holding a database
        // transaction or row lock. Finalization rechecks state under a lock.
        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment order was not found"
                ));

        if (!payment.getMemberId().equals(memberId)) {
            throw new PaymentNotFoundException(
                    "Payment order was not found"
            );
        }

        // Idempotent verification.
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (request.getRazorpayPaymentId()
                    .equals(payment.getRazorpayPaymentId())) {
                sendReceiptIfConfigured(payment.getId());
                return toPaymentResponse(payment);
            }

            throw new IllegalStateException(
                    "Payment order has already been completed"
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment is not awaiting verification"
            );
        }

        // Verify checkout signature.
        boolean validSignature =
                razorpayService.verifyPaymentSignature(
                        payment.getRazorpayOrderId(),
                        request.getRazorpayPaymentId(),
                        request.getRazorpaySignature()
                );

        if (!validSignature) {
            throw new IllegalArgumentException(
                    "Payment signature verification failed"
            );
        }

        // Independently retrieve payment details from Razorpay.
        RazorpayService.RazorpayPayment gatewayPayment =
                razorpayService.fetchPaymentDetails(
                        request.getRazorpayPaymentId()
                );

        // Verify the returned payment against the stored order.
        if (gatewayPayment == null
                || !request.getRazorpayPaymentId().equals(gatewayPayment.paymentId())
                || !payment.getRazorpayOrderId().equals(gatewayPayment.orderId())
                || !payment.getAmountPaise().equals(gatewayPayment.amountPaise())
                || !payment.getCurrency().equals(gatewayPayment.currency())) {
            throw new IllegalArgumentException(
                    "Razorpay payment details do not match the stored order"
            );
        }

        if (!"captured".equalsIgnoreCase(gatewayPayment.status())) {
            throw new IllegalStateException(
                    "Payment has not been captured yet. Await payment confirmation."
            );
        }

        MembershipIntegrationService.ActivatedMembership activation = membershipIntegrationService.activatePaidMembershipWithDetails(
                payment.getId(), memberId, payment.getPlanName());
        if (activation == null) {
            Long membershipId = membershipIntegrationService.activatePaidMembership(payment.getId(), memberId, payment.getPlanName());
            activation = fallbackActivation(membershipId, payment.getPlanName());
        }
        Payment successfulPayment = orderPersistenceService.markPaymentSuccessful(
                memberId,
                payment.getRazorpayOrderId(),
                gatewayPayment,
                activation.membershipId(), activation.startDate(), activation.endDate());
        sendReceiptIfConfigured(successfulPayment.getId());
        return toPaymentResponse(successfulPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(
            Long memberId,
            Long paymentId
    ) {
        Payment payment = paymentRepository
                .findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment was not found"
                ));

        return toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentHistory(
            Long memberId,
            int page,
            int size,
            String memberAccessToken
    ) {
        if (memberId == null || memberId <= 0
                || page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters"
            );
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Payment> history = paymentRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        restoreLatestPaidMembership(memberId, memberAccessToken, history.getContent());
        return paymentRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(this::toPaymentResponse);
    }

    @Transactional(readOnly = true)
    public MonthlyRevenueResponse getCurrentMonthRevenue() {
        YearMonth month = YearMonth.now(REVENUE_ZONE);
        java.time.Instant start = month.atDay(1).atStartOfDay(REVENUE_ZONE).toInstant();
        java.time.Instant end = month.plusMonths(1).atDay(1).atStartOfDay(REVENUE_ZONE).toInstant();
        Long amount = paymentRepository.sumSuccessfulAmountBetween(PaymentStatus.SUCCESS, start, end);
        Long count = paymentRepository.countSuccessfulPaymentsBetween(PaymentStatus.SUCCESS, start, end);
        return new MonthlyRevenueResponse(month, amount == null ? 0L : amount,
                count == null ? 0L : count, "INR");
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(String requestedPeriod, int year, Integer month,
            int page, int size) {
        String period = requestedPeriod == null ? "MONTHLY" : requestedPeriod.trim().toUpperCase();
        if (!period.equals("MONTHLY") && !period.equals("YEARLY")) {
            throw new IllegalArgumentException("Period must be MONTHLY or YEARLY");
        }
        if (year < 2000 || year > 9999 || page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid revenue report date or pagination");
        }
        if (period.equals("MONTHLY") && (month == null || month < 1 || month > 12)) {
            throw new IllegalArgumentException("A month from 1 to 12 is required for monthly reports");
        }
        java.time.LocalDate startDate = period.equals("MONTHLY")
                ? java.time.LocalDate.of(year, month, 1) : java.time.LocalDate.of(year, 1, 1);
        java.time.LocalDate endDate = period.equals("MONTHLY")
                ? startDate.plusMonths(1) : startDate.plusYears(1);
        java.time.Instant start = startDate.atStartOfDay(REVENUE_ZONE).toInstant();
        java.time.Instant end = endDate.atStartOfDay(REVENUE_ZONE).toInstant();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var results = paymentRepository.findByStatusAndVerifiedAtGreaterThanEqualAndVerifiedAtLessThanOrderByVerifiedAtDesc(
                PaymentStatus.SUCCESS, start, end, pageable);
        Long total = paymentRepository.sumSuccessfulAmountBetween(PaymentStatus.SUCCESS, start, end);
        Long count = paymentRepository.countSuccessfulPaymentsBetween(PaymentStatus.SUCCESS, start, end);
        var profiles = memberDirectoryClient.getProfiles(results.getContent().stream()
                .map(Payment::getMemberId).distinct().toList());
        List<RevenuePaymentItem> items = results.getContent().stream().map(payment -> {
            var profile = profiles.get(payment.getMemberId());
            return new RevenuePaymentItem(payment.getId(), payment.getMemberId(),
                    profile == null ? "Member #" + payment.getMemberId() : profile.name(),
                    profile == null ? null : profile.email(), payment.getPlanName(),
                    payment.getAmountPaise(), payment.getCurrency(), payment.getVerifiedAt());
        }).toList();
        return new RevenueReportResponse(period, year, month,
                total == null ? 0L : total, count == null ? 0L : count, "INR",
                page, size, results.getTotalPages(), items);
    }

    private void restoreLatestPaidMembership(Long memberId, String memberAccessToken, List<Payment> payments) {
        if (payments.stream().anyMatch(payment -> payment.getMembershipId() != null)) return;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Payment candidate = null;
        LocalDate candidateEnd = null;
        for (Payment payment : payments) {
            if (payment.getStatus() != PaymentStatus.SUCCESS || payment.getVerifiedAt() == null) continue;
            int duration = membershipIntegrationService.getPlanDurationMonths(payment.getPlanName(), memberAccessToken);
            LocalDate start = payment.getVerifiedAt().atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate end = start.plusMonths(duration);
            if (!end.isBefore(today) && (candidateEnd == null || end.isAfter(candidateEnd))) {
                candidate = payment;
                candidateEnd = end;
            }
        }
        if (candidate == null) return;
        try {
            MembershipIntegrationService.ActivatedMembership activation = membershipIntegrationService
                    .activatePaidMembershipWithDetails(candidate.getId(), memberId, candidate.getPlanName());
            if (activation == null) {
                Long membershipId = membershipIntegrationService.activatePaidMembership(candidate.getId(), memberId, candidate.getPlanName());
                activation = fallbackActivation(membershipId, candidate.getPlanName());
            }
            orderPersistenceService.linkMembership(candidate.getId(), activation.membershipId(),
                    activation.startDate(), activation.endDate());
            sendReceiptIfConfigured(candidate.getId());
        } catch (RuntimeException exception) {
            log.warn("Could not restore membership for verified payment; paymentId={}, errorType={}",
                    candidate.getId(), exception.getClass().getSimpleName());
        }
    }

    private MembershipIntegrationService.ActivatedMembership fallbackActivation(Long membershipId, String planName) {
        int months = switch (planName == null ? "" : planName.toUpperCase(Locale.ROOT)) {
            case "MONTHLY" -> 1;
            case "QUARTERLY" -> 3;
            case "YEARLY" -> 12;
            default -> 1;
        };
        LocalDate start = LocalDate.now();
        return new MembershipIntegrationService.ActivatedMembership(membershipId, start, start.plus(months, ChronoUnit.MONTHS));
    }

    private void sendReceiptIfConfigured(Long paymentId) {
        if (paymentEmailService == null || paymentId == null) return;
        paymentEmailService.sendReceiptIfNeeded(paymentId);
    }

    private CreatePaymentResponse toCreatePaymentResponse(
            Payment payment
    ) {
        return new CreatePaymentResponse(
                payment.getId(),
                payment.getRazorpayOrderId(),
                payment.getAmountPaise(),
                payment.getCurrency(),
                membershipIntegrationService.getRazorpayKeyId(),
                payment.getPlanName(),
                payment.getStatus().name()
        );
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPlanName(),
                payment.getAmountPaise(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getMembershipId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getVerifiedAt()
        );
    }
}
