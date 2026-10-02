package com.prueba.graftsql.credito.auth.application.usercase;

import com.prueba.graftsql.credito.auth.adapter.OAuthProfileRepository;
import com.prueba.graftsql.credito.auth.adapter.UserRepository;
import com.prueba.graftsql.credito.auth.application.AuthResponse;
import com.prueba.graftsql.credito.auth.application.UserDTO;
import com.prueba.graftsql.credito.auth.application.ports.JwtPort;
import com.prueba.graftsql.credito.auth.domain.AuthProvider;
import com.prueba.graftsql.credito.auth.domain.OAuthProfile;
import com.prueba.graftsql.credito.auth.domain.SocialUserInfo;
import com.prueba.graftsql.credito.auth.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterOAuthUseCase {
    private final UserRepository userRepository;
    private final OAuthProfileRepository oauthProfileRepository;
    private final JwtPort jwtPort;

    public RegisterOAuthUseCase(UserRepository userRepository, OAuthProfileRepository oauthProfileRepository, JwtPort jwtPort) {
        this.userRepository = userRepository;
        this.oauthProfileRepository = oauthProfileRepository;
        this.jwtPort = jwtPort;
    }

    @Transactional
    public AuthResponse execute(SocialUserInfo userInfo, AuthProvider provider) {
        var existingProfile = oauthProfileRepository.findByProviderAndExternalId(provider, userInfo.getId());

        User user;
        if (existingProfile.isPresent()) {
            user = userRepository.findById(existingProfile.get().getUserId()).orElseThrow();
        } else {
            if (userRepository.findByEmail(userInfo.getEmail()).isPresent()) {
                throw new OAuthAccountAlreadyExistsException();
            }
            user = new User(userInfo.getEmail(), userInfo.getName());
            user.setAuthProvider(provider);
            user = userRepository.save(user);

            OAuthProfile profile = new OAuthProfile(user.getId(), provider, userInfo.getId());
            oauthProfileRepository.save(profile);
        }

        String token = jwtPort.generateToken(user);
        String refreshToken = jwtPort.generateRefreshToken(user);

        AuthResponse response = new AuthResponse(token, new UserDTO(
                user.getId(), user.getEmail(), user.getFullName(), user.getMfaEnabled()
        ));
        response.setRefreshToken(refreshToken);
        return response;
    }
}
