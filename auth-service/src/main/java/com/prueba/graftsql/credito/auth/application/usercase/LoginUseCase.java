package com.prueba.graftsql.credito.auth.application.usercase;

import com.prueba.graftsql.credito.auth.adapter.UserRepository;
import com.prueba.graftsql.credito.auth.application.AuthResponse;
import com.prueba.graftsql.credito.auth.application.LoginRequest;
import com.prueba.graftsql.credito.auth.application.UserDTO;
import com.prueba.graftsql.credito.auth.application.ports.JwtPort;
import com.prueba.graftsql.credito.auth.application.ports.MFAPort;
import com.prueba.graftsql.credito.auth.domain.User;
import com.prueba.graftsql.credito.auth.domain.AuthProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {
    private final UserRepository userRepository;
    private final JwtPort jwtPort;
    private final MFAPort mfaPort;
    private final PasswordEncoder passwordEncoder;

    public LoginUseCase(UserRepository userRepository, JwtPort jwtPort, MFAPort mfaPort, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtPort = jwtPort;
        this.mfaPort = mfaPort;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse execute(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPassword() == null) {
            throw new RuntimeException("Use el proveedor con el que creó su cuenta");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (user.getMfaEnabled() && (request.getMfaCode() == null || request.getMfaCode().isEmpty())) {
            AuthResponse response = new AuthResponse();
            response.setMfaRequired(true);
            response.setSessionToken(jwtPort.generateToken(user));
            return response;
        }

        if (user.getMfaEnabled()) {
            if (!mfaPort.verifyMFACode(user.getMfaSecret(), request.getMfaCode())) {
                throw new RuntimeException("Invalid MFA code");
            }
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
