package com.gym.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(name = "uk_account_email", columnNames = "email"))
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false, length = 20)
    private String role = "MEMBER";
    @Column(name = "member_id", unique = true)
    private Long memberId;
    @Column(name = "registration_key", unique = true, length = 36)
    private String registrationKey;
    @Column(length = 20)
    private String status = "ACTIVE";
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Account() { }
    public Account(String name, String email, String password, String role) {
        this.name=name; this.email=email; this.password=password; this.role=role; this.createdAt=LocalDateTime.now();
    }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getEmail(){return email;} public void setEmail(String email){this.email=email;}
    public String getPassword(){return password;} public void setPassword(String password){this.password=password;}
    public String getRole(){return role;} public void setRole(String role){this.role=role;}
    public Long getMemberId(){return memberId;} public void setMemberId(Long memberId){this.memberId=memberId;}
    public String getRegistrationKey(){return registrationKey;} public void setRegistrationKey(String registrationKey){this.registrationKey=registrationKey;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}