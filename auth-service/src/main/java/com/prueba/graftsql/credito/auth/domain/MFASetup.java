package com.prueba.graftsql.credito.auth.domain;

public class MFASetup {
    private String secret;
    private String qrCodeUrl;
    private String userId;
    private Boolean isVerified = false;

    public MFASetup(String secret, String qrCodeUrl, String userId) {
        this.secret = secret;
        this.qrCodeUrl = qrCodeUrl;
        this.userId = userId;
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean verified) { this.isVerified = verified; }
}
