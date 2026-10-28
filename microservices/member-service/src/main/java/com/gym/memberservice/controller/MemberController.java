package com.gym.memberservice.controller;

import com.gym.memberservice.entity.Member;
import com.gym.memberservice.dto.AdminMemberRequest;
import com.gym.memberservice.dto.MemberProfileUpdateRequest;
import com.gym.memberservice.dto.MemberRegistrationRequest;
import com.gym.memberservice.dto.MemberResponse;
import com.gym.memberservice.exception.MemberNotFoundException;
import com.gym.memberservice.service.MemberService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;
    private final String internalServiceKey;

    public MemberController(MemberService memberService,
            @Value("${app.internal-service.key:}") String internalServiceKey) {
        this.memberService = memberService;
        this.internalServiceKey = internalServiceKey;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MemberResponse createMember(@Valid @RequestBody AdminMemberRequest request) {
        return toResponse(memberService.createMember(toMember(request)));
    }

    @PostMapping("/internal")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse createMemberInternal(
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key,
            @RequestHeader(value = "X-Registration-Key", required = false) String registrationKey,
            @Valid @RequestBody MemberRegistrationRequest request) {
        requireInternalKey(key);
        return toResponse(memberService.createMemberForRegistration(toMember(request), registrationKey));
    }

    @DeleteMapping("/internal/{id}")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMemberInternal(@RequestHeader(value = "X-Internal-Service-Key", required = false) String key,
            @PathVariable Long id) {
        requireInternalKey(key);
        memberService.deleteMember(id);
    }

    @GetMapping("/internal/{id}/exists")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public boolean memberExistsInternal(@RequestHeader(value = "X-Internal-Service-Key", required = false) String key,
            @PathVariable Long id) {
        requireInternalKey(key);
        return memberService.existsById(id);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public MemberResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return toResponse(memberService.getMemberById(authenticatedMemberId(jwt)));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public MemberResponse updateMyProfile(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MemberProfileUpdateRequest request) {
        return toResponse(memberService.updateMemberProfile(authenticatedMemberId(jwt), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<MemberResponse> getAllMembers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        }
        if (!java.util.List.of("id", "name", "age", "email", "mobile", "amount").contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        Sort.Direction sortDirection;
        try { sortDirection = Sort.Direction.fromString(direction); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort direction"); }
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return memberService.getAllMembers(pageable).map(MemberResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MemberResponse getMemberById(@PathVariable Long id) { return toResponse(memberService.getMemberById(id)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MemberResponse updateMember(@PathVariable Long id, @Valid @RequestBody AdminMemberRequest request) {
        return toResponse(memberService.updateMember(id, toMember(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMember(@PathVariable Long id) { memberService.deleteMember(id); }

    private Long authenticatedMemberId(Jwt jwt) {
        Object memberIdClaim = jwt.getClaim("memberId");
        if (!(memberIdClaim instanceof Number memberId) || memberId.longValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Member account is not linked to a profile");
        }
        return memberId.longValue();
    }

    private MemberResponse toResponse(Member member) {
        return MemberResponse.from(member);
    }

    private Member toMember(MemberRegistrationRequest request) {
        Member member = new Member();
        member.setName(request.name());
        member.setAge(request.age());
        member.setMobile(request.mobile());
        member.setGymTime(request.gymTime());
        member.setEmail(request.email());
        member.setGender(request.gender());
        member.setFatherName(request.fatherName());
        member.setAmount(request.amount());
        return member;
    }

    private Member toMember(AdminMemberRequest request) {
        Member member = new Member();
        member.setName(request.name());
        member.setAge(request.age());
        member.setMobile(request.mobile());
        member.setGymTime(request.gymTime());
        member.setEmail(request.email());
        member.setGender(request.gender());
        member.setFatherName(request.fatherName());
        member.setAmount(request.amount());
        return member;
    }

    private void requireInternalKey(String supplied) {
        if (internalServiceKey == null || internalServiceKey.isBlank() || supplied == null
                || !MessageDigest.isEqual(internalServiceKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal service credentials");
        }
    }
}