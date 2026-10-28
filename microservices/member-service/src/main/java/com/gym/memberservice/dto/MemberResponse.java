package com.gym.memberservice.dto;

import com.gym.memberservice.entity.Member;
import java.math.BigDecimal;

public record MemberResponse(Long id, String name, Integer age, String mobile, String gymTime,
        String email, String gender, String fatherName, BigDecimal amount) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getAge(), member.getMobile(),
                member.getGymTime(), member.getEmail(), member.getGender(), member.getFatherName(), member.getAmount());
    }
}
