package com.prueba.graftsql.credito.auth.application.usercase;

import com.prueba.graftsql.credito.auth.adapter.UserRepository;
import com.prueba.graftsql.credito.auth.application.MFAVerifyRequest;
import com.prueba.graftsql.credito.auth.application.ports.MFAPort;
import com.prueba.graftsql.credito.auth.domain.User;
import org.springframework.stereotype.Service;

@Service
public class VerifyMFAUseCase {
    private final UserRepository userRepository;
    private final MFAPort mfaPort;

    public VerifyMFAUseCase(UserRepository userRepository, MFAPort mfaPort) {
        this.userRepository = userRepository;
        this.mfaPort = mfaPort;
    }

    public boolean execute(MFAVerifyRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String secretToVerify = request.getSecret() != null ? request.getSecret() : user.getMfaSecret();

        boolean isValid = mfaPort.verifyMFACode(secretToVerify, request.getCode());

        if (isValid) {
            user.setMfaEnabled(true);
            user.setMfaSecret(request.getSecret() != null ? request.getSecret() : user.getMfaSecret());
            userRepository.save(user);
        }

        return isValid;
    }
}
