package com.prueba.graftsql.credito.auth.adapter;

public class MFAVerifyResponse {
    private Boolean success;

    public MFAVerifyResponse(Boolean success) {
        this.success = success;
    }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
}
