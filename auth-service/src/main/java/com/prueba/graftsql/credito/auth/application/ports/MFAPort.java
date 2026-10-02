package com.prueba.graftsql.credito.auth.application.ports;

import com.prueba.graftsql.credito.auth.domain.MFASetup;

public interface MFAPort {
    MFASetup generateMFASecret(String userId);
    boolean verifyMFACode(String secret, String code);
    String generateQRCodeUrl(String secret, String email);
}
