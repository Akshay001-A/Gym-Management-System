
package com.gym.memberservice.controller;

import com.gym.memberservice.entity.Member;
import com.gym.memberservice.service.MemberService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // Registering a member: used by the Auth Service.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Member createMember(
            @Valid @RequestBody Member member) {
        return memberService.createMember(member);
    }

    // A member can view their own profile.
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public Member getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {

        return memberService.getMemberByEmail(jwt.getSubject());
    }

    // Only Admin can view all members.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<Member> getAllMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction sortDirection =
                Sort.Direction.fromString(direction);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        return memberService.getAllMembers(pageable);
    }

    // Only Admin can look up arbitrary member IDs.
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Member getMemberById(@PathVariable Long id) {
        return memberService.getMemberById(id);
    }

    // Only Admin can update member records.
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Member updateMember(
            @PathVariable Long id,
            @Valid @RequestBody Member member) {

        return memberService.updateMember(id, member);
    }

    // Only Admin can delete members.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
    }
}
