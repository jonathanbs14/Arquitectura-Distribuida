package com.prueba.graftsql.credito.auth.adapter;

import com.prueba.graftsql.credito.auth.domain.AuthProvider;
import com.prueba.graftsql.credito.auth.domain.OAuthProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface JpaOAuthProfileRepository extends JpaRepository<OAuthProfile, String>, OAuthProfileRepository {
    Optional<OAuthProfile> findByProviderAndExternalId(AuthProvider provider, String externalId);
    Optional<OAuthProfile> findByUserId(String userId);
}
