
package com.gym.memberservice.controller;

import com.gym.memberservice.entity.Member;
import com.gym.memberservice.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // Create a new member
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Member createMember(@Valid @RequestBody Member member) {
        return memberService.createMember(member);
    }

    // Get members with pagination

    @GetMapping
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


    // Get member by ID
    @GetMapping("/{id}")
    public Member getMemberById(@PathVariable Long id) {
        return memberService.getMemberById(id);
    }

    // Update an existing member
    @PutMapping("/{id}")
    public Member updateMember(
            @PathVariable Long id,
            @Valid @RequestBody Member member) {
        return memberService.updateMember(id, member);
    }

    // Delete a member
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
    }
}
