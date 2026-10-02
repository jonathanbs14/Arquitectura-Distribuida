package com.prueba.graftsql.credito.auth.application;

public class AuthResponse {
    private String token;
    private String refreshToken;
    private UserDTO user;
    private Boolean mfaRequired = false;
    private String sessionToken;

    public AuthResponse() {}

    public AuthResponse(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }

    public Boolean getMfaRequired() { return mfaRequired; }
    public void setMfaRequired(Boolean mfaRequired) { this.mfaRequired = mfaRequired; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}
