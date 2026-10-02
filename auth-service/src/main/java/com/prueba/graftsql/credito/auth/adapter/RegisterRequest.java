package com.prueba.graftsql.credito.auth.adapter;

public class RegisterRequest {
    private String email;
    private String fullName;
    private String password;

    public RegisterRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
