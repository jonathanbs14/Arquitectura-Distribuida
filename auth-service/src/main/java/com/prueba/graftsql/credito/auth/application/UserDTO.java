package com.prueba.graftsql.credito.auth.application;

public class UserDTO {
    private String id;
    private String email;
    private String fullName;
    private Boolean mfaEnabled;

    public UserDTO() {}

    public UserDTO(String id, String email, String fullName, Boolean mfaEnabled) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.mfaEnabled = mfaEnabled;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Boolean getMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(Boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
}
