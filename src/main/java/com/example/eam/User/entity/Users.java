package com.example.eam.User.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;
import lombok.Builder.Default;

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = true, length = 150)
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Default
    @Column(nullable = true)
    private boolean deleted = false;

    @Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserRole> userRoles = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Default
    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 512)
    @JsonIgnore
    private String mfaSecret;

    @Column(name = "mfa_secret_temp", length = 512)
    @JsonIgnore
    private String mfaSecretTemp;

    @Column(name = "mfa_email_otp", length = 10)
    @JsonIgnore
    private String mfaEmailOtp;

    @Column(name = "mfa_email_otp_expires_at")
    @JsonIgnore
    private Instant mfaEmailOtpExpiresAt;

    @Default
    @Column(name = "mfa_email_verified", nullable = false)
    private boolean mfaEmailVerified = false;

}



