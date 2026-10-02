package com.prueba.graftsql.credito.auth.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    private String fullName;
    private String password;
    private Boolean mfaEnabled = false;
    private String mfaSecret;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    public User() {}

    public User(String email, String fullName) {
        this.email = email;
        this.fullName = fullName;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(Boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

    public String getMfaSecret() { return mfaSecret; }
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }
}
