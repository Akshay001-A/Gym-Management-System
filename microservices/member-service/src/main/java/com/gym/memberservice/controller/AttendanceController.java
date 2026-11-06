package com.gym.memberservice.controller;

import com.gym.memberservice.dto.AttendanceDtos.AdminVisit;
import com.gym.memberservice.dto.AttendanceDtos.MemberHistory;
import com.gym.memberservice.dto.AttendanceDtos.QrTokenResponse;
import com.gym.memberservice.dto.AttendanceDtos.ScanRequest;
import com.gym.memberservice.dto.AttendanceDtos.ScanResponse;
import com.gym.memberservice.service.AttendanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/members")
public class AttendanceController {
    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/attendance/qr-token")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public QrTokenResponse createGymQrToken(@AuthenticationPrincipal Jwt jwt) {
        return attendanceService.issueQrToken(jwt.getSubject());
    }

    @PostMapping("/me/attendance/scan")
    @PreAuthorize("hasRole('MEMBER')")
    public ScanResponse scanGymQrCode(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ScanRequest request) {
        return attendanceService.scan(authenticatedMemberId(jwt), request.qrPayload());
    }

    @GetMapping("/me/attendance")
    @PreAuthorize("hasRole('MEMBER')")
    public MemberHistory getMyAttendance(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return attendanceService.getMemberHistory(authenticatedMemberId(jwt), from, to);
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AdminVisit> getAttendanceForAdmin(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return attendanceService.getAdminAttendance(date, page, size);
    }

    private Long authenticatedMemberId(Jwt jwt) {
        Object memberIdClaim = jwt.getClaim("memberId");
        if (!(memberIdClaim instanceof Number memberId) || memberId.longValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Member account is not linked to a profile");
        }
        return memberId.longValue();
    }
}
