
package com.gym.memberservice.service;

import com.gym.memberservice.entity.Member;
import com.gym.memberservice.dto.MemberProfileUpdateRequest;
import com.gym.memberservice.exception.MemberNotFoundException;
import com.gym.memberservice.repository.MemberRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member createMemberForRegistration(Member member, String registrationKey) {
        if (registrationKey == null || registrationKey.isBlank() || registrationKey.length() > 36) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid registration key is required");
        }
        Member existing = memberRepository.findByRegistrationKey(registrationKey).orElse(null);
        if (existing != null) {
            if (existing.getEmail() != null && existing.getEmail().equalsIgnoreCase(member.getEmail())) return existing;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration key is already linked to another profile");
        }
        member.setRegistrationKey(registrationKey);
        return createMember(member);
    }
    // Create a new member
    public Member createMember(Member member) {

        if (member.getEmail() != null
                && memberRepository.existsByEmailIgnoreCase(member.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        if (member.getMobile() != null
                && memberRepository.existsByMobile(member.getMobile())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mobile number already registered");
        }

        return memberRepository.save(member);
    }

    // Get the logged-in member's profile using JWT email
    public Member getMemberByEmail(String email) {

        return memberRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new MemberNotFoundException(
                                "Member profile not found for email: " + email
                        ));
    }

    public boolean existsById(Long id) {
        return id != null && id > 0 && memberRepository.existsById(id);
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

    public java.util.List<Member> getMembersByIds(java.util.List<Long> ids) {
        if (ids == null || ids.size() > 100) throw new IllegalArgumentException("A maximum of 100 member IDs is allowed");
        return memberRepository.findAllById(ids);
    }

    public Member findMemberForAccountRegistration(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the email or mobile number registered with the gym");
        }
        String value = identifier.trim();
        if (value.contains("@")) {
            return memberRepository.findByEmailIgnoreCase(value)
                    .orElseThrow(() -> new MemberNotFoundException("No gym member matches this contact"));
        }
        String digits = value.replaceAll("\\D", "");
        return memberRepository.findByMobile(digits)
                .orElseThrow(() -> new MemberNotFoundException("No gym member matches this contact"));
    }

    public Member updateMemberProfile(Long id, MemberProfileUpdateRequest request) {
        if (request == null || request.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one profile field must be supplied");
        }
        Member existing = getMemberById(id);
        validateUniqueContactDetails(id, request.email(), request.mobile());
        if (request.name() != null) {
            if (request.name().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be blank");
            existing.setName(request.name());
        }
        if (request.age() != null) existing.setAge(request.age());
        if (request.mobile() != null) existing.setMobile(request.mobile());
        if (request.gymTime() != null) existing.setGymTime(request.gymTime());
        if (request.email() != null) existing.setEmail(request.email());
        if (request.gender() != null) existing.setGender(request.gender());
        if (request.fatherName() != null) existing.setFatherName(request.fatherName());
        return memberRepository.save(existing);
    }

    // Update an existing member
    public Member updateMember(Long id, Member updatedMember) {

        Member existingMember = memberRepository.findById(id)
                .orElseThrow(() ->
                        new MemberNotFoundException(
                                "Member not found with ID: " + id
                        ));

        boolean emailUsedByAnotherMember =
                updatedMember.getEmail() != null
                        && memberRepository.existsByEmailIgnoreCase(
                        updatedMember.getEmail())
                        && (existingMember.getEmail() == null
                        || !existingMember.getEmail().equalsIgnoreCase(
                        updatedMember.getEmail()));

        if (emailUsedByAnotherMember) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        boolean mobileUsedByAnotherMember =
                updatedMember.getMobile() != null
                        && memberRepository.existsByMobile(
                        updatedMember.getMobile())
                        && !updatedMember.getMobile().equals(
                        existingMember.getMobile());

        if (mobileUsedByAnotherMember) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mobile number already registered");
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

    private void validateUniqueContactDetails(Long id, String email, String mobile) {
        boolean emailUsedByAnotherMember = email != null && memberRepository.findByEmailIgnoreCase(email)
                .filter(member -> !member.getId().equals(id)).isPresent();
        if (emailUsedByAnotherMember) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        boolean mobileUsedByAnotherMember = mobile != null && memberRepository.findByMobile(mobile)
                .filter(member -> !member.getId().equals(id)).isPresent();
        if (mobileUsedByAnotherMember) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mobile number already registered");
        }
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
