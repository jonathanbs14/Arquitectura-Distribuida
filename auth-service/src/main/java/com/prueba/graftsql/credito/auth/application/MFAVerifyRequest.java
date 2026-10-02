package com.prueba.graftsql.credito.auth.application;

public class MFAVerifyRequest {
    private String userId;
    private String code;
    private String secret;

    public MFAVerifyRequest() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
