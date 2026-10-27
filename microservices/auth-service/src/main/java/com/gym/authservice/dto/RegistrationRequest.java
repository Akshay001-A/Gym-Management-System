
package com.gym.authservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class RegistrationRequest {

    @Valid
    @NotNull(message = "Account details are required")
    private RegisterRequest account;

    @Valid
    @NotNull(message = "Member profile details are required")
    private MemberProfileRequest profile;

    public RegistrationRequest() {
    }

    public RegisterRequest getAccount() {
        return account;
    }

    public void setAccount(RegisterRequest account) {
        this.account = account;
    }

    public MemberProfileRequest getProfile() {
        return profile;
    }

    public void setProfile(MemberProfileRequest profile) {
        this.profile = profile;
    }
}
