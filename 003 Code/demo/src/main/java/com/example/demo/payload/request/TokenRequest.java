package com.example.demo.payload.request;

import javax.validation.constraints.NotBlank;

public class TokenRequest {

    @NotBlank
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}