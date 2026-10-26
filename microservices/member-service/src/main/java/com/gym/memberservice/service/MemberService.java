
package com.gym.memberservice.service;

import com.gym.memberservice.entity.Member;
import com.gym.memberservice.exception.MemberNotFoundException;
import com.gym.memberservice.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // Create a new member
    public Member createMember(Member member) {

        if (member.getEmail() != null
                && memberRepository.existsByEmail(member.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already registered: " + member.getEmail()
            );
        }

        if (member.getMobile() != null
                && memberRepository.existsByMobile(member.getMobile())) {
            throw new IllegalArgumentException(
                    "Mobile number already registered: " + member.getMobile()
            );
        }

        return memberRepository.save(member);
    }

    // Get members with pagination
    public Page<Member> getAllMembers(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    // Get member by ID
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() ->
                        new MemberNotFoundException(
                                "Member not found with ID: " + id
                        ));
    }

    // Update an existing member
    public Member updateMember(Long id, Member updatedMember) {

        Member existingMember = memberRepository.findById(id)
                .orElseThrow(() ->
                        new MemberNotFoundException(
                                "Member not found with ID: " + id
                        ));

        // Check whether another member uses this email
        boolean emailUsedByAnotherMember =
                updatedMember.getEmail() != null
                        && memberRepository.existsByEmail(updatedMember.getEmail())
                        && (existingMember.getEmail() == null
                        || !existingMember.getEmail().equalsIgnoreCase(
                        updatedMember.getEmail()
                ));

        if (emailUsedByAnotherMember) {
            throw new IllegalArgumentException(
                    "Email already registered: " + updatedMember.getEmail()
            );
        }

        // Check whether another member uses this mobile number
        boolean mobileUsedByAnotherMember =
                updatedMember.getMobile() != null
                        && memberRepository.existsByMobile(updatedMember.getMobile())
                        && !updatedMember.getMobile().equals(
                        existingMember.getMobile()
                );

        if (mobileUsedByAnotherMember) {
            throw new IllegalArgumentException(
                    "Mobile number already registered: " + updatedMember.getMobile()
            );
        }

        existingMember.setName(updatedMember.getName());
        existingMember.setAge(updatedMember.getAge());
        existingMember.setMobile(updatedMember.getMobile());
        existingMember.setGymTime(updatedMember.getGymTime());
        existingMember.setEmail(updatedMember.getEmail());
        existingMember.setGender(updatedMember.getGender());
        existingMember.setFatherName(updatedMember.getFatherName());
        existingMember.setAmount(updatedMember.getAmount());

        return memberRepository.save(existingMember);
    }

    // Delete a member
    public void deleteMember(Long id) {

        if (!memberRepository.existsById(id)) {
            throw new MemberNotFoundException(
                    "Member not found with ID: " + id
            );
        }

        memberRepository.deleteById(id);
    }
}
