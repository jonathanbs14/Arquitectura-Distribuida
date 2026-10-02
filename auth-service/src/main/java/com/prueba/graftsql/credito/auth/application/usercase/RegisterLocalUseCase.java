package com.prueba.graftsql.credito.auth.application.usercase;

import com.prueba.graftsql.credito.auth.adapter.UserRepository;
import com.prueba.graftsql.credito.auth.application.AuthResponse;
import com.prueba.graftsql.credito.auth.application.UserDTO;
import com.prueba.graftsql.credito.auth.application.ports.JwtPort;
import com.prueba.graftsql.credito.auth.domain.AuthProvider;
import com.prueba.graftsql.credito.auth.domain.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterLocalUseCase {
    private final UserRepository userRepository;
    private final JwtPort jwtPort;
    private final PasswordEncoder passwordEncoder;

    public RegisterLocalUseCase(UserRepository userRepository, JwtPort jwtPort, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtPort = jwtPort;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse execute(String email, String fullName, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = new User(email, fullName);
        user.setPassword(passwordEncoder.encode(password));
        user.setAuthProvider(AuthProvider.LOCAL);
        user = userRepository.save(user);

        String token = jwtPort.generateToken(user);
        String refreshToken = jwtPort.generateRefreshToken(user);

        AuthResponse response = new AuthResponse(token, new UserDTO(
                user.getId(), user.getEmail(), user.getFullName(), user.getMfaEnabled()
        ));
        response.setRefreshToken(refreshToken);
        return response;
    }
}
