package com.gym.memberservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MemberBatchRequest(@NotEmpty @Size(max = 100) List<Long> memberIds) { }
