package com.prueba.graftsql.credito.auth.application.usercase;

import com.prueba.graftsql.credito.auth.adapter.UserRepository;
import com.prueba.graftsql.credito.auth.application.MFASetupResponse;
import com.prueba.graftsql.credito.auth.application.ports.MFAPort;
import com.prueba.graftsql.credito.auth.domain.User;
import org.springframework.stereotype.Service;
@Service
public class SetupMFAUseCase {
    private final UserRepository userRepository;
    private final MFAPort mfaPort;

    public SetupMFAUseCase(UserRepository userRepository, MFAPort mfaPort) {
        this.userRepository = userRepository;
        this.mfaPort = mfaPort;
    }

    public MFASetupResponse execute(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var mfaSetup = mfaPort.generateMFASecret(userId);
        user.setMfaSecret(mfaSetup.getSecret());
        userRepository.save(user);

        return new MFASetupResponse(mfaSetup.getSecret(), mfaSetup.getQrCodeUrl());
    }
}
