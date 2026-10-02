package com.prueba.graftsql.credito.auth.domain;

import jakarta.persistence.*;

@Entity
@Table(
        name = "oauth_profiles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_oauth_profile_provider_external_id",
                columnNames = {"provider", "external_id"}))
public class OAuthProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    public OAuthProfile() {}

    public OAuthProfile(String userId, AuthProvider provider, String externalId) {
        this.userId = userId;
        this.provider = provider;
        this.externalId = externalId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public AuthProvider getProvider() { return provider; }
    public void setProvider(AuthProvider provider) { this.provider = provider; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

}
