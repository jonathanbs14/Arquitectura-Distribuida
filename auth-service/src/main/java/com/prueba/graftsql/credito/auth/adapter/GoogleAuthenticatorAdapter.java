package com.prueba.graftsql.credito.auth.adapter;

import com.prueba.graftsql.credito.auth.application.ports.MFAPort;
import com.prueba.graftsql.credito.auth.domain.MFASetup;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.util.Utils;
import org.springframework.stereotype.Component;

@Component
public class GoogleAuthenticatorAdapter implements MFAPort {

    private static final CodeVerifier codeVerifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(), new SystemTimeProvider());

    @Override
    public MFASetup generateMFASecret(String userId) {
        String secret = new DefaultSecretGenerator().generate();
        String qrCodeUrl = generateQRCodeUrl(secret, userId);
        return new MFASetup(secret, qrCodeUrl, userId);
    }

    @Override
    public boolean verifyMFACode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    @Override
    public String generateQRCodeUrl(String secret, String email) {
        try {
            QrData data = new QrData.Builder()
                    .label(email)
                    .secret(secret)
                    .issuer("AuthService")
                    .digits(6)
                    .period(30)
                    .build();

            QrGenerator generator = new ZxingPngQrGenerator();
            byte[] imageData = generator.generate(data);
            return "data:image/png;base64," + Utils.getDataUriForImage(imageData, generator.getImageMimeType());
        } catch (Exception e) {
            throw new RuntimeException("Error generating QR code", e);
        }
    }
}
