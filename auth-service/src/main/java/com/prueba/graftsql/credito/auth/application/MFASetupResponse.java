package com.prueba.graftsql.credito.auth.application;

public class MFASetupResponse {
    private String secret;
    private String qrCodeUrl;

    public MFASetupResponse(String secret, String qrCodeUrl) {
        this.secret = secret;
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }
}
