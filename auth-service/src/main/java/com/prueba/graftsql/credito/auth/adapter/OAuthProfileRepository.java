package com.prueba.graftsql.credito.auth.adapter;

import com.prueba.graftsql.credito.auth.domain.AuthProvider;
import com.prueba.graftsql.credito.auth.domain.OAuthProfile;
import java.util.Optional;
public interface OAuthProfileRepository {
    OAuthProfile save(OAuthProfile profile);
    Optional<OAuthProfile> findByProviderAndExternalId(AuthProvider provider, String externalId);
    Optional<OAuthProfile> findByUserId(String userId);
}
