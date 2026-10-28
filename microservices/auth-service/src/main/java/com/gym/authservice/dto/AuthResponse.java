package com.gym.authservice.dto;

public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long accountId;
    private Long memberId;
    private String name;
    private String email;
    private String role;

    public AuthResponse() { }
    public AuthResponse(String token, Long accountId, Long memberId, String name, String email, String role) {
        this.token = token;
        this.accountId = accountId;
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.role = role;
    }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}