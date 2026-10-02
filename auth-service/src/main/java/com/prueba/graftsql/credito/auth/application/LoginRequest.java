package com.prueba.graftsql.credito.auth.application;

public class LoginRequest {
    private String email;
    private String password;
    private String mfaCode;

    public LoginRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMfaCode() { return mfaCode; }
    public void setMfaCode(String mfaCode) { this.mfaCode = mfaCode; }
}
